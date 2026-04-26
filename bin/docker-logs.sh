#!/usr/bin/env sh

set -eu

exec docker compose logs -f "$@"
