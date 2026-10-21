package org.example;

import org.example.dao.CarDao;
import org.example.dao.MemberDao;
import org.example.dao.ParkingLotDao;
import org.example.dao.RentalDao;
import org.example.domain.Car;
import org.example.domain.CarStatus;
import org.example.domain.Member;
import org.example.domain.ParkingLot;
import org.example.domain.Rental;
import org.example.domain.RentalStatus;
import org.example.dto.ReservationRequest;
import org.example.service.CarService;
import org.example.service.MemberService;
import org.example.service.RentalService;
import org.example.security.PasswordHasher;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * JDBC 학습용 수동 통합 테스트.
 *
 * 실행할 때마다 회원·주차장·차량·대여 데이터가 DB에 추가된다.
 * Main은 SQL을 직접 실행하지 않고 Service/DAO의 public API만 호출한다.
 */
public class Main {

    public static void main(String[] args) {
        MemberDao memberDao = new MemberDao();
        ParkingLotDao parkingLotDao = new ParkingLotDao();
        CarDao carDao = new CarDao();
        RentalDao rentalDao = new RentalDao();
        MemberService memberService = new MemberService(memberDao, new PasswordHasher());
        CarService carService = new CarService(carDao, parkingLotDao, rentalDao);
        RentalService rentalService = new RentalService(
                carDao, rentalDao, memberDao, parkingLotDao
        );

        // 매 실행마다 다른 이름을 써서 기존 데이터의 ID나 상태에 의존하지 않는다.
        String testRunId = "MAIN-TEST-" + System.currentTimeMillis();

        try {
            TestFixture fixture = createFixture(
                    testRunId, memberDao, memberService, parkingLotDao, carDao, carService
            );

            testInvalidTime(rentalService, fixture);
            testLicenseRequired(rentalService, memberService, fixture, testRunId);
            testPassengerLimit(rentalService, fixture);
            testUnavailableCarCannotBeReserved(rentalService, carDao, fixture);
            testCarRegistrationRollback(carService, carDao, parkingLotDao, testRunId);
            testReservationAndCancellation(
                    rentalService, carDao, rentalDao, fixture
            );
            testRentalStartAndReturn(
                    rentalService, carDao, rentalDao, parkingLotDao, fixture
            );

            System.out.println("\n모든 main 테스트를 통과했습니다.");
            System.out.println("생성한 테스트 데이터 접두사: " + testRunId);
        } catch (AssertionError | RuntimeException e) {
            System.err.println("\n테스트 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static TestFixture createFixture(
            String testRunId,
            MemberDao memberDao,
            MemberService memberService,
            ParkingLotDao parkingLotDao,
            CarDao carDao,
            CarService carService
    ) {
        String loginId = "member-" + System.currentTimeMillis();
        int memberId = Math.toIntExact(memberService.registerMember(
                testRunId + "-회원", true, loginId, "password1234"
        ));
        int pickupParkingLotId = parkingLotDao.save(
                testRunId + "-출발 주차장", 10, "서울시 강남구 출발로 1"
        );
        int carId = Math.toIntExact(carService.registerCar(
                testRunId + "-차량", 5, 200, pickupParkingLotId
        ));

        require(
                findParkingLot(parkingLotDao, pickupParkingLotId).getCurrentParkingCount() == 1,
                "차량 등록 후 출발 주차장의 현재 주차 대수는 1이어야 합니다."
        );

        require(memberDao.findById(memberId).isPresent(), "등록한 회원을 조회할 수 있어야 합니다.");
        require(memberService.login(loginId, "password1234").getId().equals((long) memberId),
                "올바른 로그인 ID와 비밀번호로 로그인할 수 있어야 합니다.");
        expectException(
                IllegalArgumentException.class,
                () -> memberService.login(loginId, "wrong-password"),
                "잘못된 비밀번호로 로그인할 수 없어야 합니다."
        );
        expectException(
                IllegalArgumentException.class,
                () -> memberService.registerMember(
                        "중복 ID 회원", true, loginId, "password1234"
                ),
                "이미 사용 중인 로그인 ID로 가입할 수 없어야 합니다."
        );
        require(carDao.findById(carId).isPresent(), "등록한 차량을 조회할 수 있어야 합니다.");

        System.out.println("[준비] 회원=" + memberId
                + ", 차량=" + carId
                + ", 픽업·반납 주차장=" + pickupParkingLotId);

        return new TestFixture(memberId, carId, pickupParkingLotId);
    }

    private static void testInvalidTime(RentalService rentalService, TestFixture fixture) {
        LocalDateTime start = LocalDateTime.now().plusDays(7).withSecond(0).withNano(0);
        ReservationRequest invalidRequest = new ReservationRequest(
                (long) fixture.memberId,
                (long) fixture.carId,
                start,
                start,
                2
        );

        expectException(
                IllegalArgumentException.class,
                () -> rentalService.reserve(invalidRequest),
                "반납 시간이 시작 시간과 같으면 예약할 수 없어야 합니다."
        );
        System.out.println("[통과] 잘못된 시간 범위 거절");
    }

    private static void testLicenseRequired(
            RentalService rentalService,
            MemberService memberService,
            TestFixture fixture,
            String testRunId
    ) {
        int unlicensedMemberId = Math.toIntExact(memberService.registerMember(
                testRunId + "-무면허 회원",
                false,
                "unlicensed-" + System.currentTimeMillis(),
                "password1234"
        ));
        LocalDateTime start = LocalDateTime.now().plusDays(7).withSecond(0).withNano(0);

        expectException(
                IllegalStateException.class,
                () -> rentalService.reserve(new ReservationRequest(
                        (long) unlicensedMemberId,
                        (long) fixture.carId,
                        start,
                        start.plusHours(1),
                        2
                )),
                "면허가 없는 회원은 예약할 수 없어야 합니다."
        );
        System.out.println("[통과] 면허 없는 회원의 예약 거절");
    }

    private static void testPassengerLimit(
            RentalService rentalService,
            TestFixture fixture
    ) {
        LocalDateTime start = LocalDateTime.now().plusDays(7).withSecond(0).withNano(0);

        expectException(
                IllegalArgumentException.class,
                () -> rentalService.reserve(new ReservationRequest(
                        (long) fixture.memberId,
                        (long) fixture.carId,
                        start,
                        start.plusHours(1),
                        6
                )),
                "차량 정원보다 많은 인원은 예약할 수 없어야 합니다."
        );
        System.out.println("[통과] 차량 정원 초과 예약 거절");
    }

    private static void testUnavailableCarCannotBeReserved(
            RentalService rentalService,
            CarDao carDao,
            TestFixture fixture
    ) {
        LocalDateTime start = LocalDateTime.now().plusDays(7).withSecond(0).withNano(0);

        require(carDao.updateStatus(fixture.carId, CarStatus.MAINTENANCE) == 1,
                "차량을 수리 상태로 변경할 수 있어야 합니다.");
        expectException(
                IllegalStateException.class,
                () -> rentalService.reserve(new ReservationRequest(
                        (long) fixture.memberId,
                        (long) fixture.carId,
                        start,
                        start.plusHours(1),
                        2
                )),
                "수리 중 차량은 예약할 수 없어야 합니다."
        );

        require(carDao.updateStatus(fixture.carId, CarStatus.INACTIVE) == 1,
                "차량을 비활성 상태로 변경할 수 있어야 합니다.");
        expectException(
                IllegalStateException.class,
                () -> rentalService.reserve(new ReservationRequest(
                        (long) fixture.memberId,
                        (long) fixture.carId,
                        start,
                        start.plusHours(1),
                        2
                )),
                "비활성 차량은 예약할 수 없어야 합니다."
        );

        require(carDao.updateStatus(fixture.carId, CarStatus.AVAILABLE) == 1,
                "테스트 후 차량을 사용 가능 상태로 복구할 수 있어야 합니다.");
        System.out.println("[통과] 수리·비활성 차량의 예약 거절");
    }

    private static void testCarRegistrationRollback(
            CarService carService,
            CarDao carDao,
            ParkingLotDao parkingLotDao,
            String testRunId
    ) {
        int parkingLotId = parkingLotDao.save(
                testRunId + "-차량 등록 테스트 주차장", 1, "서울시 강남구 등록로 4"
        );
        carService.registerCar(testRunId + "-첫 차량", 2, 200, parkingLotId);
        int carCountBefore = carDao.findAll().size();

        expectException(
                IllegalStateException.class,
                () -> carService.registerCar(testRunId + "-실패 차량", 2, 200, parkingLotId),
                "만차 주차장에는 두 번째 차량을 등록할 수 없어야 합니다."
        );

        require(carDao.findAll().size() == carCountBefore,
                "차량 등록 실패 시 차량 INSERT는 롤백되어야 합니다.");
        require(findParkingLot(parkingLotDao, parkingLotId).getCurrentParkingCount() == 1,
                "차량 등록 실패 시 주차장 현재 차량 수는 유지되어야 합니다.");
        System.out.println("[통과] 만차 차량 등록의 롤백 검증");
    }

    private static void testReservationAndCancellation(
            RentalService rentalService,
            CarDao carDao,
            RentalDao rentalDao,
            TestFixture fixture
    ) {
        LocalDateTime start = LocalDateTime.now().plusDays(7).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(3);

        ReservationRequest request = new ReservationRequest(
                (long) fixture.memberId,
                (long) fixture.carId,
                start,
                end,
                2
        );

        Rental reserved = rentalService.reserve(request);
        long rentalId = reserved.getId();

        require(reserved.getRentalStatus() == RentalStatus.RESERVED,
                "새 예약의 상태는 RESERVED여야 합니다.");
        require(rentalDao.findById(rentalId).orElseThrow().getRentalStatus() == RentalStatus.RESERVED,
                "DB의 대여 상태는 RESERVED여야 합니다.");
        require(carDao.findById(fixture.carId).orElseThrow().getCarStatus() == CarStatus.AVAILABLE,
                "미래 예약을 만들어도 차량 상태는 AVAILABLE이어야 합니다.");
        System.out.println("[통과] 미래 예약 후 rental/car 상태 검증");

        Rental laterReservation = rentalService.reserve(new ReservationRequest(
                (long) fixture.memberId,
                (long) fixture.carId,
                end.plusMinutes(10),
                end.plusHours(2),
                2
        ));
        require(laterReservation.getRentalStatus() == RentalStatus.RESERVED,
                "겹치지 않는 미래 예약은 추가로 생성할 수 있어야 합니다.");
        System.out.println("[통과] 겹치지 않는 미래 예약 허용");

        expectException(
                IllegalStateException.class,
                () -> rentalService.reserve(request),
                "예약된 차량에는 두 번째 예약을 만들 수 없어야 합니다."
        );
        System.out.println("[통과] 이미 예약된 차량의 중복 예약 거절");

        Rental canceled = rentalService.cancelReservation(rentalId, fixture.memberId);
        require(canceled.getRentalStatus() == RentalStatus.CANCELED,
                "취소 결과의 상태는 CANCELED여야 합니다.");
        require(rentalDao.findById(rentalId).orElseThrow().getRentalStatus() == RentalStatus.CANCELED,
                "DB의 대여 상태는 CANCELED여야 합니다.");

        Car canceledCar = carDao.findById(fixture.carId).orElseThrow();
        require(canceledCar.getCarStatus() == CarStatus.AVAILABLE,
                "취소 후 차량 상태는 AVAILABLE이어야 합니다.");
        require(canceledCar.getCurrentParkingLotId().equals((long) fixture.pickupParkingLotId),
                "취소 후 차량은 출발 주차장으로 돌아가야 합니다.");
        System.out.println("[통과] 취소 후 rental/car 상태 검증");

        rentalService.cancelReservation(laterReservation.getId(), fixture.memberId);

        expectException(
                IllegalStateException.class,
                () -> rentalService.cancelReservation(rentalId, fixture.memberId),
                "CANCELED 상태의 예약은 다시 취소할 수 없어야 합니다."
        );
        System.out.println("[통과] 취소된 예약의 재취소 거절");
    }

    private static void testRentalStartAndReturn(
            RentalService rentalService,
            CarDao carDao,
            RentalDao rentalDao,
            ParkingLotDao parkingLotDao,
            TestFixture fixture
    ) {
        ParkingLot pickupParkingLot = findParkingLot(parkingLotDao, fixture.pickupParkingLotId);
        int pickupCountBefore = pickupParkingLot.getCurrentParkingCount();

        LocalDateTime start = LocalDateTime.now().plusSeconds(2).withNano(0);
        Rental reserved = rentalService.reserve(new ReservationRequest(
                (long) fixture.memberId,
                (long) fixture.carId,
                start,
                start.plusHours(2),
                2
        ));

        long rentalId = reserved.getId();

        expectException(
                IllegalStateException.class,
                () -> rentalService.startRental(rentalId, fixture.memberId),
                "예약 시작 시각 전에는 대여를 시작할 수 없어야 합니다."
        );

        waitUntil(start);
        Rental renting = rentalService.startRental(rentalId, fixture.memberId);

        require(renting.getRentalStatus() == RentalStatus.IN_PROGRESS,
                "대여 시작 결과는 IN_PROGRESS여야 합니다.");
        require(rentalDao.findById(rentalId).orElseThrow().getActualPickupTime() != null,
                "대여 시작 후 실제 픽업 시간이 저장되어야 합니다.");

        Car rentedCar = carDao.findById(fixture.carId).orElseThrow();
        require(rentedCar.getCarStatus() == CarStatus.RENTED,
                "대여 시작 후 차량 상태는 RENTED여야 합니다.");
        require(rentedCar.getCurrentParkingLotId() == null,
                "대여 시작 후 차량의 현재 주차장 ID는 null이어야 합니다.");
        require(rentedCar.getCurrentAddress() == null,
                "대여 시작 후 차량의 현재 주소는 null이어야 합니다.");
        require(findParkingLot(parkingLotDao, fixture.pickupParkingLotId).getCurrentParkingCount()
                        == pickupCountBefore - 1,
                "대여 시작 후 출발 주차장의 현재 주차 대수는 1 감소해야 합니다.");
        System.out.println("[통과] 대여 시작 후 rental/car/parking_lot 상태 검증");

        Rental returned = rentalService.returnCar(rentalId, fixture.memberId);
        require(returned.getRentalStatus() == RentalStatus.COMPLETED,
                "정상 반납 결과는 COMPLETED여야 합니다.");
        require(rentalDao.findById(rentalId).orElseThrow().getActualReturnTime() != null,
                "정상 반납 후 실제 반납 시간이 저장되어야 합니다.");

        Car returnedCar = carDao.findById(fixture.carId).orElseThrow();
        require(returnedCar.getCarStatus() == CarStatus.AVAILABLE,
                "정상 반납 후 차량 상태는 AVAILABLE이어야 합니다.");
        require(returnedCar.getCurrentParkingLotId().equals((long) fixture.pickupParkingLotId),
                "정상 반납 후 차량은 픽업 주차장에 있어야 합니다.");
        require(returnedCar.getCurrentAddress().equals(pickupParkingLot.getAddress()),
                "정상 반납 후 차량 주소는 픽업 주차장 주소여야 합니다.");
        require(findParkingLot(parkingLotDao, fixture.pickupParkingLotId).getCurrentParkingCount()
                        == pickupCountBefore,
                "정상 반납 후 픽업 주차장의 현재 대수는 원래 값으로 돌아와야 합니다.");
        System.out.println("[통과] 정상 반납 후 rental/car/parking_lot 상태 검증");
    }

    private static ParkingLot findParkingLot(ParkingLotDao parkingLotDao, long parkingLotId) {
        return parkingLotDao.findAll().stream()
                .filter(parkingLot -> parkingLot.getId().equals(parkingLotId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("주차장을 찾을 수 없습니다: " + parkingLotId));
    }

    private static void expectException(
            Class<? extends RuntimeException> expectedType,
            Runnable action,
            String message
    ) {
        try {
            action.run();
        } catch (RuntimeException e) {
            if (expectedType.isInstance(e)) {
                return;
            }
            throw new AssertionError(message + " 실제 예외: " + e.getClass().getSimpleName(), e);
        }
        throw new AssertionError(message + " 예외가 발생하지 않았습니다.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void waitUntil(LocalDateTime targetTime) {
        long waitMillis = Duration.between(LocalDateTime.now(), targetTime).toMillis() + 100;
        if (waitMillis <= 0) {
            return;
        }

        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("대여 시작 시각 대기 중 인터럽트가 발생했습니다.", e);
        }
    }

    private record TestFixture(
            int memberId,
            int carId,
            int pickupParkingLotId
    ) {
    }
}
