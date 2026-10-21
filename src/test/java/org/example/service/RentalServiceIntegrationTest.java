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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void startAndReturnMoveRentalCarAndParkingLotThroughExpectedStates() throws SQLException {
        long memberId = insertMember(true);
        long parkingLotId = insertParkingLot();
        long carId = insertCar(parkingLotId);
        long rentalId = insertReservedRental(
                memberId,
                carId,
                parkingLotId,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1)
        );

        Rental startedRental = rentalService.startRental(rentalId, memberId);

        assertEquals(RentalStatus.IN_PROGRESS, startedRental.getRentalStatus());
        assertEquals(new CarState("RENTED", null), findCarState(carId));
        assertEquals(new ParkingLotState(0, 1), findParkingLotState(parkingLotId));

        Rental returnedRental = rentalService.returnCar(rentalId, memberId);

        assertEquals(RentalStatus.COMPLETED, returnedRental.getRentalStatus());
        assertEquals(new RentalState("COMPLETED", true, true), findRentalState(rentalId));
        assertEquals(new CarState("AVAILABLE", parkingLotId), findCarState(carId));
        assertEquals(new ParkingLotState(1, 0), findParkingLotState(parkingLotId));
    }

    @Test
    void startRentalMarksReservationAsNoShowAfterTenMinuteGracePeriod() throws SQLException {
        long memberId = insertMember(true);
        long parkingLotId = insertParkingLot();
        long carId = insertCar(parkingLotId);
        long rentalId = insertReservedRental(
                memberId,
                carId,
                parkingLotId,
                LocalDateTime.now().minusMinutes(11),
                LocalDateTime.now().plusHours(1)
        );

        assertThrows(IllegalStateException.class, () -> rentalService.startRental(rentalId, memberId));

        assertEquals(new RentalState("NO_SHOW", false, false), findRentalState(rentalId));
        assertEquals(new CarState("AVAILABLE", parkingLotId), findCarState(carId));
        assertEquals(new ParkingLotState(1, 0), findParkingLotState(parkingLotId));
    }

    @Test
    void concurrentReservationsForSameCarAllowExactlyOneRequest() throws Exception {
        long firstMemberId = insertMember(true);
        long secondMemberId = insertMember(true);
        long parkingLotId = insertParkingLot();
        long carId = insertCar(parkingLotId);
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withNano(0);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> firstRequest = executor.submit(
                    () -> tryReserveAtSameTime(firstMemberId, carId, startTime, ready, start)
            );
            Future<Boolean> secondRequest = executor.submit(
                    () -> tryReserveAtSameTime(secondMemberId, carId, startTime, ready, start)
            );

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int successCount = (firstRequest.get() ? 1 : 0) + (secondRequest.get() ? 1 : 0);
            assertEquals(1, successCount);
            assertEquals(1, countRentals());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void startRentalRejectsReservationOwnedByAnotherMember() throws SQLException {
        long ownerId = insertMember(true);
        long otherMemberId = insertMember(true);
        long parkingLotId = insertParkingLot();
        long carId = insertCar(parkingLotId);
        long rentalId = insertReservedRental(
                ownerId,
                carId,
                parkingLotId,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1)
        );

        assertThrows(IllegalStateException.class, () -> rentalService.startRental(rentalId, otherMemberId));

        assertEquals(new RentalState("RESERVED", false, false), findRentalState(rentalId));
        assertEquals(new CarState("AVAILABLE", parkingLotId), findCarState(carId));
        assertEquals(new ParkingLotState(1, 0), findParkingLotState(parkingLotId));
    }

    @Test
    void returnCarRejectsRentalOwnedByAnotherMember() throws SQLException {
        long ownerId = insertMember(true);
        long otherMemberId = insertMember(true);
        long parkingLotId = insertParkingLot();
        long carId = insertCar(parkingLotId);
        long rentalId = insertReservedRental(
                ownerId,
                carId,
                parkingLotId,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1)
        );
        rentalService.startRental(rentalId, ownerId);

        assertThrows(IllegalStateException.class, () -> rentalService.returnCar(rentalId, otherMemberId));

        assertEquals(new RentalState("IN_PROGRESS", true, false), findRentalState(rentalId));
        assertEquals(new CarState("RENTED", null), findCarState(carId));
        assertEquals(new ParkingLotState(0, 1), findParkingLotState(parkingLotId));
    }

    @Test
    void cancelReservationRejectsReservationOwnedByAnotherMember() throws SQLException {
        long ownerId = insertMember(true);
        long otherMemberId = insertMember(true);
        long parkingLotId = insertParkingLot();
        long carId = insertCar(parkingLotId);
        long rentalId = insertReservedRental(
                ownerId,
                carId,
                parkingLotId,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1)
        );

        assertThrows(IllegalStateException.class, () -> rentalService.cancelReservation(rentalId, otherMemberId));

        assertEquals(new RentalState("RESERVED", false, false), findRentalState(rentalId));
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

    private boolean tryReserveAtSameTime(
            long memberId,
            long carId,
            LocalDateTime startTime,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 예약 테스트 시작 신호를 받지 못했습니다.");
        }

        try {
            newRentalService().reserve(
                    new ReservationRequest(memberId, carId, startTime, startTime.plusHours(2), 2)
            );
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private RentalService newRentalService() {
        return new RentalService(
                new CarDao(),
                new RentalDao(),
                new MemberDao(),
                new ParkingLotDao()
        );
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

    private long insertReservedRental(
            long memberId,
            long carId,
            long parkingLotId,
            LocalDateTime startTime,
            LocalDateTime returnTime
    ) throws SQLException {
        String sql = """
                INSERT INTO rental (
                    start_time, return_time, passengers_count, rental_status,
                    car_id, pickup_parking_lot_id, member_id
                )
                VALUES (?, ?, ?, 'RESERVED', ?, ?, ?)
                """;
        return insertAndGetId(sql, startTime, returnTime, 2, carId, parkingLotId, memberId);
    }

    private long countRentals() throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM rental")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private CarState findCarState(long carId) throws SQLException {
        String sql = "SELECT car_status, current_parking_lot_id FROM car WHERE id = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, carId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new CarState(
                        resultSet.getString("car_status"),
                        resultSet.getObject("current_parking_lot_id", Long.class)
                );
            }
        }
    }

    private ParkingLotState findParkingLotState(long parkingLotId) throws SQLException {
        String sql = """
                SELECT current_parking_count, rented_car_count
                FROM parking_lot
                WHERE parking_lot_id = ?
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, parkingLotId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new ParkingLotState(
                        resultSet.getInt("current_parking_count"),
                        resultSet.getInt("rented_car_count")
                );
            }
        }
    }

    private RentalState findRentalState(long rentalId) throws SQLException {
        String sql = """
                SELECT rental_status, actual_pickup_time, actual_return_time
                FROM rental
                WHERE id = ?
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, rentalId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new RentalState(
                        resultSet.getString("rental_status"),
                        resultSet.getTimestamp("actual_pickup_time") != null,
                        resultSet.getTimestamp("actual_return_time") != null
                );
            }
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

    private record CarState(String status, Long parkingLotId) {
    }

    private record ParkingLotState(int currentCarCount, int rentedCarCount) {
    }

    private record RentalState(String status, boolean hasActualPickupTime, boolean hasActualReturnTime) {
    }
}
