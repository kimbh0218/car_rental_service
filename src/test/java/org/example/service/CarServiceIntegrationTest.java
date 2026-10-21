package org.example.service;

import org.example.dao.CarDao;
import org.example.dao.ParkingLotDao;
import org.example.dao.RentalDao;
import org.example.domain.Car;
import org.example.domain.CarStatus;
import org.example.domain.ParkingLot;
import org.example.domain.ParkingLotStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CarServiceIntegrationTest {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("car_rental_jdbc")
            .withUsername("test_user")
            .withPassword("test_password")
            .withInitScript("000_initial_schema.sql");

    private CarService carService;
    private ParkingLotService parkingLotService;

    @BeforeAll
    static void startDatabase() {
        MYSQL.start();
        System.setProperty("DB_URL", MYSQL.getJdbcUrl());
        System.setProperty("DB_USERNAME", MYSQL.getUsername());
        System.setProperty("DB_PASSWORD", MYSQL.getPassword());
    }

    @AfterAll
    static void stopDatabase() {
        System.clearProperty("DB_URL");
        System.clearProperty("DB_USERNAME");
        System.clearProperty("DB_PASSWORD");
        MYSQL.stop();
    }

    @BeforeEach
    void setUp() throws SQLException {
        carService = new CarService(new CarDao(), new ParkingLotDao(), new RentalDao());
        parkingLotService = new ParkingLotService(new ParkingLotDao());
        clearDatabase();
    }

    @Test
    void registerCarCreatesAvailableCarAndIncreasesParkingCount() throws SQLException {
        long parkingLotId = insertParkingLot("강남 주차장", 2, 0, 0, ParkingLotStatus.ACTIVE, "서울시 강남구 1");

        long carId = carService.registerCar("  테스트 차량  ", 4, 1500, parkingLotId);

        Car car = carService.findCarById(carId);
        assertEquals("테스트 차량", car.getName());
        assertEquals(4, car.getMaxPassengerCount());
        assertEquals(1500, car.getPricePerKm());
        assertEquals(CarStatus.AVAILABLE, car.getCarStatus());
        assertEquals(parkingLotId, car.getCurrentParkingLotId());
        assertEquals("서울시 강남구 1", car.getCurrentAddress());
        assertEquals(1, parkingLotService.findParkingLotById(parkingLotId).getCurrentParkingCount());
    }

    @Test
    void registerCarRejectsFullParkingLotWithoutSavingCar() throws SQLException {
        long parkingLotId = insertParkingLot("가득 찬 주차장", 1, 1, 0, ParkingLotStatus.ACTIVE, "서울시 강남구 1");

        assertThrows(
                IllegalStateException.class,
                () -> carService.registerCar("테스트 차량", 4, 1500, parkingLotId)
        );

        assertEquals(0, countCars());
        assertEquals(1, parkingLotService.findParkingLotById(parkingLotId).getCurrentParkingCount());
    }

    @Test
    void registerCarRejectsInactiveParkingLotWithoutSavingCar() throws SQLException {
        long parkingLotId = insertParkingLot("비활성 주차장", 2, 0, 0, ParkingLotStatus.INACTIVE, "서울시 강남구 1");

        assertThrows(
                IllegalStateException.class,
                () -> carService.registerCar("테스트 차량", 4, 1500, parkingLotId)
        );

        assertEquals(0, countCars());
        assertEquals(0, parkingLotService.findParkingLotById(parkingLotId).getCurrentParkingCount());
    }

    @Test
    void changePriceUpdatesPositivePriceAndRejectsZeroOrNegativePrice() throws SQLException {
        long parkingLotId = insertParkingLot("강남 주차장", 2, 1, 0, ParkingLotStatus.ACTIVE, "서울시 강남구 1");
        long carId = insertCar("테스트 차량", parkingLotId, CarStatus.AVAILABLE, "서울시 강남구 1");

        carService.changePricePerKm(carId, 2200);

        assertEquals(2200, carService.findCarById(carId).getPricePerKm());
        assertThrows(IllegalArgumentException.class, () -> carService.changePricePerKm(carId, 0));
        assertEquals(2200, carService.findCarById(carId).getPricePerKm());
    }

    @Test
    void changeStatusAllowsManagementTransitionsAndRejectsManualRentedStatus() throws SQLException {
        long parkingLotId = insertParkingLot("강남 주차장", 2, 1, 0, ParkingLotStatus.ACTIVE, "서울시 강남구 1");
        long carId = insertCar("테스트 차량", parkingLotId, CarStatus.AVAILABLE, "서울시 강남구 1");

        carService.changeStatus(carId, CarStatus.MAINTENANCE);
        carService.changeStatus(carId, CarStatus.INACTIVE);

        assertEquals(CarStatus.INACTIVE, carService.findCarById(carId).getCarStatus());
        assertThrows(IllegalStateException.class, () -> carService.changeStatus(carId, CarStatus.RENTED));
        assertEquals(CarStatus.INACTIVE, carService.findCarById(carId).getCarStatus());
    }

    @Test
    void moveCarUpdatesCarLocationAndBothParkingCounts() throws SQLException {
        long currentParkingLotId = insertParkingLot("강남 주차장", 2, 1, 0, ParkingLotStatus.ACTIVE, "서울시 강남구 1");
        long newParkingLotId = insertParkingLot("서초 주차장", 2, 0, 0, ParkingLotStatus.ACTIVE, "서울시 서초구 2");
        long carId = insertCar("테스트 차량", currentParkingLotId, CarStatus.AVAILABLE, "서울시 강남구 1");

        carService.moveCar(carId, newParkingLotId);

        Car car = carService.findCarById(carId);
        assertEquals(newParkingLotId, car.getCurrentParkingLotId());
        assertEquals("서울시 서초구 2", car.getCurrentAddress());
        assertEquals(0, parkingLotService.findParkingLotById(currentParkingLotId).getCurrentParkingCount());
        assertEquals(1, parkingLotService.findParkingLotById(newParkingLotId).getCurrentParkingCount());
    }

    @Test
    void moveCarRejectsInactiveDestinationWithoutChangingCounts() throws SQLException {
        long currentParkingLotId = insertParkingLot("강남 주차장", 2, 1, 0, ParkingLotStatus.ACTIVE, "서울시 강남구 1");
        long newParkingLotId = insertParkingLot("비활성 주차장", 2, 0, 0, ParkingLotStatus.INACTIVE, "서울시 서초구 2");
        long carId = insertCar("테스트 차량", currentParkingLotId, CarStatus.AVAILABLE, "서울시 강남구 1");

        assertThrows(IllegalStateException.class, () -> carService.moveCar(carId, newParkingLotId));

        assertEquals(currentParkingLotId, carService.findCarById(carId).getCurrentParkingLotId());
        assertEquals(1, parkingLotService.findParkingLotById(currentParkingLotId).getCurrentParkingCount());
        assertEquals(0, parkingLotService.findParkingLotById(newParkingLotId).getCurrentParkingCount());
    }

    @Test
    void moveCarRejectsCarWithActiveReservationWithoutChangingCounts() throws SQLException {
        long memberId = insertMember();
        long currentParkingLotId = insertParkingLot("강남 주차장", 2, 1, 0, ParkingLotStatus.ACTIVE, "서울시 강남구 1");
        long newParkingLotId = insertParkingLot("서초 주차장", 2, 0, 0, ParkingLotStatus.ACTIVE, "서울시 서초구 2");
        long carId = insertCar("테스트 차량", currentParkingLotId, CarStatus.AVAILABLE, "서울시 강남구 1");
        insertReservedRental(memberId, carId, currentParkingLotId);

        assertThrows(IllegalStateException.class, () -> carService.moveCar(carId, newParkingLotId));

        assertEquals(currentParkingLotId, carService.findCarById(carId).getCurrentParkingLotId());
        assertEquals(1, parkingLotService.findParkingLotById(currentParkingLotId).getCurrentParkingCount());
        assertEquals(0, parkingLotService.findParkingLotById(newParkingLotId).getCurrentParkingCount());
    }

    private void clearDatabase() throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            statement.executeUpdate("DELETE FROM rental");
            statement.executeUpdate("DELETE FROM car");
            statement.executeUpdate("DELETE FROM member");
            statement.executeUpdate("DELETE FROM parking_lot");
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private long insertParkingLot(
            String name,
            int maxParkingCount,
            int currentParkingCount,
            int rentedCarCount,
            ParkingLotStatus status,
            String address
    ) throws SQLException {
        String sql = """
                INSERT INTO parking_lot (
                    name, max_parking_count, current_parking_count, rented_car_count, address, parking_lot_status
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        return insertAndGetId(sql, name, maxParkingCount, currentParkingCount, rentedCarCount, address, status.name());
    }

    private long insertCar(String name, long parkingLotId, CarStatus status, String address) throws SQLException {
        String sql = """
                INSERT INTO car (
                    name, max_passenger_count, price_per_km, current_parking_lot_id, current_address, car_status
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        return insertAndGetId(sql, name, 4, 1500, parkingLotId, address, status.name());
    }

    private long insertMember() throws SQLException {
        String sql = """
                INSERT INTO member (name, license_status, login_id, password_hash)
                VALUES (?, ?, ?, ?)
                """;
        return insertAndGetId(sql, "테스트 회원", true, "test-member-" + System.nanoTime(), "unused-password-hash");
    }

    private void insertReservedRental(long memberId, long carId, long parkingLotId) throws SQLException {
        String sql = """
                INSERT INTO rental (
                    start_time, return_time, passengers_count, rental_status,
                    car_id, pickup_parking_lot_id, member_id
                )
                VALUES (?, ?, ?, 'RESERVED', ?, ?, ?)
                """;
        insertAndGetId(
                sql,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                2,
                carId,
                parkingLotId,
                memberId
        );
    }

    private long countCars() throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM car")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private long insertAndGetId(String sql, Object... values) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                generatedKeys.next();
                return generatedKeys.getLong(1);
            }
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }
}
