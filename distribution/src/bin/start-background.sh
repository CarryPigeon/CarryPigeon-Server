#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
RUN_DIR="$BASE_DIR/run"
LOG_DIR="${CP_LOG_HOME:-$BASE_DIR/service-logs}"
PID_FILE="$RUN_DIR/application.pid"
OUT_FILE="$LOG_DIR/application-stdout.log"

mkdir -p "$RUN_DIR" "$LOG_DIR"

if [ -f "$PID_FILE" ]; then
  PID=$(cat "$PID_FILE")
  if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
    echo "Application is already running with PID $PID" >&2
    exit 1
  fi
fi

nohup "$BASE_DIR/bin/start.sh" "$@" >"$OUT_FILE" 2>&1 &
PID=$!
printf '%s\n' "$PID" > "$PID_FILE"
echo "Application started in background with PID $PID"
echo "Stdout redirected to $OUT_FILE"
