package org.example.service;

import org.example.dao.CarDao;
import org.example.dao.MemberDao;
import org.example.dao.ParkingLotDao;
import org.example.dao.RentalDao;
import org.example.domain.Rental;
import org.example.domain.RentalStatus;
import org.example.dto.ReservationRequest;
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

class RentalServiceIntegrationTest {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("car_rental_jdbc")
            .withUsername("test_user")
            .withPassword("test_password")
            .withInitScript("000_initial_schema.sql");

    private RentalService rentalService;

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
        rentalService = new RentalService(
                new CarDao(),
                new RentalDao(),
                new MemberDao(),
                new ParkingLotDao()
        );
        clearDatabase();
    }

    @Test
    void reserveCreatesReservedRentalForLicensedMemberAndAvailableCar() throws SQLException {
        long memberId = insertMember(true);
        long parkingLotId = insertParkingLot();
        long carId = insertCar(parkingLotId);
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime returnTime = startTime.plusHours(2);

        Rental rental = rentalService.reserve(
                new ReservationRequest(memberId, carId, startTime, returnTime, 2)
        );

        assertEquals(RentalStatus.RESERVED, rental.getRentalStatus());
        assertEquals(memberId, rental.getMemberId());
        assertEquals(carId, rental.getCarId());
        assertEquals(1, countRentals());
    }

    @Test
    void reserveRejectsOverlappingActiveRentalForSameCar() throws SQLException {
        long memberId = insertMember(true);
        long parkingLotId = insertParkingLot();
        long carId = insertCar(parkingLotId);
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withNano(0);

        rentalService.reserve(new ReservationRequest(memberId, carId, startTime, startTime.plusHours(2), 2));

        assertThrows(
                IllegalStateException.class,
                () -> rentalService.reserve(
                        new ReservationRequest(memberId, carId, startTime.plusHours(1), startTime.plusHours(3), 2)
                )
        );
        assertEquals(1, countRentals());
    }

    @Test
    void reserveRejectsMemberWithoutLicenseAndDoesNotCreateRental() throws SQLException {
        long memberId = insertMember(false);
        long parkingLotId = insertParkingLot();
        long carId = insertCar(parkingLotId);
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withNano(0);

        assertThrows(
                IllegalStateException.class,
                () -> rentalService.reserve(
                        new ReservationRequest(memberId, carId, startTime, startTime.plusHours(2), 2)
                )
        );
        assertEquals(0, countRentals());
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

    private long insertMember(boolean hasLicense) throws SQLException {
        String sql = """
                INSERT INTO member (name, license_status, login_id, password_hash)
                VALUES (?, ?, ?, ?)
                """;
        return insertAndGetId(sql, "테스트 회원", hasLicense, "test-member-" + System.nanoTime(), "unused-password-hash");
    }

    private long insertParkingLot() throws SQLException {
        String sql = """
                INSERT INTO parking_lot (name, max_parking_count, current_parking_count, address)
                VALUES (?, ?, ?, ?)
                """;
        return insertAndGetId(sql, "테스트 주차장", 10, 1, "서울 테스트로 1");
    }

    private long insertCar(long parkingLotId) throws SQLException {
        String sql = """
                INSERT INTO car (name, max_passenger_count, price_per_km, current_parking_lot_id, current_address)
                VALUES (?, ?, ?, ?, ?)
                """;
        return insertAndGetId(sql, "테스트 차량", 4, 1000, parkingLotId, "서울 테스트로 1");
    }

    private long countRentals() throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM rental")) {
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
