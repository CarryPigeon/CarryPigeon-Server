#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
PID_FILE="$BASE_DIR/run/application.pid"

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

if [ ! -f "$PID_FILE" ]; then
  echo "PID file not found: $PID_FILE" >&2
  exit 1
fi

PID=$(cat "$PID_FILE")

if ! is_valid_pid "$PID"; then
  echo "PID file does not contain a positive process ID: $PID_FILE" >&2
  exit 1
fi

if kill -0 "$PID" 2>/dev/null; then
  if ! is_application_process "$PID"; then
    rm -f "$PID_FILE"
    echo "Refusing to stop PID $PID because it is not the CarryPigeon application process" >&2
    exit 1
  fi
  kill "$PID"
  WAIT_SECONDS=30
  ELAPSED=0
  while kill -0 "$PID" 2>/dev/null && is_application_process "$PID"; do
    if [ "$ELAPSED" -ge "$WAIT_SECONDS" ]; then
      if is_application_process "$PID"; then
        kill -9 "$PID" 2>/dev/null || true
        echo "Application process $PID did not stop gracefully and was killed"
      fi
      rm -f "$PID_FILE"
      exit 0
    fi
    sleep 1
    ELAPSED=$((ELAPSED + 1))
  done
  echo "Application process $PID stopped"
else
  echo "Process $PID is not running"
fi

rm -f "$PID_FILE"
