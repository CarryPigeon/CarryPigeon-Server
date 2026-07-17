#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
DOMAIN="localhost"

if [ ! -f "$BASE_DIR/deploy/nginx/certs/fullchain.pem" ] || [ ! -f "$BASE_DIR/deploy/nginx/certs/privkey.pem" ]; then
  sh "$BASE_DIR/bin/linux/nginx-dev-cert.sh" "$DOMAIN"
fi

COMPOSE_DISABLE_ENV_FILE=1 docker compose -f "$BASE_DIR/docker-compose.yaml" --profile edge up -d --remove-orphans nginx

elapsed=0
timeout_seconds=60
while [ "$elapsed" -lt "$timeout_seconds" ]; do
  status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' carrypigeon-nginx 2>/dev/null || true)
  if [ "$status" = "healthy" ]; then
    echo "Nginx reverse proxy is ready: https://$DOMAIN"
    exit 0
  fi
  sleep 2
  elapsed=$((elapsed + 2))
done

echo "Timed out waiting for carrypigeon-nginx to become healthy" >&2
exit 1
