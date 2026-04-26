#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

if [ ! -f "$BASE_DIR/.env" ] && [ -f "$BASE_DIR/.env.example" ]; then
  cp "$BASE_DIR/.env.example" "$BASE_DIR/.env"
  echo "Created .env from .env.example"
fi

exec docker compose up -d --remove-orphans
