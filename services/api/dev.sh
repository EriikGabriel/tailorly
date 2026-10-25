#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -f .env.garage ]]; then
  echo "Garage credentials are missing. Run bash services/api/init-garage-env.sh from the repository root." >&2
  exit 1
fi

source .env.garage
garage_access_key="${TAILORLY_STORAGE_ACCESS_KEY:-}"
garage_secret_key="${TAILORLY_STORAGE_SECRET_KEY:-}"

if [[ -f .env.local ]]; then
  set -a
  source .env.local
  set +a
fi

TAILORLY_STORAGE_ACCESS_KEY="${TAILORLY_STORAGE_ACCESS_KEY:-$garage_access_key}"
TAILORLY_STORAGE_SECRET_KEY="${TAILORLY_STORAGE_SECRET_KEY:-$garage_secret_key}"

if [[ -z "${TAILORLY_STORAGE_ACCESS_KEY:-}" || -z "${TAILORLY_STORAGE_SECRET_KEY:-}" ]]; then
  echo "TAILORLY_STORAGE_ACCESS_KEY and TAILORLY_STORAGE_SECRET_KEY must be set in .env.garage." >&2
  exit 1
fi

export TAILORLY_STORAGE_ACCESS_KEY TAILORLY_STORAGE_SECRET_KEY
exec ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
