#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
STRICT_ENV=0

while [ "$#" -gt 0 ]; do
  case "$1" in
    --strict-env)
      STRICT_ENV=1
      ;;
    *)
      echo "Unsupported argument: $1" >&2
      echo "Usage: $0 [--strict-env]" >&2
      exit 1
      ;;
  esac
  shift
done

require_path() {
  path="$1"
  description="$2"

  if [ ! -e "$path" ]; then
    echo "Missing $description: $path" >&2
    exit 1
  fi
}

require_file() {
  path="$1"
  description="$2"

  if [ ! -f "$path" ]; then
    echo "Missing $description: $path" >&2
    exit 1
  fi
}

require_exec() {
  path="$1"
  description="$2"

  if [ ! -x "$path" ]; then
    echo "Missing executable $description: $path" >&2
    exit 1
  fi
}

require_env_value() {
  value="$1"
  name="$2"

  if [ -z "$value" ]; then
    echo "Missing required env value in .env: $name" >&2
    exit 1
  fi
}

APP_JAR=$(find "$BASE_DIR/app" -maxdepth 1 -name 'application-starter-*.jar' ! -name '*-exec.jar' ! -name '*.original' | head -n 1)

require_path "$BASE_DIR/app" "app directory"
require_path "$BASE_DIR/libs" "libs directory"
require_path "$BASE_DIR/config" "config directory"
require_path "$BASE_DIR/bin" "bin directory"
require_path "$BASE_DIR/service/systemd" "systemd example directory"

require_file "$BASE_DIR/README.md" "distribution README"
require_file "$BASE_DIR/.env.example" "environment template"
require_file "$BASE_DIR/config/application.yaml" "application configuration"
require_file "$BASE_DIR/config/log4j2-spring.xml" "logging configuration"
require_file "$BASE_DIR/service/systemd/carrypigeon.service" "systemd unit example"
require_file "$BASE_DIR/service/systemd/README.md" "systemd usage guide"

require_exec "$BASE_DIR/bin/start.sh" "foreground launcher"
require_exec "$BASE_DIR/bin/start-background.sh" "background launcher"
require_exec "$BASE_DIR/bin/stop.sh" "stop launcher"
require_exec "$BASE_DIR/bin/verify.sh" "package verification launcher"

if [ -z "$APP_JAR" ]; then
  echo "application-starter thin jar not found under $BASE_DIR/app" >&2
  exit 1
fi

if [ "$STRICT_ENV" -eq 1 ]; then
  if [ ! -f "$BASE_DIR/.env" ]; then
    echo "Strict env verification requested, but $BASE_DIR/.env is missing." >&2
    exit 1
  fi

  set -a
  # shellcheck disable=SC1090
  . "$BASE_DIR/.env"
  set +a

  require_env_value "${CP_CHAT_AUTH_JWT_SECRET:-}" "CP_CHAT_AUTH_JWT_SECRET"
  require_env_value "${CP_CHAT_SERVER_ID:-}" "CP_CHAT_SERVER_ID"

  if [ "${#CP_CHAT_AUTH_JWT_SECRET}" -lt 32 ]; then
    echo "CP_CHAT_AUTH_JWT_SECRET must be at least 32 characters." >&2
    exit 1
  fi
fi

echo "Distribution package verification passed."
echo "Base directory: $BASE_DIR"
echo "Thin jar: $APP_JAR"
if [ "$STRICT_ENV" -eq 1 ]; then
  echo "Environment readiness: strict verification passed"
else
  echo "Environment readiness: skipped (.env not required without --strict-env)"
fi
