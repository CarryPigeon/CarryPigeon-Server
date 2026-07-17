#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
DIST_DIR="$BASE_DIR/distribution/target/full-distribution/full-distribution"
ZIP_FILE="$BASE_DIR/distribution/target/full-distribution.zip"
RELEASE_DIR="$BASE_DIR/distribution/target/release"
VERIFY_SCRIPT="$DIST_DIR/bin/verify.sh"

checksum_file() {
  target_file="$1"

  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$target_file" | awk '{print $1}'
    return 0
  fi

  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$target_file" | awk '{print $1}'
    return 0
  fi

  echo "Neither sha256sum nor shasum is available." >&2
  exit 1
}

if [ ! -x "$VERIFY_SCRIPT" ]; then
  echo "Distribution verification script not found. Run bin/linux/dist-package.sh first." >&2
  exit 1
fi

if [ ! -f "$ZIP_FILE" ]; then
  echo "Distribution zip not found: $ZIP_FILE" >&2
  exit 1
fi

bash "$VERIFY_SCRIPT"

mkdir -p "$RELEASE_DIR"

APP_JAR=$(find "$DIST_DIR/app" -maxdepth 1 -name 'application-starter-*.jar' ! -name '*-exec.jar' ! -name '*.original' | head -n 1)
if [ -z "$APP_JAR" ]; then
  echo "application-starter thin jar not found under $DIST_DIR/app" >&2
  exit 1
fi

APP_JAR_NAME=$(basename "$APP_JAR")
VERSION=${APP_JAR_NAME#application-starter-}
VERSION=${VERSION%.jar}
ZIP_SHA256=$(checksum_file "$ZIP_FILE")
GIT_COMMIT=$(git -C "$BASE_DIR" rev-parse HEAD 2>/dev/null || printf 'unknown')
GIT_REF=$(git -C "$BASE_DIR" rev-parse --abbrev-ref HEAD 2>/dev/null || printf 'unknown')
GENERATED_AT=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
MANIFEST_FILE="$RELEASE_DIR/full-distribution-manifest.json"
CHECKSUM_FILE="$RELEASE_DIR/full-distribution.sha256"

printf '%s  %s\n' "$ZIP_SHA256" "full-distribution.zip" > "$CHECKSUM_FILE"

cat > "$MANIFEST_FILE" <<EOF
{
  "project": "CarryPigeon Backend",
  "module": "distribution",
  "version": "$VERSION",
  "generated_at_utc": "$GENERATED_AT",
  "git_commit": "$GIT_COMMIT",
  "git_ref": "$GIT_REF",
  "artifacts": [
    {
      "name": "full-distribution.zip",
      "path": "distribution/target/full-distribution.zip",
      "sha256": "$ZIP_SHA256"
    }
  ],
  "package_layout": {
    "root_dir": "distribution/target/full-distribution/full-distribution",
    "thin_jar": "app/$APP_JAR_NAME",
    "config_dir": "config",
    "bin_dir": "bin",
    "service_dir": "service"
  },
  "verification": {
    "package_verify_command": "bash distribution/target/full-distribution/full-distribution/bin/verify.sh",
    "strict_config_verify_command": "bash distribution/target/full-distribution/full-distribution/bin/verify.sh --strict-config"
  }
}
EOF

echo "Release bundle metadata generated:"
echo "  Zip: $ZIP_FILE"
echo "  Checksum: $CHECKSUM_FILE"
echo "  Manifest: $MANIFEST_FILE"
