#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
exec "$PROJECT_DIR/run.sh" --vfs "$PROJECT_DIR/examples/stage3/invalid.csv" \
    --startup-script "$PROJECT_DIR/examples/stage3/startup-success.txt"
