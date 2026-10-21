package org.example.dao;

import org.example.config.DBConnection;
import org.example.domain.Car;
import org.example.domain.CarImage;
import org.example.domain.CarStatus;
import org.example.domain.CarWithParkingLot;
import org.example.domain.ParkingLot;
import org.example.domain.ParkingLotStatus;
import org.example.domain.RentalStatus;
import org.example.dto.ParkingLotAvailability;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
public class CarDao {
    public int save(
            String name,
            int maxPassengerCount,
            int pricePerKm,
            int currentParkingLotId,
            String currentAddress
    ) {
        String sql = """
            INSERT INTO car (
                name,
                max_passenger_count,
                price_per_km,
                current_parking_lot_id,
                current_address
            )
            VALUES (?, ?, ?, ?, ?)
            """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS
                )
        ) {
            statement.setString(1, name);
            statement.setInt(2, maxPassengerCount);
            statement.setInt(3, pricePerKm);
            statement.setInt(4, currentParkingLotId);
            statement.setString(5, currentAddress);

            int affectedRows = statement.executeUpdate();

            if (affectedRows != 1) {
                throw new SQLException("차량 정보 등록에 실패하였습니다.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            throw new SQLException("생성된 car_id를 받지 못했습니다.");

        } catch (SQLException e) {
            throw new RuntimeException("차량 저장 중 DB 오류가 발생하였습니다.", e);
        }
    }

    public int save(
            Connection connection,
            String name,
            int maxPassengerCount,
            int pricePerKm,
            long currentParkingLotId,
            String currentAddress
    ) {
        String sql = """
                INSERT INTO car (
                    name,
                    max_passenger_count,
                    price_per_km,
                    current_parking_lot_id,
                    current_address
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            statement.setString(1,name);
            statement.setInt(2,maxPassengerCount);
            statement.setInt(3, pricePerKm);
            statement.setLong(4, currentParkingLotId);
            statement.setString(5, currentAddress);

            int affectedRows = statement.executeUpdate();

            if (affectedRows != 1) {
                throw new IllegalStateException("차량 등록에 실패했습니다.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            throw new SQLException("생성된 차량 ID를 받지 못했습니다.");
        } catch (SQLException e ) {
            throw new RuntimeException("차량 저장 중 DB 오류가 발생했습니다.",e);
        }
    }

    public List<Car> findAll() {
        String sql = """
                SELECT * FROM car            
                """;

        List<Car> carList = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();
                ) {
                while (resultSet.next()) {
                    Long currentParkingLotId =
                            resultSet.getObject("current_parking_lot_id", Long.class);
                    Car car = new Car(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            resultSet.getInt("max_passenger_count"),
                            resultSet.getInt("price_per_km"),
                            currentParkingLotId,
                            resultSet.getString("current_address"),
                            CarStatus.valueOf(
                                    resultSet.getString("car_status")
                            )
                    );
                    carList.add(car);
                }

                return carList;
            } catch (SQLException e) {
                throw new RuntimeException("차량 목록 조회 중 DB 오류가 발생했습니다.", e);
        }
    }

    public Optional<Car> findById(long id) {
        String sql = """
                SELECT id, name, max_passenger_count, price_per_km, current_parking_lot_id, current_address, car_status
                FROM car
                WHERE id = ?
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1,id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Long currentParkingLotId =
                            resultSet.getObject("current_parking_lot_id", Long.class);
                    Car car = new Car(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            resultSet.getInt("max_passenger_count"),
                            resultSet.getInt("price_per_km"),
                            currentParkingLotId,
                            resultSet.getString("current_address"),
                            CarStatus.valueOf(
                                    resultSet.getString("car_status")
                            )
                    );
                    return Optional.of(car);
                }
                return Optional.empty();
            }
            } catch (SQLException e) {
                throw new RuntimeException("차량 조회 중 DB 오류가 발생했습니다.",e);
            }
    }

    //Service에서 사용하기 위해 Connection을 매개변수로 받고 사용하도록 한다.
    public Optional<Car> findById(Connection connection, long id) {
        String sql = """
                SELECT id, name, max_passenger_count, price_per_km, current_parking_lot_id, current_address, car_status
                FROM car
                WHERE id = ?
                """;

        try (
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ){
            preparedStatement.setLong(1,id);

            try(ResultSet resultSet = preparedStatement.executeQuery()){
                if (resultSet.next()) {
                    Long currentParkingLotId =
                            resultSet.getObject("current_parking_lot_id", Long.class);
                    Car car = new Car(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            resultSet.getInt("max_passenger_count"),
                            resultSet.getInt("price_per_km"),
                            currentParkingLotId,
                            resultSet.getString("current_address"),
                            CarStatus.valueOf(
                                    resultSet.getString("car_status")
                            )
                    );
                    return Optional.of(car);
                }
                return Optional.empty();
            }
        }catch (SQLException e) {
            throw new RuntimeException("차량 조회 중 DB 오류가 발생했습니다.",e);
        }
    }

    //예약 중 동시성을 막기 위해 차량 행을 잠그는 조회 메소드
    public Optional<Car> findByIdForUpdate(Connection connection, long id) {
        String sql = """
                SELECT id, name, max_passenger_count, price_per_km, current_parking_lot_id, current_address, car_status
                FROM car
                WHERE id = ?
                FOR UPDATE
                """;

        try (
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ){
            preparedStatement.setLong(1,id);

            try(ResultSet resultSet = preparedStatement.executeQuery()){
                if (resultSet.next()) {
                    Long currentParkingLotId =
                            resultSet.getObject("current_parking_lot_id", Long.class);
                    Car car = new Car(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            resultSet.getInt("max_passenger_count"),
                            resultSet.getInt("price_per_km"),
                            currentParkingLotId,
                            resultSet.getString("current_address"),
                            CarStatus.valueOf(
                                    resultSet.getString("car_status")
                            )
                    );
                    return Optional.of(car);
                }
                return Optional.empty();
            }
        }catch (SQLException e) {
            throw new RuntimeException("차량 조회 중 DB 오류가 발생했습니다.",e);
        }
    }

    public int updateStatus(long id, CarStatus newStatus) {
        String sql = """
                UPDATE car
                SET car_status = ?
                WHERE id = ?
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ) {

            statement.setString(1,newStatus.name());
            statement.setLong(2,id);

            int result = statement.executeUpdate();
            return result;

        } catch (SQLException e) {
            throw new RuntimeException("업데이트 중 DB에서 오류가 발생했습니다.",e);
        }
    }

    //차량 상태 업데이트 Service에서 사용 목적
    public int updateStatus(
            Connection connection,
            long carId,
            CarStatus newStatus
    ) {
        String sql = """
            UPDATE car
            SET car_status = ?
            WHERE id = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newStatus.name());
            statement.setLong(2, carId);

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("차량 상태 변경 중 DB 오류가 발생했습니다.", e);
        }
    }

    public int updatePricePerKm(long carId, int pricePerKm) {
        String sql = """
                UPDATE car
                SET price_per_km = ?
                WHERE id = ?
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, pricePerKm);
            statement.setLong(2, carId);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("차량 요금 변경 중 DB 오류가 발생했습니다.", e);
        }
    }

    //대여 시 사용
    public int updateStatusAndClearParkingLot(
            Connection connection,
            long carId,
            CarStatus status
    ) {
        String sql = """
            UPDATE car
            SET car_status = ?,
                current_parking_lot_id = NULL,
                current_address = NULL
            WHERE id = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setLong(2, carId);

            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("차량 대여 시작 상태 변경 중 오류가 발생했습니다.", e);
        }
    }

    //취소, 반납 시 사용
    public int updateStatusAndParkingLot(
            Connection connection,
            long carId,
            CarStatus status,
            long parkingLotId,
            String currentAddress
    ){
        String sql = """
            UPDATE car
            SET car_status =?,
                current_parking_lot_id =?,
                current_address = ?
            WHERE id = ?
        """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1,status.name());
            statement.setLong(2, parkingLotId);
            statement.setString(3, currentAddress);
            statement.setLong(4, carId);

            return statement.executeUpdate();
        }catch (SQLException e) {
            throw new RuntimeException("차량의 상태와 주차장 정보를 변경 중 DB 오류가 발생했습니다.", e);
        }
    }

    public int updateCurrentParkingLot(
            Connection connection,
            long carId,
            long parkingLotId,
            String currentAddress
    ) {
        String sql = """
            UPDATE car
            SET current_parking_lot_id = ?,
                current_address = ?
            WHERE id = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, parkingLotId);
            statement.setString(2, currentAddress);
            statement.setLong(3, carId);

            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("차량의 현재 주차장 변경 중 DB 오류가 발생했습니다.", e);
        }
    }

    //차량 목록에 현재 주차장 이름 표시
    public List<CarWithParkingLot> findAllWithParkingLot() {
        return findAllWithParkingLot(false);
    }

    public List<CarWithParkingLot> findAllVisibleWithParkingLot() {
        return findAllWithParkingLot(true);
    }

    private List<CarWithParkingLot> findAllWithParkingLot(boolean excludeInactive) {
        String sql = """
            SELECT c.id AS car_id,
                   c.name AS car_name,
                   c.max_passenger_count,
                   c.price_per_km,
                   c.current_parking_lot_id,
                   c.current_address,
                   c.car_status,
                   p.name AS parking_lot_name,
                   ci.id AS image_id,
                   ci.storage_key AS image_storage_key,
                   ci.original_file_name AS image_original_file_name,
                   ci.content_type AS image_content_type,
                   ci.size_bytes AS image_size_bytes,
                   ci.sort_order AS image_sort_order
            FROM car c
            LEFT JOIN parking_lot p
              ON c.current_parking_lot_id = p.parking_lot_id
            LEFT JOIN car_image ci
              ON ci.car_id = c.id
             AND ci.sort_order = 0
            """ + (excludeInactive ? "WHERE c.car_status <> ?\n" : "")
                + "ORDER BY c.id";

        List<CarWithParkingLot> cars = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            if (excludeInactive) {
                statement.setString(1, CarStatus.INACTIVE.name());
            }
            try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long currentParkingLotId =
                        resultSet.getLong("current_parking_lot_id");

                Long carCurrentParkingLotId = resultSet.wasNull()
                        ? null
                        : currentParkingLotId;

                Car car = new Car(
                        resultSet.getLong("car_id"),
                        resultSet.getString("car_name"),
                        resultSet.getInt("max_passenger_count"),
                        resultSet.getInt("price_per_km"),
                        carCurrentParkingLotId,
                        resultSet.getString("current_address"),
                        CarStatus.valueOf(
                                resultSet.getString("car_status")
                        )
                );

                CarWithParkingLot carWithParkingLotName =
                        new CarWithParkingLot(
                                car,
                                resultSet.getString("parking_lot_name"),
                                mapPrimaryImage(resultSet)
                        );

                cars.add(carWithParkingLotName);
            }

            return cars;
            }

        } catch (SQLException e) {
            throw new RuntimeException("주차장 이름을 포함한 차량 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    public List<CarWithParkingLot> findAvailableByParkingLotAndPeriod(
            long parkingLotId,
            LocalDateTime startTime,
            LocalDateTime returnTime,
            int passengerCount
    ) {
        String sql = """
            SELECT c.id AS car_id,
                   c.name AS car_name,
                   c.max_passenger_count,
                   c.price_per_km,
                   c.current_parking_lot_id,
                   c.current_address,
                   c.car_status,
                   p.name AS parking_lot_name,
                   ci.id AS image_id,
                   ci.storage_key AS image_storage_key,
                   ci.original_file_name AS image_original_file_name,
                   ci.content_type AS image_content_type,
                   ci.size_bytes AS image_size_bytes,
                   ci.sort_order AS image_sort_order
            FROM car c
            JOIN parking_lot p
              ON c.current_parking_lot_id = p.parking_lot_id
            LEFT JOIN car_image ci
              ON ci.car_id = c.id
             AND ci.sort_order = 0
            WHERE c.current_parking_lot_id = ?
              AND c.car_status = ?
              AND p.parking_lot_status = ?
              AND c.max_passenger_count >= ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM rental r
                  WHERE r.car_id = c.id
                    AND r.rental_status IN (?, ?)
                    AND r.start_time < ?
                    AND r.return_time > ?
              )
            ORDER BY c.id
            """;

        List<CarWithParkingLot> cars = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, parkingLotId);
            statement.setString(2, CarStatus.AVAILABLE.name());
            statement.setString(3, ParkingLotStatus.ACTIVE.name());
            statement.setInt(4, passengerCount);
            statement.setString(5, "RESERVED");
            statement.setString(6, "IN_PROGRESS");
            statement.setTimestamp(7, Timestamp.valueOf(returnTime));
            statement.setTimestamp(8, Timestamp.valueOf(startTime));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Car car = new Car(
                            resultSet.getLong("car_id"),
                            resultSet.getString("car_name"),
                            resultSet.getInt("max_passenger_count"),
                            resultSet.getInt("price_per_km"),
                            resultSet.getLong("current_parking_lot_id"),
                            resultSet.getString("current_address"),
                            CarStatus.valueOf(resultSet.getString("car_status"))
                    );
                    cars.add(new CarWithParkingLot(
                            car,
                            resultSet.getString("parking_lot_name"),
                            mapPrimaryImage(resultSet)
                    ));
                }
            }

            return cars;
        } catch (SQLException e) {
            throw new RuntimeException("대여 가능한 차량 조회 중 DB 오류가 발생했습니다.", e);
        }
    }

    public List<ParkingLotAvailability> findParkingLotAvailability(
            LocalDateTime startTime,
            LocalDateTime returnTime,
            int passengerCount
    ) {
        String sql = """
            SELECT p.parking_lot_id,
                   p.name,
                   p.max_parking_count,
                   p.address,
                   p.current_parking_count,
                   p.rented_car_count,
                   p.parking_lot_status,
                   COUNT(c.id) AS available_car_count,
                   GROUP_CONCAT(c.name ORDER BY c.id SEPARATOR '|') AS available_car_names
            FROM parking_lot p
            LEFT JOIN car c
              ON c.current_parking_lot_id = p.parking_lot_id
             AND c.car_status = ?
             AND c.max_passenger_count >= ?
             AND NOT EXISTS (
                 SELECT 1
                 FROM rental r
                 WHERE r.car_id = c.id
                   AND r.rental_status IN (?, ?)
                   AND r.start_time < ?
                   AND r.return_time > ?
             )
            WHERE p.parking_lot_status = ?
            GROUP BY p.parking_lot_id,
                     p.name,
                     p.max_parking_count,
                     p.address,
                     p.current_parking_count,
                     p.rented_car_count,
                     p.parking_lot_status
            ORDER BY p.parking_lot_id
            """;

        List<ParkingLotAvailability> parkingLots = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, CarStatus.AVAILABLE.name());
            statement.setInt(2, passengerCount);
            statement.setString(3, RentalStatus.RESERVED.name());
            statement.setString(4, RentalStatus.IN_PROGRESS.name());
            statement.setTimestamp(5, Timestamp.valueOf(returnTime));
            statement.setTimestamp(6, Timestamp.valueOf(startTime));
            statement.setString(7, ParkingLotStatus.ACTIVE.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ParkingLot parkingLot = new ParkingLot(
                            resultSet.getLong("parking_lot_id"),
                            resultSet.getString("name"),
                            resultSet.getInt("max_parking_count"),
                            resultSet.getString("address"),
                            resultSet.getInt("current_parking_count"),
                            resultSet.getInt("rented_car_count"),
                            ParkingLotStatus.valueOf(resultSet.getString("parking_lot_status"))
                    );
                    parkingLots.add(new ParkingLotAvailability(
                            parkingLot,
                            resultSet.getInt("available_car_count"),
                            toCarNames(resultSet.getString("available_car_names"))
                    ));
                }
            }

            return parkingLots;
        } catch (SQLException e) {
            throw new RuntimeException("조건별 주차장 차량 조회 중 DB 오류가 발생했습니다.", e);
        }
    }

    private CarImage mapPrimaryImage(ResultSet resultSet) throws SQLException {
        long imageId = resultSet.getLong("image_id");
        if (resultSet.wasNull()) {
            return null;
        }

        return new CarImage(
                imageId,
                resultSet.getLong("car_id"),
                resultSet.getString("image_storage_key"),
                resultSet.getString("image_original_file_name"),
                resultSet.getString("image_content_type"),
                resultSet.getLong("image_size_bytes"),
                resultSet.getInt("image_sort_order")
        );
    }

    private List<String> toCarNames(String names) {
        if (names == null || names.isBlank()) {
            return List.of();
        }
        return List.of(names.split("\\|"));
    }
}
