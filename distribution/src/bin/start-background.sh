#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
ENV_FILE="$BASE_DIR/.env"
ENV_TEMPLATE_FILE="$BASE_DIR/.env.example"
RUN_DIR="$BASE_DIR/run"
PID_FILE="$RUN_DIR/application.pid"
READY_MARKER="Started ApplicationStarter"

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

LOG_DIR="${CP_LOG_HOME:-$BASE_DIR/service-logs}"
OUT_FILE="$LOG_DIR/application-stdout.log"

mkdir -p "$RUN_DIR" "$LOG_DIR"

if [ -f "$PID_FILE" ]; then
  PID=$(cat "$PID_FILE")
  if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
    echo "Application is already running with PID $PID" >&2
    exit 1
  fi
  rm -f "$PID_FILE"
fi

nohup "$BASE_DIR/bin/start.sh" "$@" >"$OUT_FILE" 2>&1 &
PID=$!
printf '%s\n' "$PID" > "$PID_FILE"

TIMEOUT_SECONDS=60
ELAPSED=0
while [ "$ELAPSED" -lt "$TIMEOUT_SECONDS" ]; do
  if grep -q "$READY_MARKER" "$OUT_FILE" 2>/dev/null; then
    echo "Application started in background with PID $PID"
    echo "Stdout redirected to $OUT_FILE"
    exit 0
  fi

  if ! kill -0 "$PID" 2>/dev/null; then
    rm -f "$PID_FILE"
    echo "Application exited before becoming ready. See $OUT_FILE" >&2
    exit 1
  fi

  sleep 2
  ELAPSED=$((ELAPSED + 2))
done

kill "$PID" 2>/dev/null || true
rm -f "$PID_FILE"
echo "Application did not become ready within ${TIMEOUT_SECONDS}s. See $OUT_FILE" >&2
exit 1
