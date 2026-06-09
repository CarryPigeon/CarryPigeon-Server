#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
ENV_FILE="$BASE_DIR/.env"
ENV_TEMPLATE_FILE="$BASE_DIR/.env.example"
CONFIG_DIR="$BASE_DIR/config"
APP_JAR=$(find "$BASE_DIR/app" -maxdepth 1 -name 'application-starter-*.jar' ! -name '*-exec.jar' ! -name '*.original' | head -n 1)

if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
elif [ -f "$ENV_TEMPLATE_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$ENV_TEMPLATE_FILE"
  set +a
fi

DEFAULT_LOG_HOME="${CP_LOG_HOME:-$BASE_DIR/service-logs}"

require_env() {
  value="$1"
  name="$2"
  hint="$3"

  if [ -z "$value" ]; then
    echo "Missing required configuration: $name" >&2
    echo "$hint" >&2
    exit 1
  fi
}

is_enabled() {
  value="${1:-}"
  case "$value" in
    false|FALSE|False|0|no|NO|No|off|OFF|Off)
      return 1
      ;;
    *)
      return 0
      ;;
  esac
}

check_port() {
  host="$1"
  port="$2"
  name="$3"

  if ! python - "$host" "$port" <<'PY'
import socket
import sys

host = sys.argv[1]
port = int(sys.argv[2])

sock = socket.socket()
sock.settimeout(1)
try:
    sock.connect((host, port))
except OSError:
    sys.exit(1)
finally:
    sock.close()
PY
  then
    echo "$name is not reachable at $host:$port" >&2
    exit 1
  fi
}

if [ -z "$APP_JAR" ]; then
  echo "application-starter thin jar not found under $BASE_DIR/app" >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "java executable not found in PATH" >&2
  exit 1
fi

if [ ! -f "$CONFIG_DIR/application.yaml" ]; then
  echo "application.yaml not found under $CONFIG_DIR" >&2
  exit 1
fi

if [ ! -f "$CONFIG_DIR/log4j2-spring.xml" ]; then
  echo "log4j2-spring.xml not found under $CONFIG_DIR" >&2
  exit 1
fi

require_env "${CP_CHAT_AUTH_JWT_SECRET:-}" "CP_CHAT_AUTH_JWT_SECRET" "Set CP_CHAT_AUTH_JWT_SECRET in .env or export it before running the distribution package."

if [ "${#CP_CHAT_AUTH_JWT_SECRET}" -lt 32 ]; then
  echo "CP_CHAT_AUTH_JWT_SECRET must be at least 32 characters." >&2
  exit 1
fi

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
MINIO_HOST="${MINIO_HOST:-127.0.0.1}"
MINIO_PORT="${MINIO_API_PORT:-9000}"

if is_enabled "${CP_INFRASTRUCTURE_SERVICE_DATABASE_ENABLED:-true}"; then
  check_port "$MYSQL_HOST" "$MYSQL_PORT" "MySQL"
fi

if is_enabled "${CP_INFRASTRUCTURE_SERVICE_CACHE_ENABLED:-true}"; then
  check_port "$REDIS_HOST" "$REDIS_PORT" "Redis"
fi

if is_enabled "${CP_INFRASTRUCTURE_SERVICE_STORAGE_ENABLED:-true}"; then
  check_port "$MINIO_HOST" "$MINIO_PORT" "MinIO"
fi

mkdir -p "$DEFAULT_LOG_HOME"

CLASSPATH="$APP_JAR:$BASE_DIR/libs/*"
SPRING_CONFIG_ARG="--spring.config.additional-location=file:$CONFIG_DIR/"

# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} ${CP_JAVA_OPTS:-} \
  "-Dlogging.config=file:$CONFIG_DIR/log4j2-spring.xml" \
  "-Dcp.log.home=$DEFAULT_LOG_HOME" \
  -cp "$CLASSPATH" \
  team.carrypigeon.backend.starter.ApplicationStarter \
  "$SPRING_CONFIG_ARG" \
  "$@"
