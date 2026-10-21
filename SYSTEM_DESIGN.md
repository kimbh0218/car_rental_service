# 프로세스 흐름 기반 구현 설계 문서

이 문서는 현재 코드에서 확인한 구현을 기준으로 설계를 역으로 정리한다. 고객 업무 흐름은 기능을 찾는 배경으로 사용하고, 본문은 이를 구현한 **5개 기능 단위**로 설명한다. 구현된 사실은 코드와 연결해 설명하고, 아직 구현하지 않은 개선안은 **향후 검토**로 구분한다.

## 프로세스 개요

| 단계 | 사용자 행동 | 관련 기능 단위 |
| --- | --- | --- |
| 1단계 | 회원가입 또는 로그인 | 인증·권한 |
| 2~3단계 | 대여 조건 입력, 주차장·차량 선택 | 차량·주차장 조회 |
| 4~6단계 | 예약 제출, 내 예약 관리, 차량 대여 시작 | 예약·대여·반납 |

## 본문 구성: 5개 기능 단위

| 기능 단위 | 중심 질문 | 알맞은 다이어그램 |
| --- | --- | --- |
| 인증·권한 | 누가 어떤 화면과 기능을 쓸 수 있는가 | 로그인·세션 플로우차트 |
| 차량·주차장 조회 | 어떤 조건에서 예약 가능한 차량을 찾는가 | 조회 흐름도 |
| 예약·대여·반납 | 예약과 차량 상태가 어떤 규칙으로 변하는가 | 예약·차량 상태 다이어그램 |
| 운영자 자원 관리 | 차량·주차장 수량과 상태를 어떻게 유지하는가 | 관리 업무 흐름도 |
| 차량 이미지 업로드 | 파일과 DB 정보를 어떻게 안전하게 함께 관리하는가 | 업로드 데이터 흐름 + 시퀀스 다이어그램 |

2단계와 3단계는 대여 조건으로 예약 가능 차량을 찾고 선택하는 하나의 조회 기능이다. 반납은 화면의 6단계 흐름에는 없지만 실제 구현되어 있으므로, 예약·대여 기능과 같은 상태 전이 단위로 함께 설명한다.

## 설계 관계도와 책임 분리

전체 구조 다이어그램: [system-relationships.html](.archify/architecture-system-relationships-20261015-114051/system-relationships.html)

### 관계도: 요청이 처리되는 기본 경로

브라우저의 요청은 먼저 Filter를 지나 인증 상태와 권한을 확인한다. 통과한 요청은 Servlet이 받아 파라미터를 읽고, 필요한 Service를 호출한다. Service는 업무 규칙과 트랜잭션을 처리한 뒤 DAO에 데이터 처리를 맡기며, DAO는 JDBC로 MySQL에 SQL을 실행한다.

화면 응답은 Servlet이 JSP로 데이터를 전달해 렌더링한다. JSP는 공통 CSS와 JavaScript를 사용하고, 차량 일정처럼 화면 갱신에 필요한 일부 데이터는 JSON API를 `fetch`로 호출한다.

### 관계도: 계층별 책임

| 계층 | 책임 | 대표 구성 요소 |
| --- | --- | --- |
| 화면 | 사용자에게 입력 폼·조회 결과·오류를 보여 준다. | JSP, `app.css`, `page-state-refresh.js` |
| 웹 요청 | HTTP 파라미터를 읽고 Service를 호출하며 JSP 또는 JSON 응답을 선택한다. | 각 Servlet |
| 업무 규칙 | 입력의 의미를 검증하고, 상태 전이와 여러 DB 변경을 트랜잭션으로 묶는다. | `MemberService`, `CarService`, `RentalService` 등 |
| 데이터 접근 | SQL 실행, 조회 결과 객체 변환, 필요한 행 잠금을 담당한다. | 각 DAO, `DBConnection` |
| 데이터·파일 저장 | 관계 데이터와 이미지 메타데이터는 MySQL에, 이미지 바이트는 로컬 저장소에 보관한다. | MySQL, `CarImageStorage` |

`domain`은 핵심 엔터티를, `dto`는 예약 요청·조회 결과처럼 특정 기능에 필요한 전달 데이터를 표현한다. 이 둘은 Servlet, Service, DAO가 직접 화면용 문자열이나 SQL 결과에 의존하지 않도록 중간 데이터 구조 역할을 한다.

### 관계도: 공통 기능과 도메인 기능의 경계

인증 상태 설정, 역할별 접근 제어, 민감 페이지 캐시 방지는 기능마다 반복하지 않고 Filter와 `SessionAuthentication`으로 공통 처리한다. 반면 예약 가능 여부, 차량 상태 변화, 주차장 수량 변화는 차량 대여 도메인 규칙이므로 `CarService`와 `RentalService`가 담당한다.

`NoShowScheduler`는 브라우저 요청과 무관하게 매 1분 `RentalService`를 호출해 기한이 지난 예약을 노쇼 상태로 바꾼다. 차량 이미지는 `CarImageStorage` 인터페이스로 파일 저장 책임을 분리하고, 현재는 `LocalCarImageStorage`가 구현한다.

### 관계도를 먼저 설명하는 이유

이후 5개 기능 단위를 설명할 때 각 기능이 어느 계층에서 무엇을 책임지는지 이미 합의된 상태가 된다. 따라서 “Servlet에도 검증이 있는데 Service에서도 검증하는 이유”나 “이미지 파일은 DB가 아닌 어디에 저장되는가” 같은 질문을 기능별 설명에서 자연스럽게 연결할 수 있다.

## 기능 1. 인증·권한

### 1.1 기능의 목적

회원과 운영자는 서로 다른 기능을 사용한다. 회원은 예약·대여·반납 기능을, 운영자는 차량·주차장 관리 기능을 사용하므로, 로그인 성공 여부뿐 아니라 **역할에 맞는 경로만 접근하도록 제어**해야 한다.

인증 흐름 다이어그램: [authentication-workflow.html](.archify/workflow-authentication-design-20261015-111756/authentication-workflow.html)

### 1.2 처리 흐름

1. 사용자가 회원 또는 운영자 로그인 화면에서 로그인 ID와 비밀번호를 제출한다.
2. 각 `LoginServlet`은 입력을 해당 Service로 전달하고, 로그인 성공 뒤에만 내부 `next` 경로 또는 기본 화면으로 이동시킨다.
3. `MemberService` 또는 `AdminService`는 DAO에서 계정과 저장된 비밀번호 해시를 조회하고, `PasswordHasher`로 비밀번호를 비교한다.
4. 인증에 성공하면 `SessionAuthentication`이 기존 세션을 무효화하고 새 세션을 만든다.
5. 이후 `/member/*` 요청은 `MemberAuthenticationFilter`, `/admin/*` 요청은 `AdminAuthenticationFilter`가 역할과 식별자 속성을 확인한다.
6. 인증되지 않은 요청은 401, 다른 역할의 요청은 403 접근 거부 화면으로 보낸다.

### 1.3 설계 결정과 구현 근거

#### 역할별 인증 경로와 단일 세션

회원과 운영자는 각각 `/auth/member/login`, `/auth/admin/login`으로 로그인한다. 회원 로그인에는 `auth.memberId`와 면허 상태를, 운영자 로그인에는 `auth.adminId`를 저장하고, 두 경우 모두 `auth.loginType`으로 역할을 기록한다.

로그인할 때 기존 세션을 먼저 무효화한 뒤 새 세션을 생성한다. 따라서 동일 브라우저에서 회원과 운영자 역할이 동시에 유지되지 않으며, 역할 전환 시 이전 로그인 정보가 남지 않는다.

#### 비밀번호를 평문으로 비교하지 않는 방식

`PasswordHasher`는 PBKDF2-HMAC-SHA256, 반복 횟수 **600,000회**, 무작위 salt **16바이트**로 비밀번호 해시를 생성한다. 로그인 시에는 저장된 해시의 salt와 반복 횟수로 다시 계산한 값을 상수 시간 비교 방식으로 대조한다.

해시 정책이 바뀐 경우에는 성공 로그인 뒤 기존 해시의 반복 횟수를 확인하고, 낮은 반복 횟수로 생성된 해시를 새 정책으로 갱신한다.

#### URL 단위의 권한 경계

`MemberAuthenticationFilter`와 `AdminAuthenticationFilter`는 각각 `/member/*`, `/admin/*`에 적용된다. 단순히 세션 유무만 확인하지 않고, 요청 경로가 요구하는 역할과 세션 역할이 같은지 확인한다.

로그인 후 복귀할 `next` 값은 `/`로 시작하는 내부 경로만 허용하고, `//` 또는 `://`가 포함된 값은 거부한다. 외부 사이트로 사용자를 보내는 Open Redirect 위험을 줄이기 위한 처리다.

### 1.4 사용 기술과 역할

| 기술 | 이 기능에서의 역할 |
| --- | --- |
| Jakarta Servlet | 로그인 요청을 받고, 성공 시 리디렉션하거나 실패 시 JSP를 다시 렌더링한다. |
| JSP·JSTL | 회원·운영자 로그인 화면과 오류 메시지를 서버에서 렌더링한다. |
| `HttpSession` | 로그인 역할과 사용자 식별자를 서버 세션에 보관한다. |
| Servlet Filter | URL 패턴별로 인증·인가를 공통 처리해 개별 Servlet의 중복 검증을 줄인다. |
| PBKDF2-HMAC-SHA256 | 비밀번호를 salt와 반복 연산으로 해시해 DB에 평문을 저장하지 않는다. |
| JDBC·MySQL | 계정과 비밀번호 해시를 조회하고, 필요한 경우 해시를 갱신한다. |

### 1.5 구현에서 생각해 볼 설계 사항

#### 현재 구현으로 보장하는 것

* 로그인마다 세션을 새로 만들어 기존 세션 속성을 이어받지 않는다.
* `web.xml`은 세션 쿠키에 `HttpOnly`를 설정하고, 세션 유효 시간을 **30분**으로 설정한다.
* 인증·회원·운영자 경로에는 캐시 방지 헤더를 적용해 로그아웃 뒤 브라우저 뒤로 가기로 민감 화면을 다시 보는 문제를 줄인다.

#### 향후 검토 사항

* POST 폼에서 CSRF 토큰 검증은 현재 코드에서 확인되지 않는다. 외부 사이트의 요청 위조를 막아야 하는 배포 환경이라면 토큰 검증을 추가할지 결정해야 한다.
* 로그인 실패 횟수 제한, 계정 잠금, IP 기반 요청 제한은 현재 구현되어 있지 않다. 공개 서비스 규모와 위협 모델에 따라 필요성을 판단해야 한다.
* `HttpOnly`는 설정되어 있지만, HTTPS 배포 시 필요한 `Secure`·`SameSite` 쿠키 속성은 배포 환경에 맞춰 별도로 점검해야 한다.

### 1.6 주요 구현 파일

| 책임 | 파일 |
| --- | --- |
| 회원 가입·로그인 화면 및 처리 | `web/auth/MemberSignupServlet.java`, `web/auth/MemberLoginServlet.java` |
| 운영자 로그인 처리 | `web/auth/AdminLoginServlet.java` |
| 세션 생성·갱신·로그아웃 | `web/auth/SessionAuthentication.java`, `web/auth/LogoutServlet.java` |
| 역할 기반 접근 제어 | `web/auth/MemberAuthenticationFilter.java`, `web/auth/AdminAuthenticationFilter.java` |
| 인증·해시 검증 | `service/MemberService.java`, `service/AdminService.java`, `security/PasswordHasher.java` |
| 계정 조회 | `dao/MemberDao.java`, `dao/AdminDao.java` |
| 세션·캐시 설정 | `WEB-INF/web.xml`, `web/filter/DynamicPageCacheControlFilter.java` |

## 기능 2. 차량·주차장 조회

### 2.1 기능의 목적

고객은 대여 시작·반납 시각과 탑승 인원을 먼저 입력하고, 그 조건에서 이용 가능한 주차장과 차량을 찾는다. 단순 차량 목록이 아니라 **요청한 시간에 실제 예약 가능한 차량**만 보여 주는 것이 이 기능의 목적이다. 이 기능은 업무 흐름의 2단계와 3단계를 하나로 묶는다.

조회 흐름 다이어그램: [vehicle-parking-search-sequence.html](.archify/sequence-vehicle-parking-search-20261015-123000/vehicle-parking-search-sequence.html)

### 2.2 조회 흐름

1. `ParkingLotListServlet`은 조건이 없으면 운영 중인 주차장 목록을, 조건이 있으면 주차장별 예약 가능 차량 수를 보여 준다.
2. 고객이 주차장을 선택하면 `ParkingLotDetailServlet`이 주차장 ID와 대여 조건을 받는다.
3. `ParkingLotService`로 선택 주차장이 실제로 운영 중인지 확인하고, `CarService`가 시간·인원 조건을 검증한다.
4. `CarDao`는 차량, 주차장, 예약, 대표 이미지 테이블을 결합해 조건을 충족하는 차량만 조회한다.
5. 조회 결과는 `parking-lot-detail.jsp`에 표시되고, 고객은 이 목록에서 예약할 차량을 선택한다.

### 2.3 예약 가능 차량을 판단하는 규칙

`CarService`는 대여 시작 시각이 현재보다 이후인지, 반납 시각이 시작 시각보다 이후인지, 대여 시간이 최소 **30분**인지, 탑승 인원이 **1~9명**인지 검사한다. 선택한 주차장은 존재하고 `ACTIVE` 상태여야 한다.

`CarDao.findAvailableByParkingLotAndPeriod()`은 다음 조건을 SQL에서 동시에 적용한다.

* 차량의 현재 주차장이 선택한 주차장과 같을 것
* 차량 상태가 `AVAILABLE`일 것
* 주차장 상태가 `ACTIVE`일 것
* 차량 최대 탑승 인원이 요청 인원 이상일 것
* `RESERVED` 또는 `IN_PROGRESS` 예약의 시간 구간과 겹치지 않을 것

시간 중복은 기존 예약의 시작 시각이 새 반납 시각보다 빠르고, 기존 예약의 반납 시각이 새 시작 시각보다 늦을 때로 판단한다. 따라서 두 예약의 끝과 시작이 정확히 맞닿는 경우는 겹치지 않는 것으로 처리한다.

### 2.4 차량 일정 조회를 분리한 이유

예약 폼은 `CarScheduleServlet`에 차량 ID와 조회 기간을 요청하고, JSON으로 받은 `RESERVED`·`IN_PROGRESS` 일정만 표시한다. 기본 조회 기간은 **7일**, 허용 범위는 **1~90일**이다.

이 기능은 고객에게 일정 정보를 보여 주기 위한 조회다. 예약 가능 차량 목록 조회와 달리 예약을 확정하지 않으며, 실제 예약 생성 직전에는 `RentalService`가 차량 행을 잠그고 시간 중복을 다시 검증한다.

### 2.5 사용 기술과 역할

| 기술 | 이 기능에서의 역할 |
| --- | --- |
| Jakarta Servlet·JSP | 검색 조건을 받고, 주차장·차량 조회 결과와 오류 메시지를 화면에 전달한다. |
| Service 계층 | 형식이 아닌 업무 조건, 즉 시간 범위·최소 대여 시간·인원·주차장 운영 상태를 검증한다. |
| JDBC·MySQL | `JOIN`과 `NOT EXISTS`로 필요한 차량만 DB에서 선별한다. |
| JavaScript `fetch` | 예약 폼에서 차량 일정 API를 호출해 JSON 일정을 표시한다. |
| DTO | `ParkingLotAvailability`, `CarWithParkingLot`, `CarScheduleItem`으로 화면에 필요한 조회 결과만 전달한다. |

### 2.6 구현에서 생각해 볼 설계 사항

#### 조회 결과와 예약 확정은 다른 문제

조회 시점에는 차량이 가능해 보여도, 다른 사용자가 먼저 예약할 수 있다. 따라서 조회 SQL만으로 동시 예약을 완전히 막을 수 없다. 현재 구현은 예약 생성 시 `RentalService`에서 차량 행을 `FOR UPDATE`로 잠그고, 활성 예약 시간 중복을 다시 검사하는 방식으로 최종 확정을 보호한다.

#### 검증 책임을 나눈 이유

Servlet은 날짜·숫자 파싱 오류를 처리하고, Service는 대여 시간·인원·주차장 상태 같은 업무 규칙을 처리하며, DAO는 차량·주차장·예약 테이블을 결합한 후보 선별을 담당한다. 이 분리는 동일 규칙을 예약 생성에서도 재사용할 수 있게 한다.

#### 향후 검토 사항

* 예약 데이터가 늘면 `rental(car_id, rental_status, start_time, return_time)`처럼 시간 중복 조회에 맞는 인덱스가 필요한지 실제 실행 계획으로 확인해야 한다. 현재 프로젝트의 SQL 파일에서는 이 인덱스 정의를 확인하지 못했다.
* 일정 조회 API는 문자열을 직접 조합해 JSON을 만든다. 현재 값은 서버가 만든 날짜와 enum이어서 제어되지만, 일반적인 API가 커지면 JSON 라이브러리 도입을 검토할 수 있다.

### 2.7 주요 구현 파일

| 책임 | 파일 |
| --- | --- |
| 주차장 조건 검색 | `web/parking/ParkingLotListServlet.java`, `service/CarService.java`, `dao/CarDao.java` |
| 선택 주차장의 가용 차량 조회 | `web/parking/ParkingLotDetailServlet.java`, `service/ParkingLotService.java`, `service/CarService.java`, `dao/CarDao.java` |
| 차량 일정 JSON API | `web/car/CarScheduleServlet.java`, `service/RentalService.java`, `dao/RentalDao.java` |
| 조회 화면과 일정 표시 | `WEB-INF/views/parking-lot-list.jsp`, `WEB-INF/views/parking-lot-detail.jsp`, `WEB-INF/views/reservation-form.jsp` |

<!--
아래 초안은 기능 3인 "예약·대여·반납"으로 재구성하기 전의 단계별 작성본이다.
기능 2 검토가 끝난 뒤 승인받아 상태 다이어그램 중심의 본문으로 교체한다.

## 4단계. 예약 정보 제출

### 4.1 기능의 목적

차량 조회 화면에서 보인 결과는 특정 시점의 안내 정보다. 고객이 예약을 제출하는 순간에는 같은 차량을 다른 고객이 먼저 예약했을 수 있으므로, **예약 생성 시점에 다시 검증하고 예약 행을 확정**해야 한다.

예약 확정 흐름 다이어그램: [reservation-submit-workflow.html](.archify/workflow-reservation-submit-design-20261015-120000/reservation-submit-workflow.html)

### 4.2 처리 흐름

1. `ReservationCreateServlet`은 차량 ID, 시작·반납 시각, 탑승 인원을 파싱해 로그인 세션의 회원 ID와 함께 `ReservationRequest`를 만든다.
2. `RentalService.reserve()`는 하나의 JDBC Connection에서 자동 커밋을 끄고 트랜잭션을 시작한다.
3. `CarDao.findByIdForUpdate()`로 차량 행을 먼저 잠근 뒤 회원 면허 상태, 차량 `AVAILABLE` 상태, 현재 주차 위치, 출발 주차장 `ACTIVE` 상태와 최대 탑승 인원을 확인한다.
4. `RentalDao.existsOverlappingActiveRental()`으로 `RESERVED`·`IN_PROGRESS` 예약과의 시간 중복을 다시 검사한다.
5. 모든 조건을 통과하면 `rental` 테이블에 `RESERVED` 상태의 예약을 저장하고 커밋한다. 하나라도 실패하면 롤백하고 예약 폼에 오류를 표시한다.

### 4.3 설계 결정과 구현 근거

#### 조회와 확정을 분리한 이유

2~3단계의 차량 목록은 사용자가 선택할 후보를 보여 주기 위한 조회다. 동시 요청에서 그 결과만 믿고 저장하면 같은 차량·시간대에 두 예약이 만들어질 수 있다.

그래서 최종 예약은 차량 행 잠금(`FOR UPDATE`) 뒤에 활성 예약 중복을 재검사한다. 차량을 먼저 잠가 같은 차량을 대상으로 한 예약 확정 요청을 순서대로 처리하게 한 설계다.

#### 미래 예약은 차량을 이동시키지 않는 방식

예약을 생성할 때는 `rental` 행과 출발 주차장 ID만 저장한다. 차량은 계속 `AVAILABLE` 상태이고 주차장 수량도 변하지 않는다.

실제 차량·주차장 상태 변화는 6단계의 대여 시작 시점에 처리한다. 따라서 예약 취소는 `CANCELED` 상태 변경만 하면 되며, 아직 이동하지 않은 차량이나 주차장 수량을 되돌릴 필요가 없다.

#### 트랜잭션에 포함한 범위

차량 잠금, 출발 주차장 잠금, 중복 확인, 예약 저장은 모두 같은 Connection에서 처리한다. 오류가 나면 `rollback()`하므로 일부 검증만 통과하거나 예약만 남는 상태를 막는다.

### 4.4 사용 기술과 역할

| 기술 | 이 기능에서의 역할 |
| --- | --- |
| Jakarta Servlet·JSP | POST 입력을 `ReservationRequest`로 만들고, 오류면 기존 입력값과 함께 예약 폼을 다시 표시한다. |
| JDBC Transaction | 자동 커밋을 끄고 검증과 저장을 하나의 성공 또는 실패 단위로 묶는다. |
| MySQL `FOR UPDATE` | 동일 차량 예약 요청이 동시에 최종 검증을 통과하지 않도록 차량·주차장 행을 잠근다. |
| SQL 시간 중복 조건 | `start_time < 요청 반납` 및 `return_time > 요청 시작`으로 활성 예약의 겹침을 판별한다. |
| DTO·Domain | `ReservationRequest`로 입력을 전달하고 `Rental`로 저장될 예약 상태를 표현한다. |

### 4.5 구현에서 생각해 볼 설계 사항

#### 현재 구현으로 보장하는 것

* 면허 보유 회원, `AVAILABLE` 차량, `ACTIVE` 출발 주차장, 차량 정원 이하 인원, 미래의 최소 **30분** 대여 시간만 예약할 수 있다.
* `RESERVED`와 `IN_PROGRESS` 상태만 시간 중복 검사 대상으로 삼는다. `CANCELED`와 `NO_SHOW` 예약은 같은 시간대의 새 예약을 막지 않는다.
* 예약 생성 뒤 차량 상태가 곧바로 `RENTED`로 바뀌지는 않는다. 실제 픽업 전에도 다른 시간대의 미래 예약은 가능하다.

#### 향후 검토 사항

* 현재의 행 잠금은 하나의 DB 인스턴스에서 차량별 확정을 직렬화한다. 서비스가 여러 인스턴스로 확장되더라도 같은 MySQL을 공유한다는 전제가 유지되는지 점검해야 한다.
* 시간 중복 조회가 많아지면 차량 ID·예약 상태·시간 범위에 맞는 인덱스를 실제 실행 계획으로 검증해야 한다. 현재 SQL 파일에서는 해당 인덱스 정의를 확인하지 못했다.

### 4.6 주요 구현 파일

| 책임 | 파일 |
| --- | --- |
| 예약 폼 표시·제출 처리 | `web/member/ReservationCreateServlet.java` |
| 예약 규칙·트랜잭션 | `service/RentalService.java` |
| 차량·주차장 행 잠금 | `dao/CarDao.java`, `dao/ParkingLotDao.java` |
| 예약 저장·시간 중복 검사 | `dao/RentalDao.java` |
| 예약 입력·화면 | `dto/ReservationRequest.java`, `WEB-INF/views/reservation-form.jsp` |

## 5단계. 내 예약 조회 또는 취소

### 5.1 기능의 목적

회원은 자신의 예약 상태를 확인하고, 아직 시작하지 않은 예약만 취소할 수 있다. 핵심은 화면에서 보이는 예약 ID를 믿지 않고 **세션의 회원 ID를 기준으로 조회하고, 변경 직전에도 소유자를 확인하는 것**이다.

내 예약·취소 흐름 다이어그램: [my-reservation-workflow.html](.archify/workflow-my-reservation-design-20261015-120000/my-reservation-workflow.html)

### 5.2 처리 흐름

1. `MemberReservationListServlet`은 세션의 `auth.memberId`만 사용해 `RentalService.findMyRentals()`를 호출한다.
2. `RentalDao.findMyRentalsByMemberId()`는 `member_id` 조건으로 예약, 차량, 출발 주차장 정보를 조회해 목록 화면에 전달한다.
3. 회원이 취소를 요청하면 `ReservationCancelServlet`이 예약 ID와 세션의 회원 ID를 `RentalService.cancelReservation()`에 전달한다.
4. Service는 예약 행을 `FOR UPDATE`로 잠그고, 예약 소유자가 현재 회원인지와 상태가 `RESERVED`인지 다시 확인한다.
5. 통과하면 상태만 `CANCELED`로 변경하고 커밋한 뒤 목록으로 리디렉션한다.

### 5.3 설계 결정과 구현 근거

#### 조회 범위와 변경 권한을 모두 제한한 이유

목록 조회는 SQL의 `WHERE r.member_id = ?`로 처음부터 다른 회원의 예약을 제외한다. 하지만 취소 요청은 사용자가 URL이나 폼 값을 바꿔 보낼 수 있으므로 목록 필터만으로는 충분하지 않다.

취소 처리에서도 잠근 예약 행의 `memberId`와 세션 회원 ID를 비교한다. 둘이 다르면 “본인 예약만 변경할 수 있습니다” 오류로 종료한다.

#### 취소 가능한 상태를 한정한 이유

`cancelReservation()`은 `RESERVED`에서만 `CANCELED`로 바꾼다. 이미 `IN_PROGRESS`, `COMPLETED`, `CANCELED`, `NO_SHOW` 상태인 예약은 취소할 수 없다.

이 규칙은 중복 클릭이나 동시에 들어온 취소 요청에서도 의미를 유지한다. 첫 요청이 상태를 바꾸면, 다음 요청은 잠금 해제 후 더 이상 `RESERVED`가 아니므로 거절된다.

#### 취소 시 차량·주차장을 수정하지 않는 이유

4단계의 미래 예약은 차량 상태와 주차장 수량을 바꾸지 않았다. 따라서 취소는 예약 상태만 바꾸면 되고, 차량 위치나 주차장 수량을 복구하는 처리는 필요하지 않다.

### 5.4 사용 기술과 역할

| 기술 | 이 기능에서의 역할 |
| --- | --- |
| `HttpSession` | 클라이언트가 보낸 회원 ID 대신 서버 세션의 회원 ID로 조회·취소 범위를 정한다. |
| Jakarta Servlet·JSP | 목록을 렌더링하고, 취소 결과 메시지와 함께 목록으로 리디렉션한다. |
| JDBC·MySQL | 회원별 예약 목록을 `JOIN`으로 조회하고, 취소 대상 행은 `FOR UPDATE`로 잠근다. |
| 상태 enum | `RentalStatus`로 화면 표시와 허용된 상태 전이를 같은 값으로 관리한다. |

### 5.5 구현에서 생각해 볼 설계 사항

#### 현재 구현으로 보장하는 것

* 내 예약 목록은 회원 ID로 제한되며, 취소는 본인 예약만 가능하다.
* 취소 가능한 예약은 `RESERVED` 상태에 한정된다.
* 취소 후에도 이력은 삭제되지 않고 `CANCELED` 상태로 남는다.

#### 향후 검토 사항

* 취소 마감 시각이나 위약금 규칙은 현재 구현되어 있지 않다. 정책이 생기면 `RESERVED` 여부 외에 시작 시각과 요금 정보를 함께 검증해야 한다.
* 취소 POST 요청의 CSRF 방어는 현재 코드에서 확인되지 않는다. 인증된 사용자의 상태를 바꾸는 요청이므로 배포 환경에서는 토큰 검증을 검토해야 한다.

### 5.6 주요 구현 파일

| 책임 | 파일 |
| --- | --- |
| 내 예약 목록 | `web/member/MemberReservationListServlet.java`, `service/RentalService.java`, `dao/RentalDao.java` |
| 예약 취소 요청 | `web/member/ReservationCancelServlet.java`, `service/RentalService.java` |
| 목록·상태별 버튼 표시 | `WEB-INF/views/member-reservation-list.jsp` |
| 상태 값 | `domain/RentalStatus.java` |

## 6단계. 예약 시각에 차량 대여 시작

### 6.1 기능의 목적

예약이 있다고 해서 차량이 이미 대여 중인 것은 아니다. 고객이 예약 시작 시각 이후에 실제로 픽업하면, 예약·차량·주차장 정보를 함께 바꿔 **차량이 주차장을 떠나 대여 중임을 시스템 상태에 반영**한다.

대여 시작 상태 다이어그램: [rental-start-lifecycle.html](.archify/lifecycle-rental-start-design-20261015-120000/rental-start-lifecycle.html)

### 6.2 처리 흐름

1. `RentalStartServlet`은 세션 회원 ID와 예약 ID를 `RentalService.startRental()`에 전달한다.
2. Service는 예약 행을 잠그고, 본인 예약인지, 상태가 `RESERVED`인지, 예약 시작 시각 이후인지 확인한다.
3. 예약 시작 후 **10분** 이상 지났다면 해당 예약을 `NO_SHOW`로 바꾸고 대여 시작을 거절한다.
4. 허용 시간 안이면 차량 행을 잠근 뒤 차량이 `AVAILABLE`이고 예약한 출발 주차장에 실제로 있는지 확인한다.
5. 하나의 트랜잭션에서 Rental을 `IN_PROGRESS`와 실제 픽업 시각으로, Car를 `RENTED`로 변경한다. 이어 출발 주차장의 현재 주차 차량 수는 1 감소시키고 대여 차량 수는 1 증가시킨다.
6. `NoShowScheduler`도 매 **1분** 만료된 `RESERVED` 예약을 검사해, 시작 시각이 10분 지난 예약을 `NO_SHOW`로 일괄 변경한다.

### 6.3 설계 결정과 구현 근거

#### 차량 주차 위치를 “삭제”하지 않는 이유

대여 시작 시 차량 레코드 자체를 삭제하지 않는다. `CarDao.updateStatusAndClearParkingLot()`은 차량 상태를 `RENTED`로 바꾸고, 대여 중에는 특정 주차장에 있지 않으므로 `current_parking_lot_id`와 `current_address`만 `NULL`로 바꾼다.

예약의 `pickup_parking_lot_id`는 그대로 남는다. 즉, 현재 위치 정보는 비우지만 출발 주차장 이력은 `rental`에 보존한다.

#### 세 상태를 같이 바꾸는 이유

대여 시작은 Rental 하나의 상태만 바꾸는 작업이 아니다. 차량이 주차장을 떠났으므로 차량 상태, 현재 위치, 주차장 내 차량 수, 주차장이 대여 차량을 위해 보장하는 수량도 함께 바뀌어야 한다.

이 변경은 하나의 트랜잭션이다. 어느 하나라도 1건 변경에 실패하면 롤백해 `IN_PROGRESS`인데 차량은 `AVAILABLE`인 불일치 상태를 막는다.

#### 요청 처리와 스케줄러를 함께 둔 이유

고객이 10분 뒤에 직접 대여 시작을 요청한 경우에는 요청 처리 안에서 `NO_SHOW`로 전환한다. 고객이 아예 요청하지 않는 경우에도 남은 예약이 계속 활성으로 보이지 않도록 `NoShowScheduler`가 매 1분 같은 기준으로 정리한다.

### 6.4 사용 기술과 역할

| 기술 | 이 기능에서의 역할 |
| --- | --- |
| Jakarta Servlet | 대여 시작 POST를 받고 성공·실패 메시지와 함께 내 예약 목록으로 돌려보낸다. |
| JDBC Transaction·`FOR UPDATE` | 예약·차량 행을 잠그고 여러 상태·수량 변경을 원자적으로 커밋한다. |
| 상태 enum | Rental의 `RESERVED → IN_PROGRESS` 또는 `NO_SHOW`, Car의 `AVAILABLE → RENTED` 전이를 표현한다. |
| `ScheduledExecutorService` | 웹 요청과 별개로 1분 간격의 노쇼 정리를 수행한다. |
| MySQL 수량 조건 UPDATE | 주차장 현재 차량 수가 0보다 클 때만 감소시키고, 수용 가능할 때만 대여 차량 수를 증가시킨다. |

### 6.5 구현에서 생각해 볼 설계 사항

#### 현재 구현으로 보장하는 것

* 대여 시작은 예약 시작 시각 이후부터 **10분 이내**에만 가능하다.
* 차량이 출발 주차장에 있고 `AVAILABLE` 상태일 때만 대여를 시작할 수 있다.
* 대여 시작 후 차량은 `RENTED` 상태이며 현재 주차장·주소는 `NULL`이다. 이는 차량 삭제가 아니라 대여 중 위치가 없음을 나타낸다.
* 노쇼 예약은 요청 시점 또는 스케줄러 검사로 `NO_SHOW` 상태가 된다.

#### 향후 검토 사항

* `ScheduledExecutorService`는 애플리케이션 인스턴스 안에서 실행된다. 서버를 여러 대로 늘릴 경우 같은 노쇼 작업이 중복 실행되지 않도록 단일 실행 보장이나 분산 스케줄링 방식을 검토해야 한다.
* 실제 키 전달, 차량 문 열기, GPS 위치 수신은 현재 구현 범위에 없다. 외부 차량 제어 장치가 추가되면 DB 상태 변경과 외부 명령 성공 여부를 어떻게 조정할지 별도 설계가 필요하다.

### 6.6 주요 구현 파일

| 책임 | 파일 |
| --- | --- |
| 대여 시작 요청 | `web/member/RentalStartServlet.java` |
| 대여 시작·노쇼 업무 규칙 | `service/RentalService.java` |
| 노쇼 주기 실행 | `web/scheduler/NoShowScheduler.java` |
| 예약 상태·실제 픽업 시각 변경 | `dao/RentalDao.java` |
| 차량 상태·현재 주차 위치 변경 | `dao/CarDao.java` |
| 주차장 현재·대여 차량 수 변경 | `dao/ParkingLotDao.java` |
-->

## 기능 3. 예약·대여·반납

### 3.1 기능의 목적

예약은 미래의 이용 의사를 저장하는 일이고, 대여 시작은 차량이 실제로 주차장을 떠나는 일이다. 이 둘을 구분해, **예약 상태·차량 상태·주차장 수량이 언제 함께 바뀌는지**를 관리한다.

예약·차량 상태 다이어그램: [reservation-state-transition-workflow.html](.archify/workflow-reservation-state-transitions-20261015-134000/reservation-state-transition-workflow.html)

### 3.2 상태 전이

1. 예약 생성은 차량 행을 잠그고 시간 중복을 재검증한 뒤 `Rental`을 `RESERVED`로 저장한다. 이 시점에는 차량 `AVAILABLE` 상태와 주차장 수량을 바꾸지 않는다.
2. 회원은 본인의 `RESERVED` 예약만 `CANCELED`로 바꿀 수 있다. 취소는 아직 차량이 이동하지 않았으므로 차량·주차장 수량을 복구하지 않는다.
3. 예약 시작 시각 뒤 **10분**이 지나면 대여 시작 요청 또는 `NoShowScheduler`가 예약을 `NO_SHOW`로 바꾼다. 스케줄러는 매 **1분** 실행된다.
4. 시작 시각 이후 10분 안에 실제 대여를 시작하면 Rental은 `IN_PROGRESS`, Car는 `RENTED`가 된다. 차량의 현재 주차장 ID와 주소는 `NULL`로 바뀌며, 이는 차량 삭제가 아니라 대여 중 특정 주차장에 없음을 뜻한다.
5. 반납하면 Rental은 `COMPLETED`, Car는 다시 `AVAILABLE`이 되고 출발 주차장과 주소를 복구한다.

### 3.3 주차장 수량을 함께 변경하는 이유

현재 구현은 대여 시작 때 출발 주차장의 `current_parking_count`를 1 줄이고 `rented_car_count`를 1 늘린다. 반납하면 반대로 변경한다.

`rented_car_count`는 대여 중인 차량이 출발 주차장으로 돌아올 자리를 보장하는 값이다. 차량 등록·이동·주차장 최대 수용 대수 변경은 `current_parking_count + rented_car_count`를 기준으로 정원을 판단한다. 따라서 대여 상태만 바꾸고 주차장 수량을 바꾸지 않으면, 실제 대여 차량을 고려하지 못한 채 새 차량을 등록할 수 있다.

### 3.4 설계 결정과 구현 근거

#### 여러 엔터티를 하나의 트랜잭션으로 묶는 이유

대여 시작에서는 Rental 상태·실제 픽업 시간, Car 상태·현재 위치, ParkingLot 수량이 함께 변경된다. `RentalService.startRental()`은 자동 커밋을 끈 하나의 JDBC Connection에서 이 작업을 수행하고, 중간에 실패하면 롤백한다.

반납도 동일하게 Rental 상태·실제 반납 시간, Car 위치·상태, 주차장 수량을 하나의 트랜잭션으로 되돌린다. 이 설계는 `IN_PROGRESS`인데 차량은 `AVAILABLE`인 불일치 상태를 막는다.

#### 대여 시작 가능 시간을 제한한 이유

대여 시작은 예약 시각 이전에는 불가능하며, 시작 뒤 **10분** 안에만 허용한다. 기한을 넘긴 예약은 `NO_SHOW`로 전환돼 동일 시간대 차량을 계속 점유하지 않는다.

### 3.5 사용 기술과 역할

| 기술 | 이 기능에서의 역할 |
| --- | --- |
| Jakarta Servlet | 예약 생성·취소·대여 시작·반납 POST 요청을 받고 결과 화면으로 이동시킨다. |
| JDBC Transaction·`FOR UPDATE` | 예약 확정에서 차량·주차장 행을 잠그고, 여러 상태 변경을 하나의 커밋 또는 롤백 단위로 처리한다. |
| 상태 enum | `RentalStatus`와 `CarStatus`로 허용된 상태 전이를 표현한다. |
| `ScheduledExecutorService` | 매 1분 시작 시각을 10분 넘긴 `RESERVED` 예약을 노쇼로 처리한다. |
| MySQL 조건 UPDATE | 주차 차량 수를 0 미만으로 내리지 않고, 정원을 초과하지 않을 때만 수량을 늘린다. |

### 3.6 구현에서 생각해 볼 설계 사항

#### 현재 구현으로 보장하는 것

* 예약 조회와 실제 예약 저장은 다르다. 예약 저장 직전 차량 행을 잠그고 활성 예약의 시간 중복을 다시 확인한다.
* 예약 취소·노쇼는 미래 예약 상태만 끝내며, 차량과 주차장 수량을 바꾸지 않는다.
* 대여 시작과 반납은 Rental·Car·ParkingLot 변경을 같은 트랜잭션에 포함한다.

#### 향후 개선 사항: `rented_car_count` 파생값 전환

현재 `parking_lot.rented_car_count`는 `IN_PROGRESS` 대여를 시작할 때 증가시키고 반납할 때 감소시키는 저장된 카운터다. 이 값은 출발 주차장 기준 `Rental`의 `IN_PROGRESS` 건수에서 계산할 수 있으므로, 두 곳에 같은 사실을 저장해 값이 어긋날 가능성이 있다.

향후에는 `rented_car_count` 컬럼을 제거하고 `COUNT(*) FROM rental WHERE pickup_parking_lot_id = ? AND rental_status = 'IN_PROGRESS'`로 계산할 수 있다. 단순 컬럼 삭제가 아니라 차량 등록·이동, 대여 시작·반납, 주차장 정원 변경이 같은 주차장 행을 잠근 뒤 이 값을 계산하도록 함께 변경해야 기존의 반납 자리 보장 동작을 유지할 수 있다.

현재 반납 주차장은 출발 주차장으로 고정되어 있으므로 `pickup_parking_lot_id` 기준 계산이 맞다. 다른 주차장 반납을 지원하면 반납 예정 주차장을 별도 모델로 추가해야 한다.

### 3.7 주요 구현 파일

| 책임 | 파일 |
| --- | --- |
| 예약 생성 | `web/member/ReservationCreateServlet.java`, `service/RentalService.java`, `dao/RentalDao.java` |
| 내 예약 조회·취소 | `web/member/MemberReservationListServlet.java`, `web/member/ReservationCancelServlet.java` |
| 대여 시작·반납 | `web/member/RentalStartServlet.java`, `web/member/RentalReturnServlet.java`, `service/RentalService.java` |
| 노쇼 주기 처리 | `web/scheduler/NoShowScheduler.java` |
| 차량 위치·상태 변경 | `dao/CarDao.java` |
| 주차장 수량 변경 | `dao/ParkingLotDao.java` |
| 상태 값 | `domain/RentalStatus.java`, `domain/CarStatus.java` |

## 기능 4. 운영자 자원 관리

### 4.1 기능의 목적

운영자는 차량과 주차장을 단순히 수정하는 것이 아니라, **현재 예약·대여·주차 수량과 모순되지 않는 범위에서** 자원을 관리한다. 관리 화면은 차량·주차장 목록을 보여 주고, 등록·수정·상태 변경·차량 이동 요청은 Service의 업무 규칙을 통과한 뒤에만 저장된다.

관리 업무 흐름도: [admin-resource-management-workflow.html](.archify/workflow-admin-resource-management-20261015-150000/admin-resource-management-workflow.html)

### 4.2 관리 업무 흐름

1. 운영자가 차량 또는 주차장 관리 목록에서 작업을 선택한다.
2. 각 관리 Servlet은 요청 값을 숫자·enum으로 파싱하고, 해당 Service를 호출한다.
3. `CarService` 또는 `ParkingLotService`는 현재 차량 상태, 활성 예약·대여, 주차장 운영 상태와 수량을 확인한다.
4. 여러 행의 값이 함께 바뀌는 작업은 JDBC 트랜잭션으로 커밋한다. 규칙 위반이나 중간 실패가 있으면 롤백하고 기존 값을 유지한다.
5. Servlet은 성공 메시지와 함께 관리 목록으로 돌려보내거나, 입력값과 오류 메시지를 폼에 다시 표시한다.

차량 등록에 포함되는 이미지 파일 저장과 대표 이미지 교체는 파일 시스템이라는 별도 저장소를 다루므로 [기능 5. 차량 이미지 업로드](#기능-5-차량-이미지-업로드)에서 분리해 설명한다.

### 4.3 차량 관리 규칙

#### 차량 등록

차량은 운영 중인 주차장에만 등록할 수 있고, `max_parking_count - current_parking_count - rented_car_count`가 1 이상이어야 한다. 조건을 통과하면 차량 행, 선택한 이미지 메타데이터, 주차장 현재 차량 수 증가를 하나의 트랜잭션으로 저장한다.

#### 차량 상태·요금·주차장 이동

* 상태 변경은 `AVAILABLE → MAINTENANCE·INACTIVE`, `MAINTENANCE → AVAILABLE·INACTIVE`, `INACTIVE → AVAILABLE`만 허용한다. `RENTED`는 대여 시작·반납에서 자동 변경되므로 운영자가 직접 바꿀 수 없다.
* 진행 중인 `RESERVED` 또는 `IN_PROGRESS` 대여가 있으면 차량 상태와 현재 주차장을 변경할 수 없다.
* 요금은 **1원 이상**일 때만 변경한다.
* 차량 이동은 대여 중인 차량이 아니고, 대상 주차장이 `ACTIVE`이며 빈자리가 있을 때만 가능하다. 기존 주차장 수량 감소, 대상 주차장 수량 증가, 차량의 현재 주차장·주소 변경을 한 트랜잭션으로 처리한다.

### 4.4 주차장 관리 규칙

주차장 등록·수정은 이름, 주소, 최대 수용 대수가 필수이며 같은 이름은 허용하지 않는다.

최대 수용 대수는 현재 주차 차량과 대여 중 차량의 합계보다 작게 수정할 수 없다. 또한 주차장 비활성화는 현재 주차 차량과 대여 중 차량이 모두 **0대**일 때만 가능하다. 운영을 중단한 주차장에 차량이나 대여 중 차량을 남기는 상황을 막기 위한 규칙이다.

### 4.5 사용 기술과 역할

| 기술 | 이 기능에서의 역할 |
| --- | --- |
| Jakarta Servlet·JSP | 관리 목록과 폼을 제공하고, POST 요청의 파싱·성공 redirect·오류 화면 전달을 담당한다. |
| Service 계층 | 차량 상태 전이, 주차장 정원, 활성 예약·대여 여부 같은 업무 규칙을 검사한다. |
| JDBC Transaction·`FOR UPDATE` | 차량 이동, 상태 변경, 주차장 수정처럼 여러 값이 관련된 변경을 커밋 또는 롤백 단위로 묶는다. |
| MySQL | 차량·주차장·이미지 메타데이터와 주차장 수량을 영속화한다. |
| enum | `CarStatus`, `ParkingLotStatus`로 허용된 상태 값을 제한한다. |

### 4.6 구현에서 생각해 볼 설계 사항

#### 수량과 위치를 함께 바꾸는 이유

차량 이동에서 차량의 `current_parking_lot_id`만 바꾸면 기존·대상 주차장의 수량이 실제 위치와 어긋난다. 현재 구현은 두 주차장을 ID 오름차순으로 잠근 뒤 수량 두 개와 차량 위치를 같은 트랜잭션에서 변경한다. 잠금 순서를 고정해 반대 방향 이동 요청 사이의 교착 상태 가능성도 낮춘다.

#### 관리 권한과 업무 규칙의 분리

`AdminAuthenticationFilter`는 운영자만 관리 URL에 들어오게 하는 웹 계층의 권한 처리다. 반면 “예약이 있으면 상태를 못 바꾼다”와 “비활성 주차장은 차량이 0대여야 한다”는 Service가 책임지는 업무 규칙이다. URL을 직접 호출하더라도 Service 검증을 우회할 수 없도록 역할을 나눴다.

#### 향후 검토 사항

현재 차량 요금 변경은 단일 `UPDATE`이므로 별도 트랜잭션을 열지 않는다. 요금 변경 이력, 적용 시작 시각, 예약 당시 요금 보존이 필요해지면 요금 이력 모델을 별도로 설계해야 한다.

### 4.7 주요 구현 파일

| 책임 | 파일 |
| --- | --- |
| 차량·주차장 관리 목록과 대시보드 | `web/admin/AdminCarListServlet.java`, `web/admin/ParkingLotListServlet.java`, `web/admin/AdminDashboardServlet.java` |
| 차량 등록·상태·요금·주차장 이동 | `web/admin/CarRegistrationServlet.java`, `web/admin/CarStatusChangeServlet.java`, `web/admin/CarPriceChangeServlet.java`, `web/admin/CarParkingLotChangeServlet.java` |
| 주차장 등록·수정·상태 변경 | `web/admin/ParkingLotRegistrationServlet.java`, `web/admin/ParkingLotEditServlet.java`, `web/admin/ParkingLotStatusChangeServlet.java` |
| 차량 업무 규칙과 트랜잭션 | `service/CarService.java` |
| 주차장 업무 규칙과 트랜잭션 | `service/ParkingLotService.java` |
| 데이터 변경 | `dao/CarDao.java`, `dao/ParkingLotDao.java`, `dao/RentalDao.java` |

## 기능 5. 차량 이미지 업로드

### 5.1 설계의 핵심

이미지 파일 바이트와 DB 메타데이터를 분리했다. 현재 파일은 로컬에 저장하고, MySQL `car_image`에는 `storage_key`, 원본 파일명, 콘텐츠 형식, 크기만 저장한다.

* 데이터 흐름도: [car-image-upload-dataflow.html](.archify/dataflow-car-image-upload-20261015-141500/car-image-upload-dataflow.html)
* 시퀀스 다이어그램: [car-image-upload-sequence.html](.archify/sequence-car-image-upload-20261015-141500/car-image-upload-sequence.html)

### 5.2 업로드 검증과 저장

`CarImageValidator`는 파일 **5MB 이하**, 실제 **JPEG·PNG**, 가로·세로 각각 **4096px 이하**를 확인한다. 형식만 담긴 HTTP 헤더를 신뢰하지 않고 `ImageIO`로 실제 이미지를 읽어 검사한다.

검증 뒤 `CarImageStorage.save()`가 UUID 파일명으로 이미지를 저장하고 `StoredCarImage`를 돌려준다. 이 객체가 DB에 기록할 저장 키와 파일 메타데이터를 전달한다.

### 5.3 등록·교체와 실패 처리

신규 차량 등록은 파일을 먼저 저장한 뒤 차량·이미지 메타데이터·주차장 수량을 하나의 JDBC 트랜잭션으로 저장한다. DB 처리에 실패하면 Servlet이 새 파일 삭제를 시도한다.

대표 이미지 교체도 새 파일을 먼저 저장한다. `CarImageService`가 차량과 기존 대표 이미지 행을 잠근 뒤 메타데이터를 커밋하고, 커밋 성공 뒤 이전 파일을 삭제한다. 이전 파일 삭제 실패는 이미 새 DB 참조가 저장된 성공 요청을 실패로 바꾸지 않으므로, 이전 파일이 남을 수 있어 운영 점검 대상이다.

### 5.4 저장소 교체를 위한 경계

Servlet과 Service는 `CarImageStorage`의 `save`, `open`, `delete` 계약을 통해서만 파일 저장소를 사용한다. 현재 Factory는 `local` 구현체인 `LocalCarImageStorage`만 만들며, Object Storage는 아직 구현하지 않았다.

배포 시 Object Storage 구현체를 추가하면 업로드 업무 흐름과 `car_image` 메타데이터 구조는 유지한 채 저장 방식만 교체할 수 있다.

### 5.5 주요 구현 파일

| 책임 | 파일 |
| --- | --- |
| 신규 차량 등록과 업로드 실패 정리 | `web/admin/CarRegistrationServlet.java` |
| 대표 이미지 교체와 이전 파일 정리 | `web/admin/CarImageReplaceServlet.java` |
| 이미지 검증 | `web/image/CarImageValidator.java` |
| 저장소 추상화·로컬 구현·선택 | `storage/CarImageStorage.java`, `storage/LocalCarImageStorage.java`, `storage/CarImageStorageFactory.java` |
| DB 메타데이터 저장·교체 | `service/CarImageService.java`, `dao/CarImageDao.java` |
