#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)

exec mvn -f "$BASE_DIR/pom.xml" -pl distribution -am package "$@"
