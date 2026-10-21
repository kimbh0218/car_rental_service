package org.example.service;

import org.example.dao.ParkingLotDao;
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
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParkingLotServiceIntegrationTest {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("car_rental_jdbc")
            .withUsername("test_user")
            .withPassword("test_password")
            .withInitScript("000_initial_schema.sql");

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
        parkingLotService = new ParkingLotService(new ParkingLotDao());
        clearDatabase();
    }

    @Test
    void registerParkingLotNormalizesInputAndCreatesActiveEmptyLot() {
        long parkingLotId = parkingLotService.registerParkingLot(
                "  강남 주차장  ",
                10,
                "  서울시 강남구 테스트로 1  "
        );

        ParkingLot parkingLot = parkingLotService.findParkingLotById(parkingLotId);
        assertEquals("강남 주차장", parkingLot.getName());
        assertEquals("서울시 강남구 테스트로 1", parkingLot.getAddress());
        assertEquals(10, parkingLot.getMaxParkingCount());
        assertEquals(0, parkingLot.getCurrentParkingCount());
        assertEquals(0, parkingLot.getRentedCarCount());
        assertEquals(ParkingLotStatus.ACTIVE, parkingLot.getStatus());
    }

    @Test
    void updateParkingLotChangesInformationWhenCapacityCoversOccupiedCars() throws SQLException {
        long parkingLotId = insertParkingLot("기존 주차장", 5, 2, 1, ParkingLotStatus.ACTIVE);

        parkingLotService.updateParkingLot(
                parkingLotId,
                "  수정 주차장  ",
                "  서울시 서초구 테스트로 2  ",
                4
        );

        ParkingLot parkingLot = parkingLotService.findParkingLotById(parkingLotId);
        assertEquals("수정 주차장", parkingLot.getName());
        assertEquals("서울시 서초구 테스트로 2", parkingLot.getAddress());
        assertEquals(4, parkingLot.getMaxParkingCount());
        assertEquals(2, parkingLot.getCurrentParkingCount());
        assertEquals(1, parkingLot.getRentedCarCount());
    }

    @Test
    void updateParkingLotRejectsCapacityBelowCurrentAndRentedCars() throws SQLException {
        long parkingLotId = insertParkingLot("기존 주차장", 5, 2, 1, ParkingLotStatus.ACTIVE);

        assertThrows(
                IllegalArgumentException.class,
                () -> parkingLotService.updateParkingLot(parkingLotId, "수정 주차장", "서울시", 2)
        );

        assertEquals(5, parkingLotService.findParkingLotById(parkingLotId).getMaxParkingCount());
    }

    @Test
    void changeParkingLotStatusRejectsInactivationWhenCarsRemain() throws SQLException {
        long parkingLotId = insertParkingLot("운영 주차장", 5, 1, 0, ParkingLotStatus.ACTIVE);

        assertThrows(
                IllegalStateException.class,
                () -> parkingLotService.changeParkingLotStatus(parkingLotId, ParkingLotStatus.INACTIVE)
        );

        assertEquals(ParkingLotStatus.ACTIVE, parkingLotService.findParkingLotById(parkingLotId).getStatus());
    }

    @Test
    void changeParkingLotStatusInactivatesEmptyLot() throws SQLException {
        long parkingLotId = insertParkingLot("빈 주차장", 5, 0, 0, ParkingLotStatus.ACTIVE);

        parkingLotService.changeParkingLotStatus(parkingLotId, ParkingLotStatus.INACTIVE);

        assertEquals(ParkingLotStatus.INACTIVE, parkingLotService.findParkingLotById(parkingLotId).getStatus());
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
            ParkingLotStatus status
    ) throws SQLException {
        String sql = """
                INSERT INTO parking_lot (
                    name, max_parking_count, current_parking_count, rented_car_count, address, parking_lot_status
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setInt(2, maxParkingCount);
            statement.setInt(3, currentParkingCount);
            statement.setInt(4, rentedCarCount);
            statement.setString(5, "서울시 테스트로 1");
            statement.setString(6, status.name());
            statement.executeUpdate();
            try (var generatedKeys = statement.getGeneratedKeys()) {
                generatedKeys.next();
                return generatedKeys.getLong(1);
            }
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }
}
