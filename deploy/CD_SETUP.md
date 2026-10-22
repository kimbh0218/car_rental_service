# 비공개 GHCR 이미지 기반 승인 배포 설정

이 문서는 코드 변경 후, `main`의 CI가 성공하면 GitHub 승인 뒤 EC2의 `app` 컨테이너만 교체하는 초기 CD 설정이다.

```text
main push
  → CI 테스트와 GHCR 이미지 업로드
  → GitHub production 승인 대기
  → AWS SSM이 EC2에서 deploy-app.sh 실행
  → app 컨테이너 교체
  → 공개 /health 스모크 테스트
```

## 1. 이 저장소의 구성

`compose.yaml`의 `app` 서비스는 두 모드로 실행된다.

| 위치 | 이미지 | 실행 방식 |
| --- | --- | --- |
| 로컬 개발 | `car-rental-app:local` | `docker compose up --build` |
| EC2 배포 | `ghcr.io/kimbh0218/car_rental_service:<commit-sha>` | `deploy/deploy-app.sh <commit-sha>` |

EC2 배포 스크립트는 `db`, `nginx`, Docker 볼륨을 중지하거나 삭제하지 않는다. 실패 시 `.deploy.last-success.env`에 기록된 마지막 성공 SHA를 확인해 수동으로 재배포한다.

## 2. GHCR 패키지를 비공개로 유지하고 EC2에 읽기 권한 부여

GHCR 패키지는 **Private** 상태를 유지한다. GitHub에서 **프로필 → Packages → car_rental_service → Package settings**로 이동해 visibility가 Private인지 확인한다.

다음으로 GitHub에서 **Settings → Developer settings → Personal access tokens → Tokens (classic) → Generate new token (classic)**으로 이동한다. 만료일을 정하고, Packages 항목의 `read:packages`만 선택해 토큰을 만든다.

토큰은 생성 직후 한 번만 보이므로 안전한 곳에 잠시 보관한다. GitHub Actions Secret, Git 저장소, `.env`, 채팅에 저장하거나 붙여 넣지 않는다.

EC2에 SSH로 접속한 뒤, 아래를 한 번 실행한다.

```bash
cd /home/ubuntu/car_rental
chmod +x deploy/login-ghcr.sh deploy/deploy-app.sh
./deploy/login-ghcr.sh kimbh0218
```

화면에 토큰 입력을 요청하면 방금 만든 값을 붙여 넣고 Enter를 누른다. 이 명령은 토큰을 EC2의 `ubuntu` 사용자 Docker 로그인 정보에만 저장한다. 이후 SSM 배포는 로그인된 계정으로 비공개 이미지를 pull한다.

이미지에는 여전히 `.env`, DB 덤프, 사용자 업로드 파일, API 키를 넣지 않는다.

## 3. EC2 초기 반영

이 저장소 변경을 처음으로 EC2의 `/home/ubuntu/car_rental`에도 복사해야 한다. 이후 일반 Java 코드 변경은 GHCR 이미지로 자동 반영된다.

EC2에서 아래를 확인한다.

```bash
cd /home/ubuntu/car_rental
chmod +x deploy/deploy-app.sh
docker compose config --quiet
```

GHCR 로그인 뒤 아래 명령이 성공하는지 확인한다.

```bash
docker pull ghcr.io/kimbh0218/car_rental_service:<이미지_SHA>
```

## 4. EC2를 SSM 관리 대상으로 만들기

1. EC2 인스턴스에 태그 `Project=car-rental`을 추가한다.
2. IAM 역할 `CarRentalEc2SsmRole`을 만들고 AWS 관리 정책 `AmazonSSMManagedInstanceCore`을 붙인다.
3. EC2 인스턴스에 이 역할을 연결한다.
4. AWS Systems Manager의 **Managed nodes**에서 인스턴스가 `Online`인지 확인한다.

SSM Agent는 Run Command를 root 권한으로 실행한다. 워크플로는 명령을 `ubuntu` 사용자로 전환해 실행하므로 `/home/ubuntu/car_rental`의 파일 소유권이 바뀌지 않는다.

## 5. GitHub Actions용 AWS 역할 만들기

1. AWS IAM에 GitHub OIDC identity provider를 추가한다.
   - Provider URL: `https://token.actions.githubusercontent.com`
   - Audience: `sts.amazonaws.com`
2. IAM 역할 `CarRentalGithubDeployRole`을 만든다.
3. 신뢰 정책에는 [github-oidc-trust-policy.json](aws/github-oidc-trust-policy.json)을 사용하고 `<AWS_ACCOUNT_ID>`를 실제 12자리 AWS 계정 ID로 바꾼다.
4. 권한 정책에는 [github-deploy-policy.json](aws/github-deploy-policy.json)을 사용하고 `<AWS_REGION>`, `<AWS_ACCOUNT_ID>`를 실제 값으로 바꾼다.

이 역할은 `Project=car-rental` 태그가 있는 EC2에 `AWS-RunShellScript` 명령을 보내고 결과를 읽는 권한만 가진다. AWS 액세스 키를 GitHub에 저장하지 않는다.

## 6. GitHub production 환경 만들기

GitHub 저장소에서 **Settings → Environments → New environment**로 이동해 `production`을 만든다.

1. **Required reviewers**에 본인 계정을 추가한다.
2. Environment variables에 다음 값을 추가한다.

| 이름 | 값 예시 |
| --- | --- |
| `AWS_REGION` | `ap-northeast-2` |
| `AWS_ROLE_TO_ASSUME` | `arn:aws:iam::<AWS_ACCOUNT_ID>:role/CarRentalGithubDeployRole` |
| `EC2_INSTANCE_ID` | `i-0123456789abcdef0` |
| `APP_BASE_URL` | `http://<EC2_PUBLIC_IPV4>` |

이 값들은 비밀값이 아니므로 Environment variables에 넣는다. GitHub 토큰이나 AWS access key는 추가하지 않는다.

## 7. 첫 승인 배포 확인

1. 이 변경을 `main`에 push한다.
2. **Actions → CI**에서 테스트와 GHCR 이미지 업로드가 성공했는지 확인한다.
3. 이어서 **Actions → Deploy to production**에서 `production` 승인을 요청한다.
4. **Review deployments → Approve and deploy**를 누른다.
5. 배포 job이 SSM 결과와 공개 `/health` 스모크 테스트를 모두 통과하는지 확인한다.

## 운영 주의사항

- 현재 EC2 퍼블릭 IPv4가 바뀌면 `APP_BASE_URL`도 GitHub environment variable에서 바꿔야 한다. Elastic IP나 도메인을 도입하면 이 수동 변경을 없앨 수 있다.
- GHCR 토큰이 만료되거나 폐기되면 다음 배포의 image pull이 실패한다. 새 `read:packages` 토큰을 만든 뒤 EC2에서 `./deploy/login-ghcr.sh kimbh0218`를 다시 실행한다.
- 배포 중에는 app 컨테이너를 재생성하므로 짧은 요청 실패가 발생할 수 있다. Nginx, DB는 계속 실행된다.
- EC2 디스크가 20GiB이므로 이미지가 많이 쌓이면 용량을 확인한다. 첫 배포 흐름이 안정된 뒤 현재·직전 두 이미지만 보존하는 정리 정책을 추가한다.
- `compose.yaml`, Nginx 설정, DB 스키마처럼 서버 구성을 바꾸는 변경은 이 1차 CD 범위에 포함하지 않는다. 이 자동화는 Java 앱 이미지 배포가 대상이다.
