#!/usr/bin/env sh

set -eu

exec mvn -pl distribution -am package "$@"
