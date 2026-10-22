#!/usr/bin/env bash

# Run manually on EC2 once after creating a GitHub Classic PAT with only
# read:packages. The token is read from the terminal and is never written to
# the repository or passed to GitHub Actions.
set -Eeuo pipefail

GITHUB_USERNAME="${1:-kimbh0218}"

read -r -s -p "GHCR read-only token: " ghcr_token
echo

if [[ -z "$ghcr_token" ]]; then
  echo "A token is required." >&2
  exit 64
fi

umask 077
printf '%s' "$ghcr_token" | docker login ghcr.io --username "$GITHUB_USERNAME" --password-stdin
unset ghcr_token

echo "GHCR login completed for $GITHUB_USERNAME."
