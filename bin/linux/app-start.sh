#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
ENV_FILE="$BASE_DIR/.env"
ENV_TEMPLATE_FILE="$BASE_DIR/.env.example"
MYSQL_HOST="127.0.0.1"
MYSQL_PORT="${MYSQL_PORT:-3306}"
REDIS_HOST="127.0.0.1"
REDIS_PORT="${REDIS_PORT:-6379}"
MINIO_HOST="127.0.0.1"
MINIO_PORT="${MINIO_API_PORT:-9000}"

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
    echo "Run 'bash bin/linux/docker-up.sh' first and wait until dependencies are ready." >&2
    exit 1
  fi
}

require_env "${CP_CHAT_AUTH_JWT_SECRET:-}" "CP_CHAT_AUTH_JWT_SECRET" "Set CP_CHAT_AUTH_JWT_SECRET in .env or export it before running bin/linux/app-start.sh."

if [ "${#CP_CHAT_AUTH_JWT_SECRET}" -lt 32 ]; then
  echo "CP_CHAT_AUTH_JWT_SECRET must be at least 32 characters." >&2
  exit 1
fi

check_port "$MYSQL_HOST" "$MYSQL_PORT" "MySQL"
check_port "$REDIS_HOST" "$REDIS_PORT" "Redis"
check_port "$MINIO_HOST" "$MINIO_PORT" "MinIO"

mvn -f "$BASE_DIR/pom.xml" -pl application-starter -am -DskipTests install

exec mvn -f "$BASE_DIR/application-starter/pom.xml" -DskipTests spring-boot:run "$@"
