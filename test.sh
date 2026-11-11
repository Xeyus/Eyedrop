#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TOOLS=${EYEDROP_TOOLS:-/workspace/work/tools}
mkdir -p build/tests
java -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -jar "$TOOLS/ecj.jar" -17 -encoding UTF-8 -cp "$TOOLS/json.jar" -d build/tests app/src/main/java/com/eyedrop/manager/Schedule.java app/src/main/java/com/eyedrop/manager/Store.java app/src/main/java/com/eyedrop/manager/Constraints.java tests/CoreTest.java tests/ConstraintsTest.java
java -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -cp "build/tests:$TOOLS/json.jar" CoreTest
java -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -cp "build/tests:$TOOLS/json.jar" ConstraintsTest
