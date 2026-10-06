#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$PROJECT_DIR"
export DISPLAY="${DISPLAY:-:1}"
if [ ! -f target/variant7-terminal.jar ]; then
    ./mvnw --batch-mode --no-transfer-progress package
fi
exec java -jar target/variant7-terminal.jar --vfs examples/stage3/deep.csv "$@"
