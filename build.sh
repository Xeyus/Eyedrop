#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TOOLS=${EYEDROP_TOOLS:-/workspace/work/tools}
BT="$TOOLS/unpacked-buildtools.zip/android-15"
PLATFORM="$TOOLS/unpacked-platform.zip/android-35/android.jar"
mkdir -p build/classes build/dex build/compiled build/generated
"$BT/aapt2" compile --dir app/src/main/res -o build/resources.zip
sed 's/<manifest /<manifest package="com.eyedrop.manager" /' app/src/main/AndroidManifest.xml > build/AndroidManifest.xml
"$BT/aapt2" link --java build/generated -o build/unsigned.apk -I "$PLATFORM" --manifest build/AndroidManifest.xml build/resources.zip
java -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -jar "$TOOLS/ecj.jar" -1.8 -encoding UTF-8 -bootclasspath "$PLATFORM:$BT/core-lambda-stubs.jar" -d build/classes $(find app/src/main/java build/generated -name '*.java')
java -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --lib "$PLATFORM" --min-api 26 --output build/dex $(find build/classes -name '*.class')
(cd build/dex && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
if [ ! -f "$TOOLS/development.keystore" ]; then
 keytool -genkeypair -keystore "$TOOLS/development.keystore" -storepass android -alias androiddebugkey -keypass android -dname 'CN=EyeDrop Development' -keyalg RSA -keysize 2048 -validity 10000
fi
java -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -jar "$BT/lib/apksigner.jar" sign --ks "$TOOLS/development.keystore" --ks-pass pass:android --key-pass pass:android --out build/EyeDrop-v0.2.0-A.apk build/aligned.apk
java -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -jar "$BT/lib/apksigner.jar" verify --verbose build/EyeDrop-v0.2.0-A.apk
"$BT/aapt2" dump badging build/EyeDrop-v0.2.0-A.apk
