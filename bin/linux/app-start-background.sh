#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
LOG_DIR="$BASE_DIR/service-logs"
LOG_FILE="$LOG_DIR/application-starter-stdout.log"
PID_FILE="$LOG_DIR/application-starter.pid"
READY_MARKER='Started ApplicationStarter'

mkdir -p "$LOG_DIR"

nohup sh "$BASE_DIR/bin/linux/app-start.sh" "$@" >"$LOG_FILE" 2>&1 &
PID=$!
printf '%s\n' "$PID" >"$PID_FILE"

elapsed=0
while [ "$elapsed" -lt 60 ]; do
  if ! kill -0 "$PID" 2>/dev/null; then
    echo "application-starter exited before becoming ready. See log: $LOG_FILE" >&2
    exit 1
  fi

  if grep -q "$READY_MARKER" "$LOG_FILE" 2>/dev/null; then
    printf 'Started application-starter in background. PID=%s\n' "$PID"
    printf 'Stdout log: %s\n' "$LOG_FILE"
    exit 0
  fi

  sleep 2
  elapsed=$((elapsed + 2))
done

echo "application-starter did not become ready within timeout. See log: $LOG_FILE" >&2
exit 1
