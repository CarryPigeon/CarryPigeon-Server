#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DIST_DIR="$BASE_DIR/distribution/target/full-distribution/full-distribution"

if [ ! -x "$DIST_DIR/bin/stop.sh" ]; then
  echo "Distribution stop script not found. Run bin/dist-package.sh first." >&2
  exit 1
fi

exec bash "$DIST_DIR/bin/stop.sh"
