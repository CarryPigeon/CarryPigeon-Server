#!/usr/bin/env bash
# 分发脚本共用目录、JVM 参数与进程身份约定；不承载业务配置解析。
set -euo pipefail
DIST_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
cd "$DIST_DIR"
MAIN_CLASS=team.carrypigeon.backend.starter.ApplicationStarter
PID_FILE="$DIST_DIR/run/application.pid"
LOG_DIR=${CP_LOG_HOME:-"$DIST_DIR/service-logs"}
[[ "$LOG_DIR" = /* ]] || LOG_DIR="$DIST_DIR/$LOG_DIR"
JAVA=${JAVA_HOME:+"$JAVA_HOME/bin/"}java
RUNTIME_ARGS=()
CLASSPATH="$DIST_DIR/app/*:$DIST_DIR/lib/*"
safe_mode=false
for argument in "$@"; do
  if [[ "$argument" == --safe-mode ]]; then
    safe_mode=true
  else
    RUNTIME_ARGS+=("$argument")
  fi
done
if [[ "$safe_mode" == false ]]; then
  CLASSPATH="$CLASSPATH:$DIST_DIR/plugins/*"
fi
# 目录标记用于停止前确认进程归属，避免误杀 PID 被复用后的其它进程。
JAVA_COMMAND=("$JAVA" "-Dcp.distribution.home=$DIST_DIR" "-Dcp.log.home=$LOG_DIR"
  "-Dlogging.config=file:$DIST_DIR/config/log4j2-spring.xml"
  "-Dspring.config.additional-location=file:$DIST_DIR/config/"
  -cp "$CLASSPATH" "$MAIN_CLASS")

is_application_process() {
  local candidate=$1 argument has_home=false has_main=false
  [[ "$candidate" =~ ^[0-9]+$ && "$candidate" -gt 1 && -r "/proc/$candidate/cmdline" ]] || return 1
  kill -0 "$candidate" 2>/dev/null || return 1
  while IFS= read -r -d '' argument; do
    [[ "$argument" != "-Dcp.distribution.home=$DIST_DIR" ]] || has_home=true
    [[ "$argument" != "$MAIN_CLASS" ]] || has_main=true
  done < "/proc/$candidate/cmdline"
  [[ "$has_home" == true && "$has_main" == true ]]
}

lock_application() {
  command -v flock >/dev/null || { echo "Linux flock is required." >&2; exit 1; }
  mkdir -p "$DIST_DIR/run" "$LOG_DIR"
  # JVM 继承锁描述符，前后台入口使用同一把锁，退出后由内核释放。
  exec 9>"$DIST_DIR/run/application.lock"
  flock -n 9 || { echo "This distribution is already running or starting." >&2; exit 1; }
  if [[ -f "$PID_FILE" ]] && is_application_process "$(cat "$PID_FILE")"; then
    echo "This distribution is already running." >&2
    exit 1
  fi
}
