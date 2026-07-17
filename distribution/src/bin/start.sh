#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
CONFIG_DIR="$BASE_DIR/config"
APP_JAR=$(find "$BASE_DIR/app" -maxdepth 1 -name 'application-starter-*.jar' ! -name '*-exec.jar' ! -name '*.original' | head -n 1)

DEFAULT_LOG_HOME="${CP_LOG_HOME:-$BASE_DIR/service-logs}"

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
