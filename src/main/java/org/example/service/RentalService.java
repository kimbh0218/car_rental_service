package org.example.service;

import org.example.config.DBConnection;
import org.example.dao.CarDao;
import org.example.dao.MemberDao;
import org.example.dao.ParkingLotDao;
import org.example.dao.RentalDao;
import org.example.domain.*;
import org.example.dto.ReservationRequest;
import org.example.dto.CarScheduleItem;
import org.example.dto.MyRentalItem;


import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class RentalService {
    private static final Duration NO_SHOW_GRACE_PERIOD = Duration.ofMinutes(10);

    private final CarDao carDao;
    private final RentalDao rentalDao;
    private final MemberDao memberDao;
    private final ParkingLotDao parkingLotDao;

    public RentalService(
            CarDao carDao,
            RentalDao rentalDao,
            MemberDao memberDao,
            ParkingLotDao parkingLotDao
    ) {
        this.carDao = carDao;
        this.rentalDao = rentalDao;
        this.memberDao = memberDao;
        this.parkingLotDao = parkingLotDao;
    }

    public List<RentalWithMember> findAllRentals() {
        return rentalDao.findAllWithMemberName();
    }

    public List<MyRentalItem> findMyRentals(long memberId) {
        validateMemberId(memberId);
        return rentalDao.findMyRentalsByMemberId(memberId);
    }

    public Rental findRentalById(long rentalId) {
        if (rentalId <= 0) {
            throw new IllegalArgumentException("대여 ID는 양수여야 합니다.");
        }

        return rentalDao.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대여 내역입니다."));
    }

    public List<CarScheduleItem> findCarSchedule(
            long carId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        if (carId <= 0) {
            throw new IllegalArgumentException("차량 ID는 양수여야 합니다.");
        }
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException("조회 기간이 올바르지 않습니다.");
        }

        carDao.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 차량입니다."));

        return rentalDao.findScheduleByCarAndPeriod(carId, from, to);
    }

    //대여 예약
    public Rental reserve(ReservationRequest request) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false); //여러 작업이 한 묶음이어야 해서 false로 설정.
            try {
                Rental savedRental = reserveInTransaction(request, connection);

                connection.commit();

                //저장 완료된 Rental 반환
                return savedRental;

            } catch (SQLException e) {
                rollback(connection, e);
                throw new RuntimeException("예약 처리 중 DB 오류가 발생했습니다.", e);

            } catch (RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 처리 중 오류가 발생했습니다.", e);
        }
    }

    //예약된 렌탈건에 대한 대여 시작
    public Rental startRental(long rentalId, long memberId) {
        if (rentalId <= 0) {
            throw new IllegalArgumentException("ID는 양수여야 합니다.");
        }
        validateMemberId(memberId);

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {

                Rental rental = startRentalInTransaction(rentalId, memberId, connection);
                connection.commit();
                return rental;

            } catch (NoShowException e) {
                try {
                    connection.commit();
                } catch (SQLException commitException) {
                    rollback(connection, commitException);
                    throw new RuntimeException("노쇼 처리 확정 중 DB 오류가 발생했습니다.", commitException);
                }
                throw e;
            } catch (SQLException e) {
                rollback(connection, e);
                throw new RuntimeException("실제 대여 처리 중 DB 오류가 발생했습니다.");
            } catch (RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 처리 중 오류가 발생했습니다.");
        }
    }

    public Rental returnCar(long rentalId, long memberId) {
        if (rentalId <= 0) {
            throw new IllegalArgumentException("ID는 양수여야 합니다.");
        }
        validateMemberId(memberId);

        try (Connection connection = DBConnection.getConnection()){
            connection.setAutoCommit(false);

            try {
                Rental returnCar = getReturnCarInTransaction(
                        rentalId,
                        memberId,
                        connection
                );
                connection.commit();
                return returnCar;
            } catch (SQLException e) {
                rollback(connection, e);
                throw new RuntimeException("반납 처리 중 오류가 발생했습니다.",e);
            } catch (RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 처리 중 오류가 발생했습니다.", e);
        }
    }

    //예약 취소
    public Rental cancelReservation(long rentalId, long memberId) {
        if (rentalId <= 0) {
            throw new IllegalArgumentException("ID는 양수여야 합니다.");
        }
        validateMemberId(memberId);

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Rental canceledRental = getCanceledRentalInTransaction(rentalId, memberId, connection);
                connection.commit();
                return canceledRental;

            } catch (SQLException e) {
                rollback(connection, e);
                throw new RuntimeException("취소 처리 중 오류가 발생했습니다.", e);
            } catch (RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 처리 중 오류가 발생했습니다.", e);
        }
    }

    public int markExpiredReservationsAsNoShow() {
        try (Connection connection = DBConnection.getConnection()) {
            return rentalDao.markExpiredReservedRentalsAsNoShow(connection);
        } catch (SQLException e) {
            throw new RuntimeException("노쇼 예약 처리 중 DB 연결 오류가 발생했습니다.", e);
        }
    }

    private Rental reserveInTransaction(ReservationRequest request, Connection connection) {
        //사용자가 넘긴 값의 모양과 의미 검사
        if (request == null) {
            throw new IllegalArgumentException("예약 요청은 필수입니다.");
        }

        if (request.getMemberId() == null || request.getMemberId() <= 0) {
            throw new IllegalArgumentException("회원 ID는 양수여야 합니다.");
        }

        if (request.getCarId() == null || request.getCarId() <= 0) {
            throw new IllegalArgumentException("차량 ID는 양수여야 합니다.");
        }

        if (request.getStartTime() == null || request.getReturnTime() == null) {
            throw new IllegalArgumentException("대여 시간과 반납 시간은 필수입니다.");
        }

        //입력받은 시간 타당성 검사 -> 시작 시간이 반납 시간보다 앞인가?
        if (!request.getStartTime().isBefore(request.getReturnTime())) {
            throw new IllegalArgumentException("반납 시간은 대여 시작 시간보다 늦어야 합니다.");
        }

        if (Duration.between(request.getStartTime(), request.getReturnTime())
                .compareTo(Duration.ofMinutes(30)) < 0) {
            throw new IllegalArgumentException("대여 시간은 최소 30분 이상이어야 합니다.");
        }

        if (request.getPassengerCount() <= 0) {
            throw new IllegalArgumentException("탑승 인원은 1명 이상이어야 합니다.");
        }
        if (request.getPassengerCount() > 9) {
            throw new IllegalArgumentException("탑승 인원은 9명 이하여야 합니다.");
        }

        //1. 회원, 차량, 주차장 조회 및 검증
        //차량 행을 먼저 조회하면서, 다른사람이 예약할 수 없게 잠근다.
        Car car = carDao.findByIdForUpdate(
                connection,
                request.getCarId()
        ).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 차량입니다."));

        Member member = memberDao.findById(
                connection,
                request.getMemberId()
        ).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!member.isLicenseStatus()) {
            throw new IllegalStateException("면허가 있는 회원만 예약할 수 있습니다.");
        }

        if (car.getCarStatus() != CarStatus.AVAILABLE) {
            throw new IllegalStateException("현재 예약할 수 없는 차량입니다.");
        }

        if (car.getCurrentParkingLotId() == null) {
            throw new IllegalStateException("현재 주차 위치가 없는 차량입니다.");
        }

        ParkingLot pickupParkingLot = parkingLotDao.findByIdForUpdate(
                connection,
                car.getCurrentParkingLotId()
        ).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 출발 주차장입니다."));

        if (!pickupParkingLot.isActive()) {
            throw new IllegalStateException("현재 운영하지 않는 주차장의 차량은 예약할 수 없습니다.");
        }

        if (request.getPassengerCount() > car.getMaxPassengerCount()) {
            throw new IllegalArgumentException("차량 최대 탑승 인원을 초과했습니다.");
        }

        //시간 중복 결과
        boolean hasOverlap = rentalDao.existsOverlappingActiveRental(
                connection,
                request.getCarId(),
                request.getStartTime(),
                request.getReturnTime()
        );

        if (hasOverlap) {
            throw new IllegalStateException("해당 차량은 요청한 시간에 이미 예약되어 있습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!request.getStartTime().isAfter(now)) {
            throw new IllegalArgumentException("대여 시작 시간은 현재 시각보다 이후여야 합니다.");
        }

        //3. Rental 생성
        Rental rentalToSave = new Rental(
                request.getStartTime(), request.getReturnTime(), request.getPassengerCount(),
                request.getCarId(), request.getMemberId(),
                car.getCurrentParkingLotId()
        );

        //4. 미래 예약은 일정만 저장한다. 차량과 주차장 수량은 실제 이동 시점에 바뀐다.
        long rentalId = rentalDao.save(connection, rentalToSave);

        //id가 저장된 Rental 객체
        Rental savedRental = new Rental(
                rentalId,
                rentalToSave.getStartTime(),
                rentalToSave.getReturnTime(),
                rentalToSave.getPassengersCount(),
                rentalToSave.getActualPickupTime(),
                rentalToSave.getActualReturnTime(),
                rentalToSave.getRentalStatus(),
                rentalToSave.getCarId(),
                rentalToSave.getMemberId(),
                rentalToSave.getPickupParkingLotId()
        );
        return savedRental;
    }



    private Rental startRentalInTransaction(long rentalId, long memberId, Connection connection) {
        Rental rental = rentalDao.findByIdForUpdate(
                connection,
                rentalId
        ).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대여 내역입니다."));
        requireRentalOwner(rental, memberId);

        if (rental.getRentalStatus() != RentalStatus.RESERVED) {
            throw new IllegalStateException("예약 상태에서만 대여를 시작할 수 있습니다.");
        }

        LocalDateTime actualPickupTime = LocalDateTime.now();
        if (actualPickupTime.isBefore(rental.getStartTime())) {
            throw new IllegalStateException("예약 시작 시각 이후에 대여를 시작할 수 있습니다.");
        }

        if (!actualPickupTime.isBefore(rental.getStartTime().plus(NO_SHOW_GRACE_PERIOD))) {
            int updatedRows = rentalDao.markNoShowIfReserved(connection, rentalId);
            if (updatedRows != 1) {
                throw new IllegalStateException("노쇼 상태 변경에 실패했습니다.");
            }
            throw new NoShowException("예약 시작 후 10분이 지나 자동 취소되었습니다.");
        }

        Car car = carDao.findByIdForUpdate(
                connection,
                rental.getCarId()
        ).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 차량입니다."));

        if (car.getCarStatus() != CarStatus.AVAILABLE) {
            throw new IllegalStateException("현재 대여 가능한 차량만 대여를 시작할 수 있습니다.");
        }

        if (car.getCurrentParkingLotId() == null || !car.getCurrentParkingLotId().equals(rental.getPickupParkingLotId())) {
            throw new IllegalStateException("차량이 예약된 출발 주차장에 없습니다.");
        }

        //대여를 시작. (startRental은 Rental 상태, pickup time을 반영한다.
        int updateRows = rentalDao.startRental(connection, rentalId, actualPickupTime);

        if (updateRows != 1) {
            throw new IllegalStateException("대여 시작 상태 변경 중 오류가 발생했습니다.");
        }
        //차의 상태를 RENTED로 변경 (car의 현재 current_parking_lot_id를 삭제한다.)
        int updateCarStatus = carDao.updateStatusAndClearParkingLot(connection, rental.getCarId(), CarStatus.RENTED);

        if (updateCarStatus != 1) {
            throw new IllegalStateException("차량 상태를 변경하는 과정에서 이상이 생겼습니다.");
        }

        int decreaseCurrentParkingCount = parkingLotDao.decreaseCurrentParkingCount(connection, rental.getPickupParkingLotId());

        if (decreaseCurrentParkingCount != 1) {
            throw new IllegalStateException("출발할 주차장 카운트 변경하는 과정에서 이상이 생겼습니다.");
        }

        int increaseRentedCarCount = parkingLotDao.increaseRentedCarCount(
                connection,
                rental.getPickupParkingLotId()
        );

        if (increaseRentedCarCount != 1) {
            throw new IllegalStateException("대여 차량 수 변경 과정에서 이상이 생겼습니다.");
        }

        return new Rental(
                rental.getId(),
                rental.getStartTime(),
                rental.getReturnTime(),
                rental.getPassengersCount(),
                actualPickupTime,
                rental.getActualReturnTime(),
                RentalStatus.IN_PROGRESS,
                rental.getCarId(),
                rental.getMemberId(),
                rental.getPickupParkingLotId()
        );
    }



    private Rental getReturnCarInTransaction(
            long rentalId,
            long memberId,
            Connection connection
    ) {
        Rental rental = rentalDao.findByIdForUpdate(
                connection,
                rentalId
        ).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대여 내역입니다."));
        requireRentalOwner(rental, memberId);

        Car car = carDao.findByIdForUpdate(
                connection,
                rental.getCarId()
        ).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 차량입니다."));

        if (rental.getRentalStatus() != RentalStatus.IN_PROGRESS || car.getCarStatus() != CarStatus.RENTED) {
            throw new IllegalStateException("대여중인 상태에서만 반납할 수 있습니다.");
        }

        ParkingLot pickupParkingLot = parkingLotDao.findByIdForUpdate(
                connection,
                rental.getPickupParkingLotId()
        ).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 출발 주차장입니다."));

        int rentalUpdateRows = rentalDao.updateStatus(connection, rentalId, RentalStatus.COMPLETED);

        LocalDateTime actualReturnTime = LocalDateTime.now();
        int actualReturnTimeRows = rentalDao.updateActualReturnTime(
                connection,
                rentalId,
                actualReturnTime
        );

        int decreaseRentedCarCount = parkingLotDao.decreaseRentedCarCount(
                connection,
                rental.getPickupParkingLotId()
        );

        if (decreaseRentedCarCount != 1) {
            throw new IllegalStateException("대여 차량 수 변경 과정에서 이상이 생겼습니다.");
        }

        //픽업 주차장의 current_count를 +1 한다.
        int parkingLotCurrent = parkingLotDao.increaseCurrentCount(connection, rental.getPickupParkingLotId());

        //차의 상태를 가능한 상태로 만들고 현재 위치를 픽업 주차장으로 변경한다.
        int carUpdateRows = carDao.updateStatusAndParkingLot(
                connection,
                rental.getCarId(),
                CarStatus.AVAILABLE,
                rental.getPickupParkingLotId(),
                pickupParkingLot.getAddress()
        );

        if (rentalUpdateRows != 1
                || actualReturnTimeRows != 1
                || carUpdateRows != 1
                || parkingLotCurrent != 1) {
            throw new RuntimeException("정상적으로 변경되지 않았습니다.");
        }

        return new Rental(
                rental.getId(),
                rental.getStartTime(),
                rental.getReturnTime(),
                rental.getPassengersCount(),
                rental.getActualPickupTime(),
                actualReturnTime,
                RentalStatus.COMPLETED,
                rental.getCarId(),
                rental.getMemberId(),
                rental.getPickupParkingLotId()
        );
    }

    private Rental getCanceledRentalInTransaction(
            long rentalId,
            long memberId,
            Connection connection
    ) {
        //차량 ID를 알아내기 위한 조회
        Rental rental = rentalDao.findByIdForUpdate(
                connection,
                rentalId
        ).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대여 내역입니다."));
        requireRentalOwner(rental, memberId);

        if (rental.getRentalStatus() != RentalStatus.RESERVED) {
            throw new IllegalStateException("예약 상태에서만 취소할 수 있습니다.");
        }

        //Rental의 상태를 canceled로 변경
        int rentalUpdatedRows = rentalDao.updateStatus(connection, rentalId, RentalStatus.CANCELED);

        if (rentalUpdatedRows != 1) {
            throw new RuntimeException("정상적으로 변경되지 않았습니다.");
        }

        //자바 객체에서도 cancel로 변경
        Rental canceledRental = new Rental(
                rental.getId(),
                rental.getStartTime(),
                rental.getReturnTime(),
                rental.getPassengersCount(),
                rental.getActualPickupTime(),
                rental.getActualReturnTime(),
                RentalStatus.CANCELED,
                rental.getCarId(),
                rental.getMemberId(),
                rental.getPickupParkingLotId()
        );
        return canceledRental;
    }



    private void validateMemberId(long memberId) {
        if (memberId <= 0) {
            throw new IllegalArgumentException("회원 ID는 양수여야 합니다.");
        }
    }

    private void requireRentalOwner(Rental rental, long memberId) {
        if (!rental.getMemberId().equals(memberId)) {
            throw new IllegalStateException("본인 예약만 변경할 수 있습니다.");
        }
    }

    private void rollback(Connection connection, Exception e) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            e.addSuppressed(rollbackException);
        }
    }

    private static class NoShowException extends IllegalStateException {
        private NoShowException(String message) {
            super(message);
        }
    }
}
