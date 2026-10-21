# 프로젝트 파일 안내

## 1. 먼저 보는 관계도

```text
브라우저
  └─ JSP 화면 / 정적 CSS·JS
       └─ Servlet(요청·응답·화면 전환)
            └─ Service(업무 규칙·트랜잭션)
                 └─ DAO(SQL 실행)
                      └─ MySQL

공통 처리: AuthenticationStateFilter, DynamicPageCacheControlFilter
권한 처리: MemberAuthenticationFilter, AdminAuthenticationFilter, SessionAuthentication
이미지 처리: CarImageValidator → CarImageStorage → CarImageService/CarImageDao
자동 처리: NoShowScheduler → RentalService → RentalDao
```

* `web`은 HTTP 요청을 받고 JSP에 값을 전달하거나 Service를 호출한다.
* `service`는 입력 검증, 상태 전이, 여러 DAO 변경을 하나로 묶는 트랜잭션을 맡는다.
* `dao`는 MySQL 테이블을 조회·저장·수정하고, `domain`과 `dto`는 계층 사이의 데이터를 담는다.
* 고객 화면은 주차장/차량 검색 → 예약 → 대여 시작 → 반납으로, 운영자 화면은 차량·주차장·이미지 관리로 이어진다.

## 2. 빌드·설정·SQL

| 파일 | 한 줄 역할 |
| --- | --- |
| `.gitignore` | 빌드 산출물, IDE 설정, OS 메타데이터를 Git 추적에서 제외한다. |
| `.DS_Store` | macOS Finder가 만든 폴더 표시 설정 파일이며 애플리케이션과 무관하다. |
| `build.gradle` | Java WAR 프로젝트, Servlet/JSTL/MySQL/JUnit 의존성과 테스트·WAR 빌드 방식을 설정한다. |
| `settings.gradle` | Gradle 프로젝트 이름을 `car_rental`로 설정한다. |
| `gradlew` | macOS·Linux에서 지정된 Gradle 버전으로 빌드하는 실행 스크립트다. |
| `gradlew.bat` | Windows에서 지정된 Gradle 버전으로 빌드하는 실행 스크립트다. |
| `gradle/wrapper/gradle-wrapper.jar` | Gradle Wrapper를 실행하는 바이너리 라이브러리다. |
| `gradle/wrapper/gradle-wrapper.properties` | Wrapper가 내려받을 Gradle 9.6.0 배포본 위치를 지정한다. |
| `src/main/resources/db.properties` | JDBC 연결에 필요한 DB 주소와 계정 설정을 보관한다. |
| `sql/001_create_car_image.sql` | 차량 대표 이미지 메타데이터를 저장하는 `car_image` 테이블을 만든다. |

## 3. 실행·설정·보안

| 파일 | 한 줄 역할 |
| --- | --- |
| `src/main/java/org/example/Main.java` | 서비스 계층을 직접 호출해 회원·차량·예약·취소·대여·반납 규칙을 검증하는 수동 통합 테스트다. |
| `src/main/java/org/example/config/DBConnection.java` | `db.properties`를 읽어 MySQL JDBC 연결을 만들고 드라이버를 로드한다. |
| `src/main/java/org/example/security/PasswordHasher.java` | PBKDF2-HMAC-SHA256, 600,000회 반복으로 비밀번호를 해시·검증·재해시 판단한다. |

## 4. 도메인 모델과 DTO

| 파일 | 한 줄 역할 |
| --- | --- |
| `domain/Admin.java` | 운영자의 ID, 이름, 로그인 ID를 담는 도메인 객체다. |
| `domain/AdminAuth.java` | 운영자 정보와 비밀번호 해시를 함께 전달하는 로그인 조회 객체다. |
| `domain/Member.java` | 회원의 ID, 이름, 면허 보유 여부, 로그인 ID를 담는 도메인 객체다. |
| `domain/MemberAuth.java` | 회원 정보와 비밀번호 해시를 함께 전달하는 로그인 조회 객체다. |
| `domain/Car.java` | 차량명, 정원, km당 요금, 현재 주차 위치, 주소, 상태를 담는다. |
| `domain/CarImage.java` | DB에 기록된 차량 이미지 ID, 저장 키, 파일 정보, 대표 이미지 순서를 담는다. |
| `domain/CarWithParkingLot.java` | 차량과 현재 주차장 이름, 대표 이미지를 화면용으로 묶는다. |
| `domain/ParkingLot.java` | 주차장 이름·주소·수용 대수·현재 주차/대여 대수·운영 상태를 담는다. |
| `domain/Rental.java` | 예약·대여의 시간, 인원, 실제 픽업/반납 시각, 상태, 차량·회원·출발 주차장을 담는다. |
| `domain/RentalWithMember.java` | 운영자 조회용으로 대여 정보에 회원명을 결합한다. |
| `domain/StoredCarImage.java` | 파일 저장소에 저장된 이미지의 키와 메타데이터를 전달한다. |
| `domain/ValidatedCarImage.java` | 업로드 검증을 통과한 이미지의 안전한 파일명·형식·크기를 전달한다. |
| `domain/CarStatus.java` | 차량 상태 `AVAILABLE`, `RENTED`, `MAINTENANCE`, `INACTIVE`를 정의한다. |
| `domain/ParkingLotStatus.java` | 주차장 상태 `ACTIVE`, `INACTIVE`를 정의한다. |
| `domain/RentalStatus.java` | 대여 상태 `RESERVED`, `IN_PROGRESS`, `COMPLETED`, `CANCELED`, `NO_SHOW`를 정의한다. |
| `dto/ReservationRequest.java` | 예약 생성에 필요한 회원·차량·시작/반납 시각·탑승 인원을 전달한다. |
| `dto/CarScheduleItem.java` | 차량 일정 API가 반환할 시작/반납 시각과 예약 상태를 담는다. |
| `dto/MyRentalItem.java` | 내 예약 화면에 필요한 차량·주차장·대여 상태 정보를 담는다. |
| `dto/ParkingLotAvailability.java` | 검색 조건에서 주차장별 이용 가능한 차량 수를 담는다. |

## 5. DAO, 데이터베이스 접근 계층

| 파일 | 한 줄 역할 |
| --- | --- |
| `dao/AdminRepository.java` | 운영자 로그인 조회와 비밀번호 해시 갱신에 필요한 추상 인터페이스다. |
| `dao/AdminDao.java` | `AdminRepository` 구현체로 운영자 인증 정보를 MySQL에서 조회·수정한다. |
| `dao/MemberDao.java` | 회원 생성, 조회, 로그인 인증, 면허 상태와 비밀번호 해시 수정을 수행한다. |
| `dao/CarDao.java` | 차량 CRUD, 상태·요금·주차 위치 변경과 조건별 예약 가능 차량 조회를 수행한다. |
| `dao/ParkingLotDao.java` | 주차장 CRUD, 상태 변경, 주차/대여 차량 수 증감과 행 잠금을 수행한다. |
| `dao/RentalDao.java` | 예약 저장·상태 전이·일정/중복 조회·노쇼 처리·내 예약/운영자 예약 목록 조회를 수행한다. |
| `dao/CarImageDao.java` | 차량 이미지 메타데이터를 저장·조회·잠금 조회·교체한다. |
| `dao/DuplicateLoginIdException.java` | DB의 중복 로그인 ID 오류를 업무용 예외로 구분한다. |
| `dao/DuplicateParkingLotException.java` | DB의 중복 주차장 오류를 업무용 예외로 구분한다. |

## 6. 서비스, 업무 규칙 계층

| 파일 | 한 줄 역할 |
| --- | --- |
| `service/AdminService.java` | 운영자 로그인과 해시 재갱신을 처리하며 `AdminRepository`에 의존한다. |
| `service/MemberService.java` | 회원가입·로그인·면허 등록을 처리하고 로그인 ID 중복과 비밀번호 규칙을 검증한다. |
| `service/ParkingLotService.java` | 주차장 등록·수정·활성화 상태 변경을 처리하고 수용 대수 규칙을 검증한다. |
| `service/CarService.java` | 예약 가능 차량 검색, 차량 등록·상태·요금·주차장 이동을 트랜잭션과 상태 규칙으로 처리한다. |
| `service/RentalService.java` | 예약·취소·대여 시작·반납·노쇼 처리의 핵심 상태 전이와 동시성 제어를 담당한다. |
| `service/CarImageService.java` | 차량 행과 대표 이미지 행을 잠근 뒤 대표 이미지 DB 정보를 안전하게 저장·교체한다. |

## 7. 이미지 저장소

| 파일 | 한 줄 역할 |
| --- | --- |
| `storage/CarImageStorage.java` | 이미지 파일의 저장·열기·삭제를 정의한 저장소 인터페이스다. |
| `storage/CarImageStorageFactory.java` | 환경 변수 `IMAGE_STORAGE_TYPE`에 따라 현재 지원하는 로컬 저장소를 생성한다. |
| `storage/LocalCarImageStorage.java` | 이미지를 로컬 `cars/` 폴더에 UUID 파일명으로 저장하고 경로 이탈을 막는다. |

## 8. 웹 공통·인증·스케줄러

| 파일 | 한 줄 역할 |
| --- | --- |
| `web/HealthServlet.java` | `/health` 요청에 서비스 상태 확인용 응답을 제공한다. |
| `web/home/HomeServlet.java` | 루트 요청에서 운영자는 대시보드로 보내고, 나머지는 홈 JSP를 보여 준다. |
| `web/filter/AuthenticationStateFilter.java` | 모든 요청에 현재 인증 상태(`guest`/`member`/`admin`)를 JSP용 속성으로 넣는다. |
| `web/filter/DynamicPageCacheControlFilter.java` | 민감한 인증·회원·관리자 페이지의 브라우저 캐시를 막는다. |
| `web/auth/LoginType.java` | 세션에 저장할 로그인 역할 `MEMBER`와 `ADMIN`을 정의한다. |
| `web/auth/SessionAuthentication.java` | 로그인 시 세션을 교체하고 역할·사용자 ID·면허 상태를 저장하거나 로그아웃한다. |
| `web/auth/MemberAuthenticationFilter.java` | `/member/*` 요청을 회원 로그인 여부로 보호하고 미로그인 시 접근 거부 화면으로 보낸다. |
| `web/auth/AdminAuthenticationFilter.java` | `/admin/*` 요청을 운영자 로그인 여부로 보호하고 역할 불일치를 차단한다. |
| `web/auth/AccessDeniedPage.java` | 401/403 접근 거부 정보와 로그인 복귀 URL을 JSP에 전달한다. |
| `web/auth/AuthenticationStatusServlet.java` | 프런트엔드 JS가 세션 만료를 확인하도록 현재 인증 상태를 JSON으로 반환한다. |
| `web/auth/MemberSignupServlet.java` | 회원가입 폼을 보여 주고 비밀번호 확인 뒤 `MemberService`로 회원을 만든다. |
| `web/auth/MemberLoginServlet.java` | 회원 로그인을 처리하고 안전한 내부 `next` 경로로만 복귀시킨다. |
| `web/auth/AdminLoginServlet.java` | 운영자 로그인을 처리하고 안전한 내부 `next` 경로 또는 대시보드로 보낸다. |
| `web/auth/LogoutServlet.java` | 현재 세션을 무효화한 뒤 회원 로그인 화면으로 이동시킨다. |
| `web/scheduler/NoShowScheduler.java` | 앱 시작 뒤 매 1분마다 `RentalService`를 호출해 기한 지난 예약을 노쇼로 바꾼다. |

## 9. 고객용 웹 요청 처리

| 파일 | 한 줄 역할 |
| --- | --- |
| `web/parking/ParkingLotListServlet.java` | 대여 조건이 없으면 운영 주차장을, 있으면 조건별 주차장 가용 차량 수를 보여 준다. |
| `web/parking/ParkingLotDetailServlet.java` | 선택 주차장의 활성 여부와 검색 조건을 확인해 실제 예약 가능 차량을 보여 준다. |
| `web/car/CarListServlet.java` | 고객에게 노출 가능한 전체 차량 목록을 `car-list.jsp`로 전달한다. |
| `web/car/CarScheduleServlet.java` | 선택 차량의 1~90일 예약 일정을 JSON으로 반환한다. |
| `web/member/ReservationCreateServlet.java` | 면허 상태를 재확인하고 예약 폼 표시·예약 생성을 처리한다. |
| `web/member/MemberReservationListServlet.java` | 로그인 회원의 예약·대여 이력을 조회해 내 예약 화면에 전달한다. |
| `web/member/ReservationCancelServlet.java` | 본인의 예약 취소 요청을 `RentalService`에 전달하고 결과 메시지를 보낸다. |
| `web/member/RentalStartServlet.java` | 본인의 예약을 실제 대여 상태로 전환하는 요청을 처리한다. |
| `web/member/RentalReturnServlet.java` | 본인의 진행 중 대여를 반납 완료 상태로 전환하는 요청을 처리한다. |
| `web/member/MemberProfileServlet.java` | 회원 정보와 최신 면허 상태를 조회해 내 정보 화면에 전달한다. |
| `web/member/MemberLicenseRegistrationServlet.java` | 회원의 면허 보유 상태와 세션 속성을 `true`로 변경한다. |

## 10. 운영자용 웹 요청 처리

| 파일 | 한 줄 역할 |
| --- | --- |
| `web/admin/AdminDashboardServlet.java` | 전체·대여 가능·대여 중 차량 수와 운영 주차장 수를 집계해 대시보드에 전달한다. |
| `web/admin/AdminCarListServlet.java` | 모든 차량과 모든 주차장을 차량 관리 화면에 전달한다. |
| `web/admin/CarRegistrationServlet.java` | 차량 정보와 선택 이미지를 검증·저장한 뒤 `CarService`로 차량을 등록한다. |
| `web/admin/CarStatusChangeServlet.java` | 운영자가 요청한 차량 상태 변경을 검증해 `CarService`에 전달한다. |
| `web/admin/CarPriceChangeServlet.java` | 운영자가 입력한 km당 요금 변경을 `CarService`에 전달한다. |
| `web/admin/CarParkingLotChangeServlet.java` | 차량의 현재 주차장을 이동시키는 운영자 요청을 처리한다. |
| `web/admin/CarImageReplaceServlet.java` | 새 이미지 파일을 저장하고 대표 이미지 DB 정보를 바꾼 뒤 이전 파일을 정리한다. |
| `web/admin/ParkingLotListServlet.java` | 모든 주차장을 조회해 주차장 관리 목록 화면에 전달한다. |
| `web/admin/ParkingLotRegistrationServlet.java` | 주차장 등록 폼 표시와 새 주차장 생성을 처리한다. |
| `web/admin/ParkingLotEditServlet.java` | 주차장 정보 수정 폼 표시와 수용 대수 규칙을 포함한 수정을 처리한다. |
| `web/admin/ParkingLotStatusChangeServlet.java` | 주차장 활성·비활성 상태 변경 요청을 처리한다. |

## 11. JSP 화면

| 파일 | 한 줄 역할 |
| --- | --- |
| `WEB-INF/views/home.jsp` | 비회원·회원에게 대여 시작 경로와 역할별 내비게이션을 제공하는 홈 화면이다. |
| `WEB-INF/views/member-signup.jsp` | 이름·로그인 ID·비밀번호·면허 여부를 받는 회원가입 화면이다. |
| `WEB-INF/views/member-login.jsp` | 회원 로그인 및 회원가입·운영자 로그인 이동을 제공하는 화면이다. |
| `WEB-INF/views/member-profile.jsp` | 회원 정보와 면허 등록 또는 예약 전 면허 안내를 보여 주는 화면이다. |
| `WEB-INF/views/member-reservation-list.jsp` | 내 예약을 상태별로 보여 주고 취소·대여 시작·반납 버튼을 제공한다. |
| `WEB-INF/views/parking-lot-list.jsp` | 시간·인원 조건으로 주차장과 가용 차량 수를 검색하는 고객 화면이다. |
| `WEB-INF/views/parking-lot-detail.jsp` | 선택 주차장의 조건 충족 차량을 보여 주고 예약 화면으로 연결한다. |
| `WEB-INF/views/car-list.jsp` | 고객용 전체 노출 차량, 대표 이미지, 예약 진입 버튼을 보여 준다. |
| `WEB-INF/views/reservation-form.jsp` | 예약 정보를 확인·제출하고 JavaScript로 차량 일정을 표시하는 화면이다. |
| `WEB-INF/views/admin-login.jsp` | 운영자 로그인 전용 화면이다. |
| `WEB-INF/views/admin-dashboard.jsp` | 차량·주차장 현황과 관리 메뉴를 보여 주는 운영자 첫 화면이다. |
| `WEB-INF/views/admin-car-form.jsp` | 차량 정보와 최대 5MB 대표 이미지를 등록하는 운영자 폼이다. |
| `WEB-INF/views/admin-car-list.jsp` | 차량별 상태·요금·주차장·이미지를 관리하는 운영자 목록 화면이다. |
| `WEB-INF/views/admin-parking-lot-form.jsp` | 주차장 이름·주소·수용 대수를 등록하는 운영자 폼이다. |
| `WEB-INF/views/admin-parking-lot-list.jsp` | 주차장 현황, 수정, 활성·비활성 변경을 제공하는 운영자 목록 화면이다. |
| `WEB-INF/views/admin-parking-lot-edit.jsp` | 선택 주차장의 이름·주소·수용 대수를 수정하는 운영자 폼이다. |
| `WEB-INF/views/error/access-denied.jsp` | 로그인 필요 또는 권한 부족 시 안내와 다음 행동을 보여 주는 401/403 화면이다. |
| `WEB-INF/views/error/bad-request.jsp` | 잘못된 요청의 400 오류 화면이다. |
| `WEB-INF/views/error/not-found.jsp` | 찾을 수 없는 경로·자원의 404 오류 화면이다. |
| `WEB-INF/views/error/server-error.jsp` | 처리 중 예외가 발생했을 때 보여 주는 500 오류 화면이다. |
| `WEB-INF/web.xml` | 세션 30분, HttpOnly 쿠키, 쿠키 기반 세션 추적, 400/404/500 오류 페이지를 설정한다. |
| `static/css/app.css` | 모든 JSP 화면의 공통 레이아웃, 카드, 폼, 상태 표시, 반응형 스타일을 정의한다. |
| `static/js/page-state-refresh.js` | 페이지 복귀 시 인증 상태 API를 조회해 세션 만료 뒤 화면을 새로고침한다. |

## 12. 테스트

| 파일 | 한 줄 역할 |
| --- | --- |
| `src/test/java/org/example/security/PasswordHasherTest.java` | 비밀번호 해시 검증과 잘못된 비밀번호 거부를 JUnit으로 테스트한다. |
| `src/test/java/org/example/service/AdminServiceTest.java` | 가짜 `AdminRepository`로 운영자 로그인과 해시 재갱신을 테스트한다. |

## 13. 도구·자동 생성 파일

### IDE 및 운영체제 메타데이터

| 파일 또는 범위 | 한 줄 역할 |
| --- | --- |
| `.idea/.gitignore` | IntelliJ가 추적하지 않을 IDE 내부 파일을 지정한다. |
| `.idea/compiler.xml` | IntelliJ 컴파일러 설정이다. |
| `.idea/gradle.xml` | IntelliJ의 Gradle 연동 설정이다. |
| `.idea/misc.xml` | IntelliJ 프로젝트의 일반 설정이다. |
| `.idea/workspace.xml` | 개인 IDE 창·작업 상태를 저장하는 로컬 설정이다. |
| `src/.DS_Store`, `src/main/.DS_Store`, `src/main/java/.DS_Store`, `src/main/java/org/.DS_Store`, `src/main/java/org/example/.DS_Store` | macOS Finder가 만든 표시 설정 파일이며 소스 실행과 무관하다. |
| `.archify/.DS_Store` | Archify 결과 폴더의 macOS Finder 메타데이터다. |

### 기존 Archify 산출물

| 파일 | 한 줄 역할 |
| --- | --- |
| `.archify/use-case-user-admin-20261013-160740/user-admin-process-map.html` | 사용자·운영자 업무 프로세스를 시각화한 독립 HTML 결과물이다. |
| `.archify/workflow-car-image-upload-20261013-152959/car-image-upload.html` | 차량 이미지 업로드 흐름을 시각화한 독립 HTML 결과물이다. |
| `.archify/workflow-car-image-upload-20261013-152959/candidate.json` | 차량 이미지 업로드 다이어그램 생성 후보 데이터다. |
| `.archify/workflow-car-image-upload-20261013-152959/car-image-upload.browser-check.json` | 차량 이미지 업로드 다이어그램의 브라우저 검증 결과다. |
| `.archify/workflow-car-image-upload-20261013-152959/browser-check-retry/car-image-upload.browser-check.json` | 차량 이미지 업로드 브라우저 검증 재시도 결과다. |
| `.archify/workflow-car-image-upload-20261013-152959/car-image-upload.delivery.json` | 차량 이미지 업로드 결과물 전달 정보다. |
| `.archify/workflow-car-image-upload-20261013-152959/car-image-upload.finalize.json` | 차량 이미지 업로드 다이어그램 마무리 처리 기록이다. |
| `.archify/workflow-car-image-upload-20261013-152959/car-image-upload.finalize-summary.json` | 차량 이미지 업로드 다이어그램 마무리 요약이다. |
| `.archify/workflow-member-login-auth-20261014-130651/member-login-auth.html` | 회원 로그인·인증 흐름을 시각화한 독립 HTML 결과물이다. |
| `.archify/workflow-member-login-auth-20261014-130651/candidate.json` | 회원 로그인·인증 다이어그램 생성 후보 데이터다. |
| `.archify/workflow-member-login-auth-20261014-130651/member-login-auth.browser-check.json` | 회원 로그인·인증 다이어그램의 브라우저 검증 결과다. |
| `.archify/workflow-member-login-auth-20261014-130651/member-login-auth.delivery.json` | 회원 로그인·인증 결과물 전달 정보다. |
| `.archify/workflow-member-login-auth-20261014-130651/member-login-auth.finalize.json` | 회원 로그인·인증 다이어그램 마무리 처리 기록이다. |
| `.archify/workflow-member-login-auth-20261014-130651/member-login-auth.finalize-summary.json` | 회원 로그인·인증 다이어그램 마무리 요약이다. |
| `.archify/workflow-user-admin-20261013-155354/user-admin-workflow.html` | 사용자·운영자 흐름을 시각화한 독립 HTML 결과물이다. |
| `.archify/workflow-user-admin-20261013-155354/candidate.json` | 사용자·운영자 다이어그램 생성 후보 데이터다. |
| `.archify/workflow-user-admin-20261013-155354/user-admin-workflow.browser-check.json` | 사용자·운영자 다이어그램의 브라우저 검증 결과다. |
| `.archify/workflow-user-admin-20261013-155354/user-admin-workflow.delivery.json` | 사용자·운영자 결과물 전달 정보다. |
| `.archify/workflow-user-admin-20261013-155354/user-admin-workflow.finalize.json` | 사용자·운영자 다이어그램 마무리 처리 기록이다. |
| `.archify/workflow-user-admin-20261013-155354/user-admin-workflow.finalize-summary.json` | 사용자·운영자 다이어그램 마무리 요약이다. |

### Gradle 및 build 산출물

| 파일 또는 범위 | 한 줄 역할 |
| --- | --- |
| `.gradle/9.6.0/checksums/*` | 의존성 파일의 체크섬 캐시와 잠금 파일로, Gradle이 자동 관리한다. |
| `.gradle/9.6.0/executionHistory/*` | 이전 Gradle 작업 실행 이력을 저장하는 캐시다. |
| `.gradle/9.6.0/fileChanges/*` | 증분 빌드에 쓸 파일 변경 기록이다. |
| `.gradle/9.6.0/fileHashes/*` | 증분 빌드에 쓸 파일 해시와 잠금 파일이다. |
| `.gradle/9.6.0/gc.properties`, `.gradle/vcs-1/gc.properties` | Gradle 캐시 정리 설정 파일이다. |
| `.gradle/buildOutputCleanup/*` | 이전 빌드 산출물 정리를 위한 Gradle 캐시다. |
| `.gradle/file-system.probe` | Gradle이 파일 시스템 특성을 확인한 결과다. |
| `build/classes/java/main/**/*.class` | `src/main/java`를 컴파일해 만든 실행용 바이트코드다. |
| `build/classes/java/test/**/*.class` | `src/test/java`를 컴파일해 만든 테스트 바이트코드다. |
| `build/resources/main/db.properties` | 런타임 클래스패스에 복사된 DB 설정 파일이다. |
| `build/libs/car_rental-1.0-SNAPSHOT.war` | Tomcat 같은 서블릿 컨테이너에 배포할 WAR 파일이다. |
| `build/reports/tests/test/**` | Gradle 테스트 실행 결과를 보여 주는 HTML·CSS·JS 보고서다. |
| `build/test-results/test/*.xml` | CI 도구가 읽는 JUnit XML 테스트 결과다. |
| `build/test-results/test/binary/*` | Gradle 내부 테스트 결과 바이너리다. |
| `build/tmp/compileJava/*`, `build/tmp/compileTestJava/*` | Java 증분 컴파일을 위한 임시 데이터다. |
| `build/tmp/war/MANIFEST.MF` | WAR 조립 과정에서 만든 매니페스트 임시 파일이다. |

`build/`과 `.gradle/`은 `./gradlew clean build`로 다시 만들 수 있는 산출물이며, 기능 수정은 원본 `src/`, `sql/`, Gradle 설정에서 한다.
