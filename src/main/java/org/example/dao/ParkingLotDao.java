package org.example.dao;

import org.example.config.DBConnection;
import org.example.domain.ParkingLot;
import org.example.domain.ParkingLotStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParkingLotDao {

    public int save(String name, int maxParkingCount, String address) {
        String sql = """
                INSERT INTO parking_lot(name, max_parking_count, address)
                values(?, ?, ?)
                """;
        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)

                ){
            statement.setString(1, name);
            statement.setInt(2, maxParkingCount);
            statement.setString(3, address);

            int affectedRows = statement.executeUpdate();

            if (affectedRows != 1) {
                throw new SQLException("주차장 정보 등록에 실패했습니다.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            } throw new SQLException("생성된 parking_lot_id를 받지 못했습니다.");
        } catch (SQLException e) {
            if (isDuplicateParkingLot(e)) {
                throw new DuplicateParkingLotException(e);
            }
            throw new RuntimeException("주차장 저장 중 DB 오류가 발생했습니다.",e);
        }
    }

    public List<ParkingLot> findAll() {
        String sql = """
            SELECT parking_lot_id,
                   name,
                   max_parking_count,
                   address,
                   current_parking_count,
                   rented_car_count,
                   parking_lot_status
            FROM parking_lot
            ORDER BY parking_lot_id
            """;

        List<ParkingLot> parkingLots = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                parkingLots.add(mapParkingLot(resultSet));
            }
            return parkingLots;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "주차장 목록 조회 중 DB 오류가 발생했습니다.",e);
        }
    }

    public List<ParkingLot> findAllActive() {
        String sql = """
                SELECT parking_lot_id,
                       name,
                       max_parking_count,
                       address,
                       current_parking_count,
                       rented_car_count,
                       parking_lot_status
                FROM parking_lot
                WHERE parking_lot_status = ?
                ORDER BY parking_lot_id
                """;

        List<ParkingLot> parkingLots = new ArrayList<>();
        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, ParkingLotStatus.ACTIVE.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    parkingLots.add(mapParkingLot(resultSet));
                }
            }
            return parkingLots;
        } catch (SQLException e) {
            throw new RuntimeException("운영 중인 주차장 목록 조회에 실패했습니다.", e);
        }
    }

    //주차장 조회 메소드
    public Optional<ParkingLot> findById(long parkingLotId) {
        String sql = """
        SELECT parking_lot_id,
               name,
               max_parking_count,
               address,
               current_parking_count,
               rented_car_count,
               parking_lot_status
        FROM parking_lot
        WHERE parking_lot_id = ?
        """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, parkingLotId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapParkingLot(resultSet));
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("주차장 조회 중 DB 오류가 발생했습니다.", e);
        }
    }

    //주차장 행을 잠그는 조회 메소드
    public Optional<ParkingLot> findByIdForUpdate(
            Connection connection,
            long parkingLotId
    ) {
        String sql = """
            SELECT parking_lot_id,
                   name,
                   max_parking_count,
                   address,
                   current_parking_count,
                   rented_car_count,
                   parking_lot_status
            FROM parking_lot
            WHERE parking_lot_id = ?
            FOR UPDATE
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, parkingLotId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapParkingLot(resultSet));
            }

        } catch (SQLException e) {
            throw new RuntimeException("주차장 조회 중 DB 오류가 발생했습니다.", e);
        }
    }


    //대여 중인 차량의 반납 자리를 보장한다.
    public int increaseRentedCarCount(
            Connection connection,
            long parkingLotId
    ) {
        String sql = """
            UPDATE parking_lot
            SET rented_car_count = rented_car_count + 1
            WHERE parking_lot_id = ?
              AND current_parking_count + rented_car_count
                    < max_parking_count
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, parkingLotId);

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("예약 주차 대수 변경 중 DB 오류가 발생했습니다.", e);
        }
    }

    //반납 완료 후 보장했던 자리를 해제한다.
    public int decreaseRentedCarCount(
            Connection connection,
            long parkingLotId
    ){
        String sql = """
                UPDATE parking_lot
                SET rented_car_count = rented_car_count - 1
                WHERE parking_lot_id = ?
                AND rented_car_count > 0
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1,parkingLotId);

            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("대여 차량 수 변경 중 DB 오류가 발생했습니다.", e);
        }
    }

    //예약이 아닌 대여 이후 감소시킬 카운트
    public int decreaseCurrentParkingCount(Connection connection, Long parkingLotId) {
        String sql = """
            UPDATE parking_lot
            SET current_parking_count = current_parking_count - 1
            WHERE parking_lot_id = ?
              AND current_parking_count > 0
            """;

        try (
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, parkingLotId);

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("현재 주차 차량 수 변경 중 DB 오류가 발생했습니다.", e);
        }
    }

    //현재 주차장의 카운트를 증가
    public int increaseCurrentCount(Connection connection, Long parkingLotId) {
        String sql = """
            UPDATE parking_lot
            SET current_parking_count = current_parking_count + 1
            WHERE parking_lot_id = ?
                    AND current_parking_count + rented_car_count
                    < max_parking_count
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, parkingLotId);

            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException ("현재 주차장의 카운트를 증가시키는 중 DB오류가 발생했습니다.", e);
        }
    }

    public int updateInformation(
            Connection connection,
            long parkingLotId,
            String name,
            String address,
            int maxParkingCount
    ) {
        String sql = """
                UPDATE parking_lot
                SET name = ?,
                    address = ?,
                    max_parking_count = ?
                WHERE parking_lot_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, address);
            statement.setInt(3, maxParkingCount);
            statement.setLong(4, parkingLotId);
            return statement.executeUpdate();
        } catch (SQLException e) {
            if (isDuplicateParkingLot(e)) {
                throw new DuplicateParkingLotException(e);
            }
            throw new RuntimeException("주차장 정보 수정 중 DB 오류가 발생했습니다.", e);
        }
    }

    public int updateStatus(
            Connection connection,
            long parkingLotId,
            ParkingLotStatus status
    ) {
        String sql = """
                UPDATE parking_lot
                SET parking_lot_status = ?
                WHERE parking_lot_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setLong(2, parkingLotId);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("주차장 상태 변경 중 DB 오류가 발생했습니다.", e);
        }
    }

    private ParkingLot mapParkingLot(ResultSet resultSet) throws SQLException {
        return new ParkingLot(
                resultSet.getLong("parking_lot_id"),
                resultSet.getString("name"),
                resultSet.getInt("max_parking_count"),
                resultSet.getString("address"),
                resultSet.getInt("current_parking_count"),
                resultSet.getInt("rented_car_count"),
                ParkingLotStatus.valueOf(resultSet.getString("parking_lot_status"))
        );
    }

    private boolean isDuplicateParkingLot(SQLException exception) {
        return "23000".equals(exception.getSQLState())
                && exception.getErrorCode() == 1062;
    }
}
