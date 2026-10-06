#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
CASE=${1:-success}
case "$CASE" in
    success) SCRIPT=startup-success.txt ;;
    missing|directory|binary|options|arguments|unknown|quotes|cd-file|wc-options|wc-arguments|empty-path|exit-arguments)
        SCRIPT="error-$CASE.txt" ;;
    *) echo "Неизвестный сценарий: $CASE" >&2; exit 2 ;;
esac
exec "$PROJECT_DIR/run.sh" --vfs "$PROJECT_DIR/examples/stage3/deep.csv" \
    --startup-script "$PROJECT_DIR/examples/stage4/$SCRIPT"
