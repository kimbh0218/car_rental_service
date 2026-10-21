# Docker 배포 순서

이 구성은 Tomcat 앱과 MySQL을 Docker 내부 네트워크에 두고, Nginx만 인터넷에 공개한다.

## 1. 서버 준비

Docker와 Docker Compose를 설치한다. 방화벽은 `80/tcp`, `443/tcp`만 열고, `8080`과 `3306`은 열지 않는다.

```bash
cp .env.example .env
```

`.env`의 `MYSQL_PASSWORD`와 `MYSQL_ROOT_PASSWORD`를 서로 다른 긴 무작위 값으로 바꾼다. `.env`는 Git에 포함하지 않는다.

## 2. DB와 앱을 내부에서 실행

```bash
docker compose up --build -d db app
docker compose ps
curl --fail http://127.0.0.1:8080/health
```

`/health`는 Tomcat뿐 아니라 MySQL 연결까지 확인한다. 앱 포트는 `127.0.0.1`에만 연결되므로 인터넷에서 직접 접근할 수 없다.

`sql/000_initial_schema.sql`은 새 `mysql_data` 볼륨을 처음 만들 때만 실행된다. 이 파일은 빈 스키마만 만들며 관리자·회원·차량 데이터는 넣지 않는다. 기존 서비스를 옮기는 경우에는 먼저 실제 DB의 전체 백업을 별도로 복원해야 한다.

`sql/001_create_car_image.sql`은 `000_initial_schema.sql`에 이미 포함된 테이블을 다시 만들기 때문에 Docker 초기화 경로에 넣지 않는다.

## 3. Nginx HTTP와 인증서 발급

도메인의 A 또는 AAAA 레코드를 서버 공인 IP로 연결한다. `.env`에서 `APP_DOMAIN`을 실제 도메인으로 바꾸고, `NGINX_CONFIG=http.conf`를 유지한다.

```bash
docker compose --profile public up -d nginx
```

아래에서 `example.com`과 이메일을 실제 값으로 바꿔 한 번 실행한다.

```bash
docker compose --profile certbot run --rm certbot certonly --webroot --webroot-path /var/www/certbot --domain example.com --email admin@example.com --agree-tos --no-eff-email
```

발급이 끝나면 `.env`의 `NGINX_CONFIG`를 `https.conf`로 바꾸고 Nginx를 다시 만든다.

```bash
docker compose --profile public up -d --force-recreate nginx
```

## 4. 공개 전 확인

실제 도메인에서 HTTPS 접속, 회원 로그인, 예약, 차량 이미지 업로드를 점검한다. 이어서 `docker compose restart` 후에도 DB 데이터와 업로드 이미지가 남는지 확인한다.

## 5. 배포 후 스모크 테스트

새 이미지를 EC2에 배포한 뒤, 외부 사용자가 접근하는 공개 URL로 `/health`를 확인한다.

```bash
APP_BASE_URL=http://<EC2_PUBLIC_IP> ./deploy/smoke-test.sh
```

HTTPS를 적용한 뒤에는 `APP_BASE_URL=https://<DOMAIN>`으로 실행한다. 이 스크립트는 HTTP 상태가 `200`이고 본문이 정확히 `OK`일 때만 성공한다. 앱 시작과 MySQL 준비를 기다릴 수 있도록 기본 12회, 5초 간격으로 재시도한다.

`127.0.0.1:8080` 대신 공개 URL을 넣어야 Nginx를 포함한 외부 접속 경로까지 검증한다. GitHub Actions의 **Deployment smoke test** 워크플로도 같은 스크립트를 실행하며, Actions 탭에서 **Run workflow**를 눌러 공개 URL을 입력하면 된다.

## 6. 운영

MySQL과 업로드 파일은 Docker 볼륨에 보관된다. DB 백업과 복구 테스트를 정기적으로 수행한다. 인증서 갱신은 만료 전에 아래를 실행하고 Nginx를 다시 읽힌다.

```bash
docker compose --profile certbot run --rm certbot renew
docker compose exec nginx nginx -s reload
```

현재 앱은 Tomcat 메모리 세션과 앱 내부 스케줄러를 사용한다. 따라서 첫 운영 버전은 `app` 서비스를 한 개만 실행해야 한다.
