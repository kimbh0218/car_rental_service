# EC2 Docker 웹 요청 흐름

이 문서는 현재 배포된 서비스에서 사용자가 `http://<EC2 퍼블릭 IPv4>`로 접속했을 때, 요청이 화면으로 돌아올 때까지의 경로를 설명한다.

## 한눈에 보는 구조

```text
브라우저
  │  HTTP 요청: http://<EC2 퍼블릭 IPv4>
  ▼
AWS 인터넷 구간
  ▼
EC2 보안 그룹: TCP 80 허용
  ▼
EC2 인스턴스
  ▼
Docker의 Nginx 컨테이너: 80
  │  리버스 프록시
  ▼
Docker 내부 네트워크
  ▼
Tomcat app 컨테이너: 8080
  │  JDBC
  ▼
MySQL db 컨테이너: 3306
```

## 1. 브라우저가 EC2를 찾는다

사용자는 `http://<EC2 퍼블릭 IPv4>` 주소로 접속한다.

퍼블릭 IPv4는 인터넷에서 EC2 인스턴스를 찾기 위한 공개 주소다. 이 주소 자체가 웹사이트를 실행하는 것은 아니며, 브라우저의 HTTP 요청을 EC2까지 전달하는 역할을 한다.

예를 들어 주소에 포트를 따로 쓰지 않은 `http://<EC2 퍼블릭 IPv4>`는 기본 HTTP 포트인 `80`으로 요청을 보낸다.

## 2. AWS 보안 그룹이 80번 포트를 확인한다

요청이 EC2에 도착하기 전에 보안 그룹이 외부 TCP 80번 접근을 허용하는지 판단한다.

현재 웹사이트가 외부에서 열리는 것은 보안 그룹이 HTTP 80번 포트를 허용하고 있다는 뜻이다. 반면 Tomcat 8080번과 MySQL 3306번은 외부에 열지 않는다.

## 3. EC2의 Docker가 Nginx 컨테이너로 전달한다

EC2 운영체제의 80번 포트는 Docker의 Nginx 컨테이너 80번 포트와 연결되어 있다.

따라서 외부 사용자의 첫 요청은 Nginx 컨테이너가 받는다. Nginx는 서비스의 공개 입구다.

## 4. Nginx가 Tomcat 앱으로 요청을 전달한다

Nginx는 화면과 업무 로직을 직접 처리하지 않는다. 받은 요청을 Docker 내부 네트워크의 `app:8080`으로 전달하는 리버스 프록시 역할을 한다.

`app`은 Docker Compose 서비스 이름이다. Docker 내부에서는 이 이름으로 Tomcat 앱 컨테이너를 찾을 수 있다.

외부 사용자는 Nginx에만 접속한다. Tomcat의 내부 주소나 8080번 포트를 알 필요가 없고, 직접 접근할 수도 없다.

## 5. Tomcat이 프로젝트를 실행한다

`app` 컨테이너에는 Dockerfile로 만든 WAR 파일과 Tomcat이 들어 있다. Tomcat은 WAR 안의 Servlet, JSP, Filter 코드를 실행한다.

예를 들어 차량 목록, 회원가입, 예약 생성 요청은 Tomcat이 받아 Service와 DAO를 호출하고, 최종 HTML 또는 리다이렉트 응답을 만든다.

## 6. 앱이 MySQL에만 내부적으로 연결한다

회원, 차량, 주차장, 예약 데이터를 읽거나 저장해야 할 때 app 컨테이너는 Docker 내부 네트워크의 `db:3306`으로 JDBC 연결을 만든다.

`db`도 Docker Compose 서비스 이름이다. MySQL은 외부 포트를 공개하지 않았으므로 브라우저가 DB에 직접 접속할 수 없다. 데이터는 Docker 볼륨에 보관되어 컨테이너를 다시 만들어도 유지된다.

## 7. 응답은 반대 경로로 사용자에게 돌아간다

MySQL 조회 또는 저장 결과를 받은 Tomcat은 HTML 응답을 만들고, 이를 Nginx에 반환한다. Nginx는 다시 EC2 퍼블릭 IPv4를 통해 사용자 브라우저로 응답을 보낸다.

```text
MySQL 결과
  → Tomcat이 업무 처리·HTML 생성
  → Nginx
  → EC2 퍼블릭 IPv4
  → 브라우저 화면
```

## 공개 포트와 내부 포트

| 대상 | 포트 | 외부 공개 여부 | 역할 |
| --- | ---: | --- | --- |
| Nginx | 80 | 공개 | 사용자 요청을 받는 입구 |
| Tomcat app | 8080 | 비공개 | Java 웹 프로젝트 실행 |
| MySQL db | 3306 | 비공개 | 서비스 데이터 저장 |

이 구분 덕분에 사용자는 Nginx만 거쳐 앱에 도달하며, Tomcat과 MySQL은 외부 인터넷에 직접 노출되지 않는다.

## `/health` 요청의 의미

`http://<EC2 퍼블릭 IPv4>/health` 요청도 같은 경로를 거친다.

```text
브라우저 또는 스모크 테스트
  → Nginx
  → Tomcat의 HealthServlet
  → MySQL 연결 확인
  → OK 또는 503 응답
```

본문이 정확히 `OK`이면 외부 접속, Nginx, Tomcat, MySQL 연결이 모두 동작하는 상태다. MySQL 연결이 실패하면 HealthServlet은 `503 Service Unavailable`을 반환한다.

## HTTPS를 도입하면 달라지는 부분

현재는 HTTP이므로 브라우저와 Nginx 사이 통신이 암호화되지 않는다.

HTTPS를 도입하면 공개 입구인 Nginx가 인증서와 443번 포트를 맡는다. 이후 Nginx → Tomcat → MySQL의 내부 흐름은 현재와 동일하다.

```text
브라우저 -- HTTPS:443, 암호화 --> Nginx -- 내부 HTTP --> Tomcat --> MySQL
```
