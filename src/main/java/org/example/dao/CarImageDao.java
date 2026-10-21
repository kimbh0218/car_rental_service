package org.example.dao;

import org.example.config.DBConnection;
import org.example.domain.CarImage;
import org.example.domain.StoredCarImage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class CarImageDao {

    public void save(Connection connection, long carId, StoredCarImage image) {
        String sql = """
                INSERT INTO car_image (
                    car_id,
                    storage_key,
                    original_file_name,
                    content_type,
                    size_bytes,
                    sort_order
                )
                VALUES (?, ?, ?, ?, ?, 0)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, carId);
            statement.setString(2, image.getStorageKey());
            statement.setString(3, image.getOriginalFileName());
            statement.setString(4, image.getContentType());
            statement.setLong(5, image.getSizeBytes());

            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("차량 이미지 정보 저장에 실패했습니다.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("차량 이미지 정보 저장 중 DB 오류가 발생했습니다.", e);
        }
    }

    public Optional<CarImage> findById(long imageId) {
        String sql = """
                SELECT id, car_id, storage_key, original_file_name,
                       content_type, size_bytes, sort_order
                FROM car_image
                WHERE id = ?
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, imageId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(map(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("차량 이미지 조회 중 DB 오류가 발생했습니다.", e);
        }
    }

    public Optional<CarImage> findPrimaryByCarIdForUpdate(
            Connection connection,
            long carId
    ) {
        String sql = """
                SELECT id, car_id, storage_key, original_file_name,
                       content_type, size_bytes, sort_order
                FROM car_image
                WHERE car_id = ?
                  AND sort_order = 0
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, carId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(map(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("차량 대표 이미지 조회 중 DB 오류가 발생했습니다.", e);
        }
    }

    public void update(
            Connection connection,
            CarImage image,
            StoredCarImage replacement
    ) {
        String sql = """
                UPDATE car_image
                SET storage_key = ?,
                    original_file_name = ?,
                    content_type = ?,
                    size_bytes = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, replacement.getStorageKey());
            statement.setString(2, replacement.getOriginalFileName());
            statement.setString(3, replacement.getContentType());
            statement.setLong(4, replacement.getSizeBytes());
            statement.setLong(5, image.getId());

            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("차량 이미지 정보 변경에 실패했습니다.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("차량 이미지 정보 변경 중 DB 오류가 발생했습니다.", e);
        }
    }

    private CarImage map(ResultSet resultSet) throws SQLException {
        return new CarImage(
                resultSet.getLong("id"),
                resultSet.getLong("car_id"),
                resultSet.getString("storage_key"),
                resultSet.getString("original_file_name"),
                resultSet.getString("content_type"),
                resultSet.getLong("size_bytes"),
                resultSet.getInt("sort_order")
        );
    }
}
