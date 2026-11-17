#!/usr/bin/env bash
set -euo pipefail
source "$(dirname -- "$0")/runtime.sh"
bash "$DIST_DIR/bin/verify.sh" --strict-config "${RUNTIME_ARGS[@]}"
timeout_seconds=${CP_START_TIMEOUT_SECONDS:-60}
[[ "$timeout_seconds" =~ ^[1-9][0-9]*$ ]] || { echo "CP_START_TIMEOUT_SECONDS must be positive." >&2; exit 1; }
lock_application
log_file="$LOG_DIR/application-stdout.log"
# 每次启动清空本次 stdout，避免历史启动标记被误认为本次已就绪。
nohup "${JAVA_COMMAND[@]}" "${RUNTIME_ARGS[@]}" > "$log_file" 2>&1 < /dev/null &
application_pid=$!
printf '%s\n' "$application_pid" > "$PID_FILE"

cleanup_failed_start() {
  if is_application_process "$application_pid"; then
    kill -TERM "$application_pid" 2>/dev/null || true
    for ((attempt=0; attempt<10; attempt++)); do
      is_application_process "$application_pid" || break
      sleep 1
    done
    # 仅清理本次启动且仍符合目录和主类标记的进程。
    if is_application_process "$application_pid"; then
      kill -KILL "$application_pid" 2>/dev/null || true
    fi
  fi
  wait "$application_pid" 2>/dev/null || true
  rm -f "$PID_FILE"
}
trap 'cleanup_failed_start' EXIT
trap 'exit 1' INT TERM
deadline=$((SECONDS + timeout_seconds))
while ((SECONDS < deadline)); do
  if ! kill -0 "$application_pid" 2>/dev/null; then
    echo "Application exited before startup completed. See $log_file" >&2
    exit 1
  fi
  # main 在 SpringApplication.run 返回后写入此标记，包含初始化检查和监听启动。
  # 就绪探针尚未实现，因此不把任意可连接端口或发现接口当作完整就绪。
  if is_application_process "$application_pid" && grep -Fq 'Application is running ...' "$log_file"; then
    trap - EXIT INT TERM
    echo "Application started. PID=$application_pid; log=$log_file"
    exit 0
  fi
  sleep 1
done
echo "Application startup timed out. See $log_file" >&2
exit 1
