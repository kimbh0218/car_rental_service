package org.example.service;

import org.example.config.DBConnection;
import org.example.dao.CarDao;
import org.example.dao.CarImageDao;
import org.example.domain.CarImage;
import org.example.domain.StoredCarImage;

import java.sql.Connection;
import java.sql.SQLException;

public class CarImageService {
    private final CarDao carDao;
    private final CarImageDao carImageDao;

    public CarImageService(CarDao carDao, CarImageDao carImageDao) {
        this.carDao = carDao;
        this.carImageDao = carImageDao;
    }

    public CarImage findById(long imageId) {
        if (imageId <= 0) {
            throw new IllegalArgumentException("이미지 정보가 올바르지 않습니다.");
        }

        return carImageDao.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다."));
    }

    public String replacePrimaryImage(long carId, StoredCarImage replacement) {
        if (carId <= 0 || replacement == null) {
            throw new IllegalArgumentException("차량 이미지 정보가 올바르지 않습니다.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                carDao.findByIdForUpdate(connection, carId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("존재하지 않는 차량입니다."));

                CarImage current = carImageDao.findPrimaryByCarIdForUpdate(
                        connection,
                        carId
                ).orElse(null);

                if (current == null) {
                    carImageDao.save(connection, carId, replacement);
                } else {
                    carImageDao.update(connection, current, replacement);
                }

                connection.commit();
                return current == null ? null : current.getStorageKey();
            } catch (RuntimeException e) {
                rollback(connection, e);
                throw e;
            } catch (SQLException e) {
                rollback(connection, e);
                throw new RuntimeException("차량 이미지 변경 처리 중 DB 오류가 발생했습니다.", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 처리 중 오류가 발생했습니다.", e);
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
