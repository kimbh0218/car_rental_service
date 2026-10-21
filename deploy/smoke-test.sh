#!/usr/bin/env bash
set -euo pipefail

app_base_url="${APP_BASE_URL:-${1:-}}"
max_attempts="${SMOKE_MAX_ATTEMPTS:-12}"
retry_delay_seconds="${SMOKE_RETRY_DELAY_SECONDS:-5}"

if [[ -z "$app_base_url" ]]; then
    echo "Usage: APP_BASE_URL=https://example.com ./deploy/smoke-test.sh" >&2
    exit 2
fi

if ! [[ "$max_attempts" =~ ^[1-9][0-9]*$ ]]; then
    echo "SMOKE_MAX_ATTEMPTS must be a positive integer." >&2
    exit 2
fi

if ! [[ "$retry_delay_seconds" =~ ^[0-9]+$ ]]; then
    echo "SMOKE_RETRY_DELAY_SECONDS must be zero or a positive integer." >&2
    exit 2
fi

health_url="${app_base_url%/}/health"

for ((attempt = 1; attempt <= max_attempts; attempt++)); do
    if response_body="$(curl --fail --silent --show-error --connect-timeout 5 --max-time 10 "$health_url")"; then
        if [[ "$response_body" == "OK" ]]; then
            echo "Smoke test passed: $health_url returned OK"
            exit 0
        fi

        echo "Attempt $attempt/$max_attempts returned an unexpected body: $response_body" >&2
    else
        echo "Attempt $attempt/$max_attempts could not get a successful response from $health_url" >&2
    fi

    if (( attempt < max_attempts )); then
        sleep "$retry_delay_seconds"
    fi
done

echo "Smoke test failed: $health_url did not return HTTP 200 with body OK." >&2
exit 1
