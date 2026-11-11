#!/usr/bin/env bash
# Linux x86_64; Java runtime 17+, keytool, curl, unzip and zip must be available.
set -euo pipefail
cd "$(dirname "$0")"
TOOLS=${EYEDROP_TOOLS:-/workspace/work/tools}
mkdir -p "$TOOLS"
fetch() { curl --fail --location --retry 2 "$1" -o "$2"; }
fetch https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.38.0/ecj-3.38.0.jar "$TOOLS/ecj.jar"
fetch https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar "$TOOLS/json.jar"
fetch https://dl.google.com/android/repository/platform-35_r02.zip "$TOOLS/platform.zip"
fetch https://dl.google.com/android/repository/build-tools_r35_linux.zip "$TOOLS/buildtools.zip"
unzip -qo "$TOOLS/platform.zip" -d "$TOOLS/unpacked-platform.zip"
unzip -qo "$TOOLS/buildtools.zip" -d "$TOOLS/unpacked-buildtools.zip"
rm "$TOOLS/platform.zip" "$TOOLS/buildtools.zip"
echo 'Build tools ready. Run ./test.sh and ./build.sh with the same EYEDROP_TOOLS value.'
