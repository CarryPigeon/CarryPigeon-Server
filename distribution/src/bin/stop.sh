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
  echo "Application process $PID stopped"
else
  echo "Process $PID is not running"
fi

rm -f "$PID_FILE"
