#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
PID_FILE="$BASE_DIR/run/application.pid"

if [ ! -f "$PID_FILE" ]; then
  echo "PID file not found: $PID_FILE" >&2
  exit 1
fi

PID=$(cat "$PID_FILE")

if [ -z "$PID" ]; then
  echo "PID file is empty: $PID_FILE" >&2
  exit 1
fi

if kill -0 "$PID" 2>/dev/null; then
  kill "$PID"
  WAIT_SECONDS=30
  ELAPSED=0
  while kill -0 "$PID" 2>/dev/null; do
    if [ "$ELAPSED" -ge "$WAIT_SECONDS" ]; then
      kill -9 "$PID" 2>/dev/null || true
      echo "Application process $PID did not stop gracefully and was killed"
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
