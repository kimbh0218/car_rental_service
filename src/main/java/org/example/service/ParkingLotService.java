package org.example.service;

import org.example.config.DBConnection;
import org.example.dao.ParkingLotDao;
import org.example.dao.DuplicateParkingLotException;
import org.example.domain.ParkingLot;
import org.example.domain.ParkingLotStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ParkingLotService {

    private final ParkingLotDao parkingLotDao;

    public ParkingLotService(ParkingLotDao parkingLotDao) {
        this.parkingLotDao = parkingLotDao;
    }

    public List<ParkingLot> findAllParkingLots() {
        return parkingLotDao.findAll();
    }

    public List<ParkingLot> findAllActiveParkingLots() {
        return parkingLotDao.findAllActive();
    }

    public long registerParkingLot(
            String name,
            int maxParkingCount,
            String address
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("주차장명은 필수입니다.");
        }

        if (maxParkingCount <= 0) {
            throw new IllegalArgumentException("최대 주차 수는 1 이상이어야 합니다.");
        }

        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("주차장 주소는 필수입니다.");
        }

        try {
            return parkingLotDao.save(
                    name.trim(),
                    maxParkingCount,
                    address.trim()
            );
        } catch (DuplicateParkingLotException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public ParkingLot findParkingLotById(long parkingLotId) {
        if (parkingLotId <= 0) {
            throw new IllegalArgumentException("주차장 ID는 양수여야 합니다.");
        }

        return parkingLotDao.findById(parkingLotId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 주차장입니다.")
                );
    }

    public ParkingLot findActiveParkingLotById(long parkingLotId) {
        ParkingLot parkingLot = findParkingLotById(parkingLotId);
        if (!parkingLot.isActive()) {
            throw new IllegalArgumentException("현재 운영하지 않는 주차장입니다.");
        }
        return parkingLot;
    }

    public void updateParkingLot(
            long parkingLotId,
            String name,
            String address,
            int maxParkingCount
    ) {
        validateParkingLotInput(name, maxParkingCount, address);

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                ParkingLot parkingLot = parkingLotDao.findByIdForUpdate(
                        connection,
                        parkingLotId
                ).orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 주차장입니다.")
                );

                int occupiedCarCount = parkingLot.getCurrentParkingCount()
                        + parkingLot.getRentedCarCount();
                if (maxParkingCount < occupiedCarCount) {
                    throw new IllegalArgumentException(
                            "최대 수용 대수는 현재 주차·대여 차량 합계 "
                                    + occupiedCarCount + "대 이상이어야 합니다."
                    );
                }

                if (parkingLotDao.updateInformation(
                        connection,
                        parkingLotId,
                        name.trim(),
                        address.trim(),
                        maxParkingCount
                ) != 1) {
                    throw new IllegalStateException("주차장 정보 수정에 실패했습니다.");
                }

                connection.commit();
            } catch (DuplicateParkingLotException e) {
                rollback(connection, e);
                throw new IllegalArgumentException(e.getMessage(), e);
            } catch (RuntimeException e) {
                rollback(connection, e);
                throw e;
            } catch (SQLException e) {
                rollback(connection, e);
                throw new RuntimeException("주차장 정보 수정 중 DB 오류가 발생했습니다.", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 처리 중 오류가 발생했습니다.", e);
        }
    }

    public void changeParkingLotStatus(long parkingLotId, ParkingLotStatus newStatus) {
        if (parkingLotId <= 0) {
            throw new IllegalArgumentException("주차장 정보가 올바르지 않습니다.");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("변경할 주차장 상태는 필수입니다.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ParkingLot parkingLot = parkingLotDao.findByIdForUpdate(connection, parkingLotId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주차장입니다."));

                if (parkingLot.getStatus() == newStatus) {
                    throw new IllegalArgumentException("이미 해당 상태의 주차장입니다.");
                }

                if (newStatus == ParkingLotStatus.INACTIVE
                        && (parkingLot.getCurrentParkingCount() > 0
                        || parkingLot.getRentedCarCount() > 0)) {
                    throw new IllegalStateException(
                            "현재 주차 차량과 대여 중 차량이 모두 0대일 때만 주차장을 비활성화할 수 있습니다."
                    );
                }

                if (parkingLotDao.updateStatus(connection, parkingLotId, newStatus) != 1) {
                    throw new IllegalStateException("주차장 상태 변경에 실패했습니다.");
                }

                connection.commit();
            } catch (RuntimeException e) {
                rollback(connection, e);
                throw e;
            } catch (SQLException e) {
                rollback(connection, e);
                throw new RuntimeException("주차장 상태 변경 중 DB 오류가 발생했습니다.", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 처리 중 오류가 발생했습니다.", e);
        }
    }

    private void validateParkingLotInput(
            String name,
            int maxParkingCount,
            String address
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("주차장명은 필수입니다.");
        }

        if (maxParkingCount <= 0) {
            throw new IllegalArgumentException("최대 주차 수는 1 이상이어야 합니다.");
        }

        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("주차장 주소는 필수입니다.");
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
