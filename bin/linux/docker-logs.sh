#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)

exec docker compose --env-file "$BASE_DIR/.env" -f "$BASE_DIR/docker-compose.yaml" logs -f "$@"
