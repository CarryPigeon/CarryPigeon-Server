#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
CONFIG_DIR="$BASE_DIR/config"
MYSQL_HOST="127.0.0.1"
MYSQL_PORT="${MYSQL_PORT:-3306}"
REDIS_HOST="127.0.0.1"
REDIS_PORT="${REDIS_PORT:-6379}"
MINIO_HOST="127.0.0.1"
MINIO_PORT="${MINIO_API_PORT:-9000}"

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

check_port "$MYSQL_HOST" "$MYSQL_PORT" "MySQL"
check_port "$REDIS_HOST" "$REDIS_PORT" "Redis"
check_port "$MINIO_HOST" "$MINIO_PORT" "MinIO"

mvn -f "$BASE_DIR/pom.xml" -pl application-starter -am -DskipTests install

exec mvn -f "$BASE_DIR/application-starter/pom.xml" \
  -DskipTests \
  "-Dspring-boot.run.arguments=--spring.config.additional-location=file:$CONFIG_DIR/" \
  spring-boot:run \
  "$@"
