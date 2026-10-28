# GCS 차량 이미지 저장소 전환

차량 이미지는 기본적으로 Docker 볼륨에 저장된다. GCS 전환 뒤에는 MySQL의 `storage_key`는 그대로 두고, 실제 파일만 비공개 GCS 버킷에 저장한다.

```text
관리자 업로드 → app 컨테이너 → private GCS 버킷
사용자 조회 → /images/cars/{id} → app 컨테이너 → private GCS 버킷
```

버킷을 공개하지 않는다. 애플리케이션이 기존 이미지 조회 URL을 통해 GCS 객체를 읽어 응답한다.

## 1. GCP 버킷과 IAM 설정

1. Cloud Storage에서 VM과 같은 리전의 Standard 버킷을 만든다.
2. Uniform bucket-level access와 Public access prevention을 활성화한다.
3. 버킷의 **권한** 탭에서 다음 주 구성원에게 `Storage Object User`를 부여한다.

```text
car-rental-vm@<PROJECT_ID>.iam.gserviceaccount.com
```

이 역할은 객체 생성·조회·삭제 권한을 제공한다. 프로젝트 전체가 아닌 이 버킷에만 부여한다.

서비스 계정 키 JSON, 액세스 키, 공개 `allUsers` 권한은 만들지 않는다. VM에 연결된 `car-rental-vm` 서비스 계정의 Application Default Credentials를 사용한다.

## 2. 기존 이미지 확인

전환 전에 `car_image` 테이블과 Docker `car_uploads` 볼륨에 이미지가 있는지 확인한다.

- 이미지가 없다면 다음 단계로 바로 진행한다.
- 이미지가 있다면 DB `storage_key`와 동일한 키(`cars/<UUID>.jpg|png`)로 GCS에 복사한 뒤 전환한다.

기존 Docker 볼륨은 전환 직후 삭제하지 않는다. 문제가 있으면 `IMAGE_STORAGE_TYPE=local`로 되돌릴 수 있다.

## 3. VM 환경 변수 전환

GCP VM의 `/home/estatehrd43/car_rental/.env`에 아래 값을 설정한다.

```dotenv
IMAGE_STORAGE_TYPE=gcs
GCS_BUCKET=<PRIVATE_BUCKET_NAME>
```

`GCS_BUCKET`은 비밀값이 아니지만, `.env`는 Git에 커밋하지 않는다.

## 4. 앱 컨테이너 재생성

`main` 병합으로 새 앱 이미지가 GHCR에 배포된 뒤, VM에서 앱 컨테이너만 재생성한다.

```bash
sudo -u estatehrd43 -H docker compose --project-directory /home/estatehrd43/car_rental --env-file /home/estatehrd43/car_rental/.env --env-file /home/estatehrd43/car_rental/.deploy.env up -d --no-build --force-recreate app
```

MySQL, Nginx, 인증서, Docker 볼륨은 이 명령으로 바뀌지 않는다.

## 5. 전환 확인과 롤백

1. 관리자 화면에서 JPEG 또는 PNG 차량 이미지를 등록한다.
2. 차량 목록과 상세 화면에서 이미지가 보이는지 확인한다.
3. 이미지를 교체하고 이전 객체가 삭제되는지 확인한다.
4. GCS 콘솔에서 객체 키가 `cars/`로 시작하는지 확인한다.

문제가 있으면 `.env`를 아래처럼 되돌리고 app 컨테이너를 다시 재생성한다.

```dotenv
IMAGE_STORAGE_TYPE=local
```

GCS 객체와 기존 Docker 볼륨은 롤백을 위해 즉시 삭제하지 않는다.
