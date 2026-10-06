#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
"$PROJECT_DIR/scripts/build.sh"
JAVAC=javac
JAVA=java
if [ -n "${JAVA_HOME:-}" ]; then
    JAVAC="$JAVA_HOME/bin/javac"
    JAVA="$JAVA_HOME/bin/java"
fi
mkdir -p "$PROJECT_DIR/build/test-classes"
find "$PROJECT_DIR/checks" -name '*.java' -print |
    sed 's/.*/"&"/' > "$PROJECT_DIR/build/tests.txt"
"$JAVAC" --release 17 -encoding UTF-8 -cp "$PROJECT_DIR/build/classes" \
    -d "$PROJECT_DIR/build/test-classes" @"$PROJECT_DIR/build/tests.txt"
CP="$PROJECT_DIR/build/classes:$PROJECT_DIR/build/test-classes"
cd "$PROJECT_DIR"
"$JAVA" -Djava.awt.headless=true -cp "$CP" ru.mirea.emulator.RegressionChecks
"$JAVA" -Djava.awt.headless=true -cp "$CP" ru.mirea.emulator.VfsChecks
"$JAVA" -Djava.awt.headless=true -cp "$CP" ru.mirea.emulator.BasicChecks
"$JAVA" -Djava.awt.headless=true -cp "$CP" ru.mirea.emulator.ScenarioChecks
if [ "${GUI_TESTS:-0}" = 1 ]; then
    "$JAVA" -cp "$CP" ru.mirea.emulator.GuiChecks
fi
