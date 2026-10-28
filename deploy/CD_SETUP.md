# 비공개 GHCR 이미지 기반 GCP 승인 배포 설정

이 문서는 `main`의 CI가 성공한 뒤 GitHub 승인을 거쳐 Google Compute Engine VM의 `app` 컨테이너만 교체하는 CD 설정이다.

```text
main push
  → CI 테스트와 GHCR 이미지 업로드
  → GitHub production 승인 대기
  → GitHub OIDC와 Workload Identity Federation 인증
  → GCP VM에 OS Login SSH로 deploy-app.sh 실행
  → app 컨테이너 교체
  → 공개 HTTPS /health 스모크 테스트
```

## 1. 이 저장소의 구성

`compose.yaml`의 `app` 서비스는 두 모드로 실행된다.

| 위치 | 이미지 | 실행 방식 |
| --- | --- | --- |
| 로컬 개발 | `car-rental-app:local` | `docker compose up --build` |
| GCP 배포 | `ghcr.io/kimbh0218/car_rental_service:<commit-sha>` | `deploy/deploy-app.sh <commit-sha>` |

배포 스크립트는 `db`, `nginx`, Docker 볼륨을 중지하거나 삭제하지 않는다. 마지막 성공 SHA는 `.deploy.last-success.env`에 기록한다.

## 2. GHCR 읽기 권한을 VM에 한 번 설정

GHCR 패키지는 **Private** 상태로 유지한다. GitHub에서 Classic Personal Access Token을 만들고 `read:packages`만 부여한다. 토큰은 GitHub Secrets, 저장소, `.env`에 저장하지 않는다.

VM에서 Compose 프로젝트를 실행하는 Linux 사용자로 로그인한다.

```bash
sudo -u <APP_USER> -H /home/<APP_USER>/car_rental/deploy/login-ghcr.sh kimbh0218
sudo -u <APP_USER> -H docker pull ghcr.io/kimbh0218/car_rental_service:latest
```

토큰은 해당 Linux 사용자의 Docker 로그인 정보에만 저장된다. 토큰이 만료되거나 폐기되면 같은 명령으로 다시 로그인한다.

## 3. GCP 배포 권한 구성

서비스 계정은 목적별로 분리한다.

| 계정 | 역할 |
| --- | --- |
| `github-deployer` | GitHub Actions가 VM 배포 명령을 실행하는 계정 |
| `car-rental-vm` | VM 안의 애플리케이션이 나중에 GCS 같은 GCP 리소스에 접근하는 계정 |

`github-deployer`에는 프로젝트 수준에서 다음 역할이 필요하다.

- `Compute OS Admin Login`
- `Compute Viewer`

대상 VM의 맞춤 메타데이터에는 `enable-oslogin=TRUE`를 설정한다. GitHub Actions는 Workload Identity Federation으로 이 계정을 가장하므로, 서비스 계정 키 JSON이나 장기 GCP 액세스 키를 GitHub에 저장하지 않는다.

Workload Identity Provider는 GitHub OIDC 발급자 `https://token.actions.githubusercontent.com`를 사용하고, `repository`, `ref`, `environment` 속성을 매핑한다. 다음 조건으로 이 저장소의 `main`과 `production` 환경만 허용한다.

```text
assertion.repository == 'kimbh0218/car_rental_service' &&
assertion.ref == 'refs/heads/main' &&
assertion.environment == 'production'
```

## 4. GitHub production 환경 변수

저장소 **Settings → Environments → production → Variables**에 다음 값을 저장한다. 식별자이므로 Secrets가 아닌 Variables를 사용한다.

| 이름 | 값 |
| --- | --- |
| `GCP_PROJECT_ID` | GCP 프로젝트 ID |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | `projects/<PROJECT_NUMBER>/locations/global/workloadIdentityPools/<POOL_ID>/providers/<PROVIDER_ID>` |
| `GCP_DEPLOY_SERVICE_ACCOUNT` | `github-deployer@<PROJECT_ID>.iam.gserviceaccount.com` |
| `GCP_INSTANCE_NAME` | 대상 Compute Engine VM 이름 |
| `GCP_APP_USER` | `/home/<APP_USER>/car_rental`을 소유하고 Docker를 실행할 Linux 사용자 |
| `APP_BASE_URL` | `https://<PUBLIC_DOMAIN>` |

`production` 환경의 Required reviewers를 설정하면 CI 성공 뒤 승인할 때까지 VM 배포가 시작되지 않는다.

## 5. 첫 승인 배포 확인

1. 이 변경을 `main`에 병합한다.
2. **Actions → CI**에서 테스트와 GHCR 이미지 업로드가 성공했는지 확인한다.
3. **Actions → Deploy to production**에서 `production` 승인을 요청한다.
4. **Review deployments → Approve and deploy**를 누른다.
5. VM 배포 명령과 공개 HTTPS `/health` 스모크 테스트가 모두 통과하는지 확인한다.

## 운영 주의사항

- 배포는 `app` 컨테이너만 재생성하므로 짧은 요청 실패가 발생할 수 있다. Nginx와 MySQL은 계속 실행된다.
- HTTPS Nginx는 80 포트 요청을 443 포트로 리다이렉트한다. Nginx 컨테이너 상태 검사는 private 네트워크의 `app:8080/health`를 검사한다.
- 도메인을 `APP_BASE_URL`로 사용하므로 VM의 외부 IP가 바뀌어도 DuckDNS 레코드만 갱신하면 GitHub 설정을 바꿀 필요가 없다. 현재는 고정 IP를 사용한다.
- VM 런타임 계정에 GCS 권한을 추가할 때는 필요한 버킷의 최소 권한만 부여한다.
