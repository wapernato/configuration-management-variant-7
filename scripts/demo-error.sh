#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
EXAMPLES="$PROJECT_DIR/examples/paths with spaces"
exec "$PROJECT_DIR/run.sh" --vfs "$EXAMPLES/vfs demo.csv" \
    --startup-script "$EXAMPLES/startup-error.txt"
