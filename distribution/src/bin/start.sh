#!/usr/bin/env bash
set -euo pipefail
source "$(dirname -- "$0")/runtime.sh"
bash "$DIST_DIR/bin/verify.sh" --strict-config "${RUNTIME_ARGS[@]}"
lock_application
printf '%s\n' "$$" > "$PID_FILE"
exec "${JAVA_COMMAND[@]}" "${RUNTIME_ARGS[@]}"
