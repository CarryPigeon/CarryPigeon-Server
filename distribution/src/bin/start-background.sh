#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
RUN_DIR="$BASE_DIR/run"
PID_FILE="$RUN_DIR/application.pid"

LOG_DIR="${CP_LOG_HOME:-$BASE_DIR/service-logs}"
OUT_FILE="$LOG_DIR/application-stdout.log"
READINESS_URL="${CP_READINESS_URL:-http://127.0.0.1:${SERVER_PORT:-8080}/internal/readiness}"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl executable not found in PATH; background readiness probe cannot run" >&2
  exit 1
fi

mkdir -p "$RUN_DIR" "$LOG_DIR"

is_valid_pid() {
  case "$1" in
    ''|0|*[!0-9]*) return 1 ;;
    *) return 0 ;;
  esac
}

is_application_process() {
  target_pid="$1"
  if [ -r "/proc/$target_pid/cmdline" ]; then
    command_line=$(tr '\000' ' ' < "/proc/$target_pid/cmdline")
  else
    command_line=$(ps -p "$target_pid" -o command= 2>/dev/null || true)
  fi
  case "$command_line" in
    *team.carrypigeon.backend.starter.ApplicationStarter*) return 0 ;;
    *) return 1 ;;
  esac
}

if [ -f "$PID_FILE" ]; then
  PID=$(cat "$PID_FILE")
  if is_valid_pid "$PID" && kill -0 "$PID" 2>/dev/null; then
    if is_application_process "$PID"; then
      echo "Application is already running with PID $PID" >&2
      exit 1
    fi
    echo "Removing stale PID file that points to a different process: $PID" >&2
  fi
  rm -f "$PID_FILE"
fi

nohup "$BASE_DIR/bin/start.sh" "$@" >"$OUT_FILE" 2>&1 &
PID=$!
printf '%s\n' "$PID" > "$PID_FILE"

TIMEOUT_SECONDS=60
ELAPSED=0
while [ "$ELAPSED" -lt "$TIMEOUT_SECONDS" ]; do
  if curl --fail --silent --show-error --max-time 2 "$READINESS_URL" >/dev/null 2>&1; then
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

if kill -0 "$PID" 2>/dev/null && is_application_process "$PID"; then
  kill "$PID" 2>/dev/null || true
  STOP_ELAPSED=0
  while kill -0 "$PID" 2>/dev/null && is_application_process "$PID" && [ "$STOP_ELAPSED" -lt 10 ]; do
    sleep 1
    STOP_ELAPSED=$((STOP_ELAPSED + 1))
  done
  if kill -0 "$PID" 2>/dev/null && is_application_process "$PID"; then
    kill -9 "$PID" 2>/dev/null || true
  fi
fi
rm -f "$PID_FILE"
echo "Application did not become ready within ${TIMEOUT_SECONDS}s at $READINESS_URL. See $OUT_FILE" >&2
exit 1
