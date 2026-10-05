#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
"$PROJECT_DIR/scripts/build.sh"
JAVA=java
if [ -n "${JAVA_HOME:-}" ]; then JAVA="$JAVA_HOME/bin/java"; fi
exec "$JAVA" -cp "$PROJECT_DIR/build/classes" ru.mirea.emulator.Main "$@"
