package org.example.service;

import org.example.config.DBConnection;
import org.example.dao.CarDao;
import org.example.dao.CarImageDao;
import org.example.dao.ParkingLotDao;
import org.example.dao.RentalDao;
import org.example.domain.Car;
import org.example.domain.StoredCarImage;
import org.example.domain.CarStatus;
import org.example.domain.CarWithParkingLot;
import org.example.domain.ParkingLot;
import org.example.dto.ParkingLotAvailability;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class CarService {

    private final CarDao carDao;
    private final ParkingLotDao parkingLotDao;
    private final RentalDao rentalDao;
    private final CarImageDao carImageDao;

    public CarService(CarDao carDao, ParkingLotDao parkingLotDao, RentalDao rentalDao) {
        this(carDao, parkingLotDao, rentalDao, new CarImageDao());
    }

    public CarService(
            CarDao carDao,
            ParkingLotDao parkingLotDao,
            RentalDao rentalDao,
            CarImageDao carImageDao
    ) {
        this.carDao = carDao;
        this.parkingLotDao = parkingLotDao;
        this.rentalDao = rentalDao;
        this.carImageDao = carImageDao;
    }

    public List<CarWithParkingLot> findAllCars() {
        return carDao.findAllWithParkingLot();
    }

    public List<CarWithParkingLot> findAllVisibleCars() {
        return carDao.findAllVisibleWithParkingLot();
    }

    public Car findCarById(long carId) {
        if (carId <= 0) {
            throw new IllegalArgumentException("차량 ID는 양수여야 합니다.");
        }

        return carDao.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 차량입니다."));
    }

    public List<CarWithParkingLot> findAvailableCarsAtParkingLot(
            long parkingLotId,
            LocalDateTime startTime,
            LocalDateTime returnTime,
            int passengerCount
    ) {
        validateRentalSearchCondition(startTime, returnTime, passengerCount);

        if (parkingLotId <= 0) {
            throw new IllegalArgumentException("주차장 정보가 올바르지 않습니다.");
        }

        ParkingLot parkingLot = parkingLotDao.findById(parkingLotId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주차장입니다."));
        if (!parkingLot.isActive()) {
            throw new IllegalArgumentException("현재 운영하지 않는 주차장입니다.");
        }

        return carDao.findAvailableByParkingLotAndPeriod(
                parkingLotId,
                startTime,
                returnTime,
                passengerCount
        );
    }

    public List<ParkingLotAvailability> findParkingLotAvailability(
            LocalDateTime startTime,
            LocalDateTime returnTime,
            int passengerCount
    ) {
        validateRentalSearchCondition(startTime, returnTime, passengerCount);
        return carDao.findParkingLotAvailability(startTime, returnTime, passengerCount);
    }

    public long registerCar(
            String name,
            int maxPassengerCount,
            int pricePerKm,
            long parkingLotId
    ) {
        return registerCar(name, maxPassengerCount, pricePerKm, parkingLotId, null);
    }

    public long registerCar(
            String name,
            int maxPassengerCount,
            int pricePerKm,
            long parkingLotId,
            StoredCarImage image
    ) {
        validate(name, maxPassengerCount, pricePerKm, parkingLotId);

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                ParkingLot parkingLot = parkingLotDao.findByIdForUpdate(
                        connection,
                        parkingLotId
                ).orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 주차장입니다.")
                );

                if (!parkingLot.isActive()) {
                    throw new IllegalStateException("운영 중인 주차장에만 차량을 등록할 수 있습니다.");
                }

                int availableParkingCount =
                        parkingLot.getMaxParkingCount()
                                - parkingLot.getCurrentParkingCount()
                                - parkingLot.getRentedCarCount();

                if (availableParkingCount <= 0) {
                    throw new IllegalStateException("주차 가능한 자리가 없습니다.");
                }

                int carId = carDao.save(
                        connection,
                        name.trim(),
                        maxPassengerCount,
                        pricePerKm,
                        parkingLotId,
                        parkingLot.getAddress()
                );

                if (image != null) {
                    carImageDao.save(connection, carId, image);
                }

                int updatedRows = parkingLotDao.increaseCurrentCount(
                        connection,
                        parkingLotId
                );

                if (updatedRows != 1) {
                    throw new IllegalStateException("주차장 차량 수 증가에 실패했습니다.");
                }

                connection.commit();
                return carId;

            } catch (RuntimeException e) {
                rollback(connection, e);
                throw e;

            } catch (SQLException e) {
                rollback(connection, e);
                throw new RuntimeException("차량 등록 처리 중 DB 오류가 발생했습니다.", e);
            }

        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 처리 중 오류가 발생했습니다.", e);
        }
    }

    public void changeStatus(long carId, CarStatus newStatus) {
        if (carId <= 0) {
            throw new IllegalArgumentException("차량 ID는 양수여야 합니다.");
        }

        if (newStatus == null) {
            throw new IllegalArgumentException("변경할 차량 상태는 필수입니다.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Car car = carDao.findByIdForUpdate(connection, carId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("존재하지 않는 차량입니다."));

                if (car.getCarStatus() == CarStatus.RENTED) {
                    throw new IllegalStateException("대여 중인 차량은 반납이 완료된 뒤 상태를 변경할 수 있습니다.");
                }

                if (car.getCarStatus() == newStatus) {
                    throw new IllegalArgumentException("이미 선택한 차량 상태입니다.");
                }

                if (!isAllowedStatusTransition(
                        car.getCarStatus(),
                        newStatus
                )) {
                    throw new IllegalStateException(
                            invalidStatusTransitionMessage(car.getCarStatus(), newStatus)
                    );
                }

                if (rentalDao.hasActiveRentalByCar(connection, carId)) {
                    throw new IllegalStateException(
                            "진행 중인 예약 또는 대여가 있어 차량 상태를 변경할 수 없습니다."
                    );
                }

                int updatedRows = carDao.updateStatus(
                        connection,
                        carId,
                        newStatus
                );

                if (updatedRows != 1) {
                    throw new IllegalStateException("차량 상태 변경에 실패했습니다.");
                }

                connection.commit();

            } catch (RuntimeException e) {
                rollback(connection, e);
                throw e;

            } catch (SQLException e) {
                rollback(connection, e);
                throw new RuntimeException(
                        "차량 상태 변경 처리 중 DB 오류가 발생했습니다.",
                        e
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 처리 중 오류가 발생했습니다.", e);
        }
    }

    public void changePricePerKm(long carId, int pricePerKm) {
        if (carId <= 0) {
            throw new IllegalArgumentException("차량 ID는 양수여야 합니다.");
        }
        if (pricePerKm <= 0) {
            throw new IllegalArgumentException("1km당 요금은 1원 이상이어야 합니다.");
        }

        if (carDao.updatePricePerKm(carId, pricePerKm) != 1) {
            throw new IllegalArgumentException("존재하지 않는 차량입니다.");
        }
    }

    public void moveCar(long carId, long newParkingLotId) {
        if (carId <= 0 || newParkingLotId <= 0) {
            throw new IllegalArgumentException("차량과 주차장 정보가 올바르지 않습니다.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Car car = carDao.findByIdForUpdate(connection, carId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("존재하지 않는 차량입니다."));

                if (car.getCarStatus() == CarStatus.RENTED
                        || car.getCurrentParkingLotId() == null) {
                    throw new IllegalStateException("대여 중인 차량은 주차장을 변경할 수 없습니다.");
                }

                if (rentalDao.hasActiveRentalByCar(connection, carId)) {
                    throw new IllegalStateException(
                            "진행 중인 예약 또는 대여가 있어 차량 주차장을 변경할 수 없습니다."
                    );
                }

                long currentParkingLotId = car.getCurrentParkingLotId();
                if (currentParkingLotId == newParkingLotId) {
                    throw new IllegalArgumentException("이미 해당 주차장에 있는 차량입니다.");
                }

                ParkingLot firstParkingLot = parkingLotDao.findByIdForUpdate(
                        connection,
                        Math.min(currentParkingLotId, newParkingLotId)
                ).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주차장입니다."));

                ParkingLot secondParkingLot = parkingLotDao.findByIdForUpdate(
                        connection,
                        Math.max(currentParkingLotId, newParkingLotId)
                ).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주차장입니다."));

                ParkingLot currentParkingLot = firstParkingLot.getId() == currentParkingLotId
                        ? firstParkingLot
                        : secondParkingLot;
                ParkingLot newParkingLot = firstParkingLot.getId() == newParkingLotId
                        ? firstParkingLot
                        : secondParkingLot;

                if (currentParkingLot.getCurrentParkingCount() <= 0) {
                    throw new IllegalStateException("기존 주차장의 차량 수가 올바르지 않습니다.");
                }

                if (newParkingLot.getAvailableParkingCount() <= 0) {
                    throw new IllegalStateException("이동할 주차장에 여유 자리가 없습니다.");
                }

                if (!newParkingLot.isActive()) {
                    throw new IllegalStateException("운영 중인 주차장으로만 차량을 이동할 수 있습니다.");
                }

                if (parkingLotDao.decreaseCurrentParkingCount(
                        connection,
                        currentParkingLotId
                ) != 1) {
                    throw new IllegalStateException("기존 주차장의 차량 수 변경에 실패했습니다.");
                }

                if (parkingLotDao.increaseCurrentCount(connection, newParkingLotId) != 1) {
                    throw new IllegalStateException("이동할 주차장에 차량을 등록하지 못했습니다.");
                }

                if (carDao.updateCurrentParkingLot(
                        connection,
                        carId,
                        newParkingLotId,
                        newParkingLot.getAddress()
                ) != 1) {
                    throw new IllegalStateException("차량 주차장 변경에 실패했습니다.");
                }

                connection.commit();

            } catch (RuntimeException e) {
                rollback(connection, e);
                throw e;

            } catch (SQLException e) {
                rollback(connection, e);
                throw new RuntimeException("차량 이동 처리 중 DB 오류가 발생했습니다.", e);
            }

        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 처리 중 오류가 발생했습니다.", e);
        }
    }

    private boolean isAllowedStatusTransition(CarStatus currentStatus, CarStatus newStatus) {
        return (currentStatus == CarStatus.AVAILABLE
                && (newStatus == CarStatus.MAINTENANCE
                || newStatus == CarStatus.INACTIVE))
                || (currentStatus == CarStatus.MAINTENANCE
                && (newStatus == CarStatus.AVAILABLE
                || newStatus == CarStatus.INACTIVE))
                || (currentStatus == CarStatus.INACTIVE
                && newStatus == CarStatus.AVAILABLE);
    }

    private String invalidStatusTransitionMessage(
            CarStatus currentStatus,
            CarStatus newStatus
    ) {
        if (newStatus == CarStatus.RENTED) {
            return "대여 중 상태는 대여 시작과 반납 처리에서 자동으로 변경됩니다.";
        }
        if (currentStatus == CarStatus.INACTIVE && newStatus == CarStatus.MAINTENANCE) {
            return "비활성 차량은 먼저 운영 가능 상태로 활성화한 뒤 정비 상태로 변경할 수 있습니다.";
        }
        return "현재 차량 상태에서는 선택한 상태로 변경할 수 없습니다.";
    }

    private void validateRentalSearchCondition(
            LocalDateTime startTime,
            LocalDateTime returnTime,
            int passengerCount
    ) {
        if (startTime == null || returnTime == null) {
            throw new IllegalArgumentException("대여 시작과 반납 시각을 모두 선택해주세요.");
        }
        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("대여 시작 시각은 현재 시각보다 이후여야 합니다.");
        }
        if (!returnTime.isAfter(startTime)) {
            throw new IllegalArgumentException("반납 시각은 대여 시작 시각보다 늦어야 합니다.");
        }
        if (Duration.between(startTime, returnTime).compareTo(Duration.ofMinutes(30)) < 0) {
            throw new IllegalArgumentException("대여 시간은 최소 30분 이상이어야 합니다.");
        }
        if (passengerCount <= 0) {
            throw new IllegalArgumentException("탑승 인원은 1명 이상이어야 합니다.");
        }
        if (passengerCount > 9) {
            throw new IllegalArgumentException("탑승 인원은 9명 이하여야 합니다.");
        }
    }

    private void validate(
            String name,
            int maxPassengerCount,
            int pricePerKm,
            long parkingLotId
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("차량명은 필수입니다.");
        }

        if (maxPassengerCount <= 0) {
            throw new IllegalArgumentException("최대 탑승 인원은 1명 이상이어야 합니다.");
        }

        if (pricePerKm <= 0) {
            throw new IllegalArgumentException("1km당 요금은 1원 이상이어야 합니다.");
        }

        if (parkingLotId <= 0) {
            throw new IllegalArgumentException("주차장 ID는 양수여야 합니다.");
        }
    }

    private void rollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }
}
