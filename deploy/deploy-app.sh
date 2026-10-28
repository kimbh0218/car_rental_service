#!/usr/bin/env bash

# Runs on the deployment VM. It deliberately replaces only the app service:
# MySQL, Nginx, and their persistent volumes stay running.
set -Eeuo pipefail

PROJECT_DIR="${PROJECT_DIR:-$HOME/car_rental}"
# The deployment VM authenticates to this private package once through deploy/login-ghcr.sh.
IMAGE_REPOSITORY="${IMAGE_REPOSITORY:-ghcr.io/kimbh0218/car_rental_service}"
IMAGE_TAG="${1:-}"
DEPLOY_ENV="$PROJECT_DIR/.deploy.env"
LAST_SUCCESS_ENV="$PROJECT_DIR/.deploy.last-success.env"
PREVIOUS_ENV="$PROJECT_DIR/.deploy.previous.env"

if [[ ! "$IMAGE_TAG" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Expected one 40-character lowercase Git commit SHA." >&2
  exit 64
fi

if [[ ! -d "$PROJECT_DIR" || ! -f "$PROJECT_DIR/compose.yaml" || ! -f "$PROJECT_DIR/.env" ]]; then
  echo "Expected compose.yaml and .env in $PROJECT_DIR." >&2
  exit 66
fi

cd "$PROJECT_DIR"

compose=(docker compose --project-directory "$PROJECT_DIR" --env-file "$PROJECT_DIR/.env" --env-file "$DEPLOY_ENV")
image="$IMAGE_REPOSITORY:$IMAGE_TAG"

echo "Pulling $image"
docker pull "$image"

temporary_env="$(mktemp "$PROJECT_DIR/.deploy.env.XXXXXX")"
trap 'rm -f "$temporary_env"' EXIT
chmod 600 "$temporary_env"
printf 'APP_IMAGE=%s\nAPP_IMAGE_TAG=%s\n' "$IMAGE_REPOSITORY" "$IMAGE_TAG" > "$temporary_env"
mv "$temporary_env" "$DEPLOY_ENV"
trap - EXIT

# Validate interpolation before replacing the currently running app container.
"${compose[@]}" config --quiet
"${compose[@]}" up -d --no-deps --no-build app

container_id="$("${compose[@]}" ps -q app)"
if [[ -z "$container_id" ]]; then
  echo "The app container was not created." >&2
  exit 1
fi

for attempt in {1..36}; do
  health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id")"

  if [[ "$health" == "healthy" ]]; then
    if ! curl --fail --silent --show-error http://127.0.0.1:8080/health | grep -qx 'OK'; then
      echo "The app container is healthy, but /health did not return OK." >&2
      exit 1
    fi

    if [[ -f "$LAST_SUCCESS_ENV" ]]; then
      cp "$LAST_SUCCESS_ENV" "$PREVIOUS_ENV"
    fi
    cp "$DEPLOY_ENV" "$LAST_SUCCESS_ENV"
    echo "Deployment completed with $image"
    exit 0
  fi

  if [[ "$health" == "unhealthy" || "$health" == "exited" || "$health" == "dead" ]]; then
    echo "The app container became $health." >&2
    "${compose[@]}" logs --tail=200 app >&2 || true
    exit 1
  fi

  echo "Waiting for app health ($attempt/36): $health"
  sleep 5
done

echo "Timed out waiting for the app container to become healthy." >&2
"${compose[@]}" logs --tail=200 app >&2 || true
exit 1
