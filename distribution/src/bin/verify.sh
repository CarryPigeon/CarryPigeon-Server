#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
STRICT_CONFIG=0

while [ "$#" -gt 0 ]; do
  case "$1" in
    --strict-config)
      STRICT_CONFIG=1
      ;;
    *)
      echo "Unsupported argument: $1" >&2
      echo "Usage: $0 [--strict-config]" >&2
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

yaml_value() {
  file="$1"
  path="$2"

  python - "$file" "$path" <<'PY'
import re
import sys

file_path = sys.argv[1]
target = sys.argv[2].split(".")
stack = []

with open(file_path, encoding="utf-8") as config_file:
    for raw_line in config_file:
        if not raw_line.strip() or raw_line.lstrip().startswith("#"):
            continue
        if ":" not in raw_line:
            continue

        indent = len(raw_line) - len(raw_line.lstrip(" "))
        content = raw_line.strip()
        key, value = content.split(":", 1)
        key = key.strip()
        value = value.strip()

        while stack and stack[-1][0] >= indent:
            stack.pop()
        stack.append((indent, key))

        current_path = [item[1] for item in stack]
        if current_path == target:
            value = re.sub(r"\s+#.*$", "", value).strip()
            if len(value) >= 2 and value[0] == value[-1] and value[0] in "'\"":
                value = value[1:-1]
            print(value)
            sys.exit(0)
PY
}

require_config_value() {
  value="$1"
  name="$2"

  if [ -z "$value" ]; then
    echo "Missing required config value in config/application.yaml: $name" >&2
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

if [ "$STRICT_CONFIG" -eq 1 ]; then
  JWT_SECRET=$(yaml_value "$BASE_DIR/config/application.yaml" "cp.chat.auth.jwt.secret")
  SERVER_ID=$(yaml_value "$BASE_DIR/config/application.yaml" "cp.chat.server.id")

  require_config_value "$JWT_SECRET" "cp.chat.auth.jwt.secret"
  require_config_value "$SERVER_ID" "cp.chat.server.id"

  if [ "${#JWT_SECRET}" -lt 32 ]; then
    echo "cp.chat.auth.jwt.secret must be at least 32 characters." >&2
    exit 1
  fi
fi

echo "Distribution package verification passed."
echo "Base directory: $BASE_DIR"
echo "Thin jar: $APP_JAR"
if [ "$STRICT_CONFIG" -eq 1 ]; then
  echo "Configuration readiness: strict verification passed"
else
  echo "Configuration readiness: skipped (use --strict-config to validate required YAML values)"
fi
