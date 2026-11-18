#!/usr/bin/env bash
# 离线验证交付文件和 YAML；严格模式复用应用自身的配置绑定约束。
set -euo pipefail
source "$(dirname -- "$0")/runtime.sh"
command -v "$JAVA" >/dev/null || { echo "Java is required; configure JAVA_HOME or PATH." >&2; exit 1; }
java_version=$("$JAVA" -XshowSettings:properties -version 2>&1 | awk '/java.specification.version =/ {print $3}')
[[ "$java_version" =~ ^[0-9]+$ && "$java_version" -ge 25 ]] || {
  echo "Java 25 or newer is required." >&2
  exit 1
}
shopt -s nullglob
app_jars=("$DIST_DIR"/app/application-starter-*.jar)
lib_jars=("$DIST_DIR"/lib/*.jar)
[[ ${#app_jars[@]} -eq 1 && ${#lib_jars[@]} -gt 0 ]] || {
  echo "Expected one application thin JAR and runtime libraries." >&2
  exit 1
}
for file in config/application.yaml config/log4j2-spring.xml; do
  [[ -r "$DIST_DIR/$file" ]] || { echo "Missing distribution file: $file" >&2; exit 1; }
done
exec "$JAVA" "-Dspring.config.additional-location=file:$DIST_DIR/config/" \
  -cp "$DIST_DIR/app/*:$DIST_DIR/lib/*" \
  team.carrypigeon.backend.distribution.DistributionVerifier "$@"
