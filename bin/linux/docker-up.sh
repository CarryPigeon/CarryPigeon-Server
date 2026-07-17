#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)

compose() {
  COMPOSE_DISABLE_ENV_FILE=1 docker compose -f "$BASE_DIR/docker-compose.yaml" "$@"
}

compose up -d --remove-orphans

wait_for_health() {
  container_name="$1"
  expected_status="$2"
  timeout_seconds="$3"
  elapsed=0

  while [ "$elapsed" -lt "$timeout_seconds" ]; do
    status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_name" 2>/dev/null || true)
    if [ "$status" = "$expected_status" ]; then
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done

  echo "Timed out waiting for $container_name to become $expected_status" >&2
  return 1
}

wait_for_exit_code() {
  container_name="$1"
  expected_exit_code="$2"
  timeout_seconds="$3"
  elapsed=0

  while [ "$elapsed" -lt "$timeout_seconds" ]; do
    status=$(docker inspect -f '{{.State.Status}}' "$container_name" 2>/dev/null || true)
    exit_code=$(docker inspect -f '{{.State.ExitCode}}' "$container_name" 2>/dev/null || true)
    if [ "$status" = "exited" ] && [ "$exit_code" = "$expected_exit_code" ]; then
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done

  echo "Timed out waiting for $container_name to exit with code $expected_exit_code" >&2
  return 1
}

wait_for_health "carrypigeon-mysql" "healthy" 120
wait_for_health "carrypigeon-redis" "healthy" 60
wait_for_health "carrypigeon-minio" "healthy" 120
wait_for_exit_code "carrypigeon-minio-init" 0 120

echo "Docker dependencies are ready."
