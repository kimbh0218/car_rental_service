package org.example.dao;

import org.example.config.DBConnection;
import org.example.domain.Rental;
import org.example.domain.RentalStatus;
import org.example.domain.RentalWithMember;
import org.example.dto.CarScheduleItem;
import org.example.dto.MyRentalItem;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RentalDao {

    public List<MyRentalItem> findMyRentalsByMemberId(long memberId) {
        String sql = """
                SELECT r.id AS rental_id,
                       r.start_time,
                       r.return_time,
                       r.passengers_count,
                       r.rental_status,
                       c.name AS car_name,
                       pickup_lot.address AS current_address,
                       pickup_lot.name AS current_parking_lot_name
                FROM rental r
                JOIN car c
                  ON r.car_id = c.id
                JOIN parking_lot pickup_lot
                  ON r.pickup_parking_lot_id = pickup_lot.parking_lot_id
                WHERE r.member_id = ?
                ORDER BY r.start_time DESC, r.id DESC
                """;

        List<MyRentalItem> rentals = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, memberId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rentals.add(new MyRentalItem(
                            resultSet.getLong("rental_id"),
                            resultSet.getString("car_name"),
                            resultSet.getString("current_parking_lot_name"),
                            resultSet.getString("current_address"),
                            resultSet.getInt("passengers_count"),
                            resultSet.getTimestamp("start_time").toLocalDateTime(),
                            resultSet.getTimestamp("return_time").toLocalDateTime(),
                            RentalStatus.valueOf(resultSet.getString("rental_status"))
                    ));
                }
            }

            return rentals;
        } catch (SQLException e) {
            throw new RuntimeException("내 예약 목록 조회 중 DB 오류가 발생했습니다.", e);
        }
    }

    public List<CarScheduleItem> findScheduleByCarAndPeriod(
            long carId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        String sql = """
                SELECT start_time, return_time, rental_status
                FROM rental
                WHERE car_id = ?
                  AND rental_status IN (?, ?)
                  AND start_time < ?
                  AND return_time > ?
                ORDER BY start_time
                """;

        List<CarScheduleItem> schedule = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, carId);
            statement.setString(2, RentalStatus.RESERVED.name());
            statement.setString(3, RentalStatus.IN_PROGRESS.name());
            statement.setTimestamp(4, Timestamp.valueOf(to));
            statement.setTimestamp(5, Timestamp.valueOf(from));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    schedule.add(new CarScheduleItem(
                            resultSet.getTimestamp("start_time").toLocalDateTime(),
                            resultSet.getTimestamp("return_time").toLocalDateTime(),
                            RentalStatus.valueOf(resultSet.getString("rental_status"))
                    ));
                }
            }

            return schedule;
        } catch (SQLException e) {
            throw new RuntimeException("차량 예약 현황 조회 중 DB 오류가 발생했습니다.", e);
        }
    }

    public long save(Rental rental) {
        String sql = """
                INSERT INTO rental (
                    start_time,
                    return_time,
                    passengers_count,
                    rental_status,
                    car_id,
                    pickup_parking_lot_id,
                    member_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS
                )
        ) {
            statement.setTimestamp(1, Timestamp.valueOf(rental.getStartTime()));
            statement.setTimestamp(2, Timestamp.valueOf(rental.getReturnTime()));
            statement.setInt(3, rental.getPassengersCount());
            statement.setString(4, rental.getRentalStatus().name());
            statement.setLong(5, rental.getCarId());
            statement.setLong(6, rental.getPickupParkingLotId());
            statement.setLong(7, rental.getMemberId());

            int affectedRows = statement.executeUpdate();

            if (affectedRows != 1) {
                throw new SQLException("대여 등록에 실패했습니다.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }

            throw new SQLException("생성된 rental id를 받지 못했습니다.");

        } catch (SQLException e) {
            throw new RuntimeException("대여 저장 중 DB 오류가 발생했습니다.", e);
        }
    }

    public List<Rental> findAll() {
        String sql = """
                SELECT id, start_time, return_time, passengers_count,
                       rental_status, car_id, pickup_parking_lot_id,
                       member_id,
                       actual_pickup_time, actual_return_time
                FROM rental
                """;

        List<Rental> rentalList = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                rentalList.add(mapRental(resultSet));
            }

            return rentalList;

        } catch (SQLException e) {
            throw new RuntimeException("대여 목록 조회 중 DB 오류가 발생했습니다.", e);
        }
    }

    public Optional<Rental> findById(long id) {
        String sql = """
                SELECT id, start_time, return_time, passengers_count,
                       rental_status, car_id, pickup_parking_lot_id,
                       member_id,
                       actual_pickup_time, actual_return_time
                FROM rental
                WHERE id = ?
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRental(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("대여 조회 중 DB 오류가 발생했습니다.", e);
        }
    }

    //트랜잭션용 findByID
    public Optional<Rental> findByIdForUpdate(Connection connection, Long id) {
        String sql = """
                SELECT id, start_time, return_time, passengers_count,
                       rental_status, car_id, pickup_parking_lot_id,
                       member_id,
                       actual_pickup_time, actual_return_time
                FROM rental
                WHERE id = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRental(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("대여 조회 중 DB 오류가 발생했습니다.", e);
        }
    }
    public int updateStatus(long id, RentalStatus newStatus) {
        String sql = """
                UPDATE rental
                SET rental_status = ?
                WHERE id = ?
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, newStatus.name());
            statement.setLong(2, id);

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("대여 상태 변경 중 DB 오류가 발생했습니다.", e);
        }
    }

    //새로 connection하지 않는 트랜잭션용 버전
    public int updateStatus(Connection connection, long id, RentalStatus newStatus) {
        String sql = """
                UPDATE rental
                SET rental_status = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setString(1, newStatus.name());
            statement.setLong(2,id);

            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("대여 상태 변경 중 DB 오류가 발생했습니다.", e);        }
    }

    private Rental mapRental(ResultSet resultSet) throws SQLException {
        return new Rental(
                resultSet.getLong("id"),
                toLocalDateTime(resultSet, "start_time"),
                toLocalDateTime(resultSet, "return_time"),
                resultSet.getInt("passengers_count"),
                toLocalDateTime(resultSet, "actual_pickup_time"),
                toLocalDateTime(resultSet, "actual_return_time"),
                RentalStatus.valueOf(resultSet.getString("rental_status")),
                resultSet.getLong("car_id"),
                resultSet.getLong("member_id"),
                resultSet.getLong("pickup_parking_lot_id")
        );
    }

    //실제 대여시간 반영
    public int updateActualPickupTime(Connection connection,long rentalId, LocalDateTime actualPickupTime) {
        String sql = """
            UPDATE rental
            SET actual_pickup_time = ?
            WHERE id = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(actualPickupTime));
            statement.setLong(2, rentalId);

            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("실제 픽업 시간 변경 중 DB 오류가 발생했습니다.", e);
        }
    }

    public int markNoShowIfReserved(Connection connection, long rentalId) {
        String sql = """
                UPDATE rental
                SET rental_status = ?
                WHERE id = ?
                  AND rental_status = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, RentalStatus.NO_SHOW.name());
            statement.setLong(2, rentalId);
            statement.setString(3, RentalStatus.RESERVED.name());

            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("노쇼 상태 변경 중 DB 오류가 발생했습니다.", e);
        }
    }

    public int markExpiredReservedRentalsAsNoShow(Connection connection) {
        String sql = """
                UPDATE rental
                SET rental_status = ?
                WHERE rental_status = ?
                  AND start_time <= CURRENT_TIMESTAMP - INTERVAL 10 MINUTE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, RentalStatus.NO_SHOW.name());
            statement.setString(2, RentalStatus.RESERVED.name());

            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("노쇼 예약 일괄 처리 중 오류가 발생했습니다.", e);
        }
    }

    //실제 반납시간 반영
    public int updateActualReturnTime(Connection connection, long rentalId, LocalDateTime actualReturnTime) {
        String sql = """
            UPDATE rental
            SET actual_return_time = ?
            WHERE id = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(actualReturnTime));
            statement.setLong(2, rentalId);

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("실제 반납 시간 변경 중 DB 오류가 발생했습니다.", e);
        }
    }

    //시간 중복 검사
    public boolean existsOverlappingActiveRental(
            Connection connection,
            long carId,
            LocalDateTime requestedStartTime,
            LocalDateTime requestedReturnTime
    ) {
        String sql = """
            SELECT 1
            FROM rental
            WHERE car_id = ?
              AND rental_status IN (?, ?)
              AND start_time < ?
              AND return_time > ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, carId);
            statement.setString(2, RentalStatus.RESERVED.name());
            statement.setString(3, RentalStatus.IN_PROGRESS.name());

            // 기존 시작 < 새 반납
            statement.setTimestamp(4, Timestamp.valueOf(requestedReturnTime));

            // 기존 반납 > 새 시작
            statement.setTimestamp(5, Timestamp.valueOf(requestedStartTime));

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException("대여 시간 중복 확인 중 DB 오류가 발생했습니다.", e);
        }
    }

    public boolean hasActiveRentalByCar(Connection connection, long carId){
        String sql = """ 
                    SELECT 1
                    FROM rental
                    WHERE car_id = ?
                        AND rental_status IN(?,?)
                        LIMIT 1
                    """;

        try (PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setLong(1, carId);
            statement.setString(2, RentalStatus.RESERVED.name());
            statement.setString(3, RentalStatus.IN_PROGRESS.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("차량 예약 확인중 DB 오류가 발생했습니다.", e);
        }
    }

    //상태변경과 실제 픽업시간 변경 메소드
    public int startRental (
            Connection connection,
            long rentalId,
            LocalDateTime actualPickupTime

    ) {
        String sql = """
                UPDATE rental
                SET rental_status = ?,
                    actual_pickup_time = ?
                WHERE id = ?
                    AND rental_status = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setString(1,RentalStatus.IN_PROGRESS.name());
            statement.setTimestamp(2,Timestamp.valueOf(actualPickupTime));
            statement.setLong(3,rentalId);
            statement.setString(4,RentalStatus.RESERVED.name());

            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("대여 시작 상태 변경 중 오류가 발생했습니다.", e);
        }

    }

    //트랜잭션용 저장 메소드
    public long save(Connection connection, Rental rental) {
        String sql = """
            INSERT INTO rental (
                start_time,
                return_time,
                passengers_count,
                rental_status,
                car_id,
                pickup_parking_lot_id,
                member_id
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (
                PreparedStatement statement = connection.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {
            statement.setTimestamp(1, Timestamp.valueOf(rental.getStartTime()));
            statement.setTimestamp(2, Timestamp.valueOf(rental.getReturnTime()));
            statement.setInt(3, rental.getPassengersCount());
            statement.setString(4, rental.getRentalStatus().name());
            statement.setLong(5, rental.getCarId());
            statement.setLong(6, rental.getPickupParkingLotId());
            statement.setLong(7, rental.getMemberId());

            int affectedRows = statement.executeUpdate();

            if (affectedRows != 1) {
                throw new SQLException("대여 등록에 실패했습니다.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }

            throw new SQLException("생성된 rental id를 받지 못했습니다.");

        } catch (SQLException e) {
            throw new RuntimeException("대여 저장 중 DB 오류가 발생했습니다.", e);
        }
    }

    //대여 목록에 회원 이름을 표시
    public List<RentalWithMember> findAllWithMemberName() {
        String sql = """
            SELECT r.id AS rental_id,
                   r.start_time,
                   r.return_time,
                   r.passengers_count,
                   r.rental_status,
                   r.car_id,
                   r.member_id,
                   r.pickup_parking_lot_id,
                   r.actual_pickup_time,
                   r.actual_return_time,
                   m.name AS member_name
            FROM rental r
            JOIN member m
              ON r.member_id = m.id
            ORDER BY r.start_time DESC
            """;

        List<RentalWithMember> rentalList = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                Timestamp actualPickupTimestamp =
                        resultSet.getTimestamp("actual_pickup_time");

                Timestamp actualReturnTimestamp =
                        resultSet.getTimestamp("actual_return_time");

                Rental rental = new Rental(
                        resultSet.getLong("rental_id"),
                        resultSet.getTimestamp("start_time").toLocalDateTime(),
                        resultSet.getTimestamp("return_time").toLocalDateTime(),
                        resultSet.getInt("passengers_count"),
                        actualPickupTimestamp == null
                                ? null
                                : actualPickupTimestamp.toLocalDateTime(),
                        actualReturnTimestamp == null
                                ? null
                                : actualReturnTimestamp.toLocalDateTime(),
                        RentalStatus.valueOf(
                                resultSet.getString("rental_status")
                        ),
                        resultSet.getLong("car_id"),
                        resultSet.getLong("member_id"),
                        resultSet.getLong("pickup_parking_lot_id")
                );

                RentalWithMember rentalWithMember = new RentalWithMember(
                        rental,
                        resultSet.getString("member_name")
                );

                rentalList.add(rentalWithMember);
            }

            return rentalList;

        } catch (SQLException e) {
            throw new RuntimeException("회원 이름을 포함한 대여 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    private LocalDateTime toLocalDateTime(
            ResultSet resultSet,
            String columnName
    ) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);

        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
