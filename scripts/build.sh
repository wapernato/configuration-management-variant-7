#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
JAVAC=javac
if [ -n "${JAVA_HOME:-}" ]; then JAVAC="$JAVA_HOME/bin/javac"; fi
mkdir -p "$PROJECT_DIR/build/classes"
find "$PROJECT_DIR/src/main/java" -name '*.java' -print |
    sed 's/.*/"&"/' > "$PROJECT_DIR/build/sources.txt"
"$JAVAC" --release 17 -encoding UTF-8 -d "$PROJECT_DIR/build/classes" \
    @"$PROJECT_DIR/build/sources.txt"
