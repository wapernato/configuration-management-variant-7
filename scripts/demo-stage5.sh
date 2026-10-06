#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
CASE=${1:-success}
case "$CASE" in
    success) SCRIPT=startup-success.txt ;;
    mkdir-parent|mkdir-existing|mkdir-file|mkdir-options|mkdir-arguments|mkdir-rollback|\
    cp-missing|cp-directory|cp-self|cp-descendant|cp-parent|cp-conflict|\
    cp-options|cp-arguments|cp-multiple|cp-rollback|empty-path|quotes) SCRIPT="error-$CASE.txt" ;;
    *) echo "Неизвестный сценарий: $CASE" >&2; exit 2 ;;
esac
exec "$PROJECT_DIR/run.sh" --vfs "$PROJECT_DIR/examples/stage3/deep.csv" \
    --startup-script "$PROJECT_DIR/examples/stage5/$SCRIPT"
