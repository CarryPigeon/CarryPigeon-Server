#!/usr/bin/env bash
set -euo pipefail
source "$(dirname -- "$0")/runtime.sh"
[[ -f "$PID_FILE" ]] || { echo "No application PID file."; exit 0; }
application_pid=$(cat "$PID_FILE")
[[ "$application_pid" =~ ^[0-9]+$ && "$application_pid" -gt 1 ]] || {
  echo "Invalid PID file; inspect $PID_FILE before removing it." >&2
  exit 1
}
if ! is_application_process "$application_pid"; then
  if kill -0 "$application_pid" 2>/dev/null; then
    echo "PID belongs to another process; refusing to stop it." >&2
    exit 1
  fi
  rm -f "$PID_FILE"
  echo "Removed stale PID file."
  exit 0
fi
timeout_seconds=${CP_STOP_TIMEOUT_SECONDS:-30}
[[ "$timeout_seconds" =~ ^[1-9][0-9]*$ ]] || { echo "CP_STOP_TIMEOUT_SECONDS must be positive." >&2; exit 1; }
kill -TERM "$application_pid"
deadline=$((SECONDS + timeout_seconds))
while ((SECONDS < deadline)); do
  if ! is_application_process "$application_pid"; then
    # 仅删除本次停止的 PID，避免覆盖另一入口刚写入的新进程记录。
    [[ "$(cat "$PID_FILE" 2>/dev/null || true)" != "$application_pid" ]] || rm -f "$PID_FILE"
    echo "Application stopped."
    exit 0
  fi
  sleep 1
done
echo "Application did not stop in time; PID file retained for inspection." >&2
exit 1
