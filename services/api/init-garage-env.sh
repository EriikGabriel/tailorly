#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

umask 077
if [[ ! -e .env.garage ]]; then
  access_key="GK$(openssl rand -hex 16)"
  secret_key="$(openssl rand -hex 32)"
  rpc_secret="$(openssl rand -hex 32)"

  cat > .env.garage <<EOF
GARAGE_RPC_SECRET=$rpc_secret
GARAGE_DEFAULT_ACCESS_KEY=$access_key
GARAGE_DEFAULT_SECRET_KEY=$secret_key
GARAGE_DEFAULT_BUCKET=tailorly-private
TAILORLY_STORAGE_ACCESS_KEY=$access_key
TAILORLY_STORAGE_SECRET_KEY=$secret_key
EOF
  echo 'Created .env.garage with local Garage and API credentials'
fi

if ! grep -q '^GARAGE_ADMIN_TOKEN=' .env.garage; then
  admin_token="$(openssl rand -base64 32)"
  printf 'GARAGE_ADMIN_TOKEN=%s\nGARAGE_UI_GARAGE_ADMIN_TOKEN=%s\n' \
    "$admin_token" "$admin_token" >> .env.garage
  echo 'Added Garage UI admin token to .env.garage'
elif ! grep -q '^GARAGE_UI_GARAGE_ADMIN_TOKEN=' .env.garage; then
  admin_token="$(sed -n 's/^GARAGE_ADMIN_TOKEN=//p' .env.garage | head -n 1)"
  if [[ -z "$admin_token" ]]; then
    echo 'GARAGE_ADMIN_TOKEN is empty in .env.garage' >&2
    exit 1
  fi
  printf 'GARAGE_UI_GARAGE_ADMIN_TOKEN=%s\n' "$admin_token" >> .env.garage
  echo 'Added Garage UI token mapping to .env.garage'
fi

chmod 600 .env.garage
