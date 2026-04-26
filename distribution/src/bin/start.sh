#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APP_JAR=$(find "$BASE_DIR/app" -maxdepth 1 -name 'application-starter-*.jar' ! -name '*-exec.jar' ! -name '*.original' | head -n 1)

if [ -z "$APP_JAR" ]; then
  echo "application-starter thin jar not found under $BASE_DIR/app" >&2
  exit 1
fi

CLASSPATH="$APP_JAR:$BASE_DIR/libs/*"

exec java -cp "$CLASSPATH" team.carrypigeon.backend.starter.ApplicationStarter "$@"
