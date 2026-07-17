#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)

exec env COMPOSE_DISABLE_ENV_FILE=1 docker compose -f "$BASE_DIR/docker-compose.yaml" logs -f "$@"
