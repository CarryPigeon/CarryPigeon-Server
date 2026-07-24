#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
CONFIG_DIR="$BASE_DIR/config"
LIB_DIR="$BASE_DIR/lib"
PLUGIN_DIR="$BASE_DIR/plugins"
SAFE_MODE=0

if [ "$#" -gt 0 ] && [ "$1" = "--safe-mode" ]; then
  SAFE_MODE=1
  shift
fi

APP_COUNT=$(find "$BASE_DIR/app" -maxdepth 1 -type f -name 'application-starter-*.jar' ! -name '*-exec.jar' ! -name '*.original' | wc -l | tr -d ' ')
APP_JAR=$(find "$BASE_DIR/app" -maxdepth 1 -type f -name 'application-starter-*.jar' ! -name '*-exec.jar' ! -name '*.original' | head -n 1)

DEFAULT_LOG_HOME="${CP_LOG_HOME:-$BASE_DIR/service-logs}"

if [ -z "$APP_JAR" ]; then
  echo "application-starter thin jar not found under $BASE_DIR/app" >&2
  exit 1
fi

if [ "$APP_COUNT" -ne 1 ]; then
  echo "expected exactly one application-starter thin jar under $BASE_DIR/app" >&2
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

if [ ! -d "$LIB_DIR" ]; then
  echo "lib directory not found under $BASE_DIR" >&2
  exit 1
fi

if [ ! -d "$PLUGIN_DIR" ]; then
  echo "plugins directory not found under $BASE_DIR" >&2
  exit 1
fi

mkdir -p "$DEFAULT_LOG_HOME"

CLASSPATH="$APP_JAR:$LIB_DIR/*"
if [ "$SAFE_MODE" -eq 0 ]; then
  CLASSPATH="$CLASSPATH:$PLUGIN_DIR/*"
fi
SPRING_CONFIG_ARG="--spring.config.additional-location=file:$CONFIG_DIR/"

# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} ${CP_JAVA_OPTS:-} \
  "-Dlogging.config=file:$CONFIG_DIR/log4j2-spring.xml" \
  "-Dcp.log.home=$DEFAULT_LOG_HOME" \
  -cp "$CLASSPATH" \
  team.carrypigeon.backend.starter.ApplicationStarter \
  "$SPRING_CONFIG_ARG" \
  "$@"
