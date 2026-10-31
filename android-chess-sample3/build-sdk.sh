#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
sdk="${ANDROID_HOME:?}"
bt="$sdk/build-tools/36.0.0"
platform="$sdk/platforms/android-36/android.jar"
mkdir -p build/classes build/dex build/test-classes build/test-dex app/build/outputs/apk/debug app/build/outputs/apk/androidTest/debug
"$bt/aapt2" compile --dir app/src/main/res -o build/resources.zip
"$bt/aapt2" link -I "$platform" --manifest app/src/main/AndroidManifest.xml --min-sdk-version 26 --target-sdk-version 36 --version-code 1 --version-name 1.0 -A app/src/main/assets -o build/app-unsigned.apk build/resources.zip
javac -source 8 -target 8 -classpath "$platform" -d build/classes app/src/main/java/io/github/ae6820dc/chesssample3/MainActivity.java
"$bt/d8" --min-api 26 --lib "$platform" --output build/dex $(find build/classes -name '*.class')
(cd build/dex && zip -q ../app-unsigned.apk classes.dex)
"$bt/zipalign" -f -p 4 build/app-unsigned.apk build/app-aligned.apk
"$bt/apksigner" sign --ks signing/chess-debug.jks --ks-key-alias chess-debug --ks-pass pass:android --key-pass pass:android --out app/build/outputs/apk/debug/app-debug.apk build/app-aligned.apk
"$bt/apksigner" verify --verbose app/build/outputs/apk/debug/app-debug.apk
"$bt/aapt2" link -I "$platform" --manifest app/src/androidTest/AndroidManifest.xml --min-sdk-version 26 --target-sdk-version 36 -o build/test-unsigned.apk
javac -source 8 -target 8 -classpath "$platform:build/classes" -d build/test-classes app/src/androidTest/java/io/github/ae6820dc/chesssample3/SmokeInstrumentation.java
"$bt/d8" --min-api 26 --lib "$platform" --classpath build/classes --output build/test-dex $(find build/test-classes -name '*.class')
(cd build/test-dex && zip -q ../test-unsigned.apk classes.dex)
"$bt/zipalign" -f -p 4 build/test-unsigned.apk build/test-aligned.apk
"$bt/apksigner" sign --ks signing/chess-debug.jks --ks-key-alias chess-debug --ks-pass pass:android --key-pass pass:android --out app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk build/test-aligned.apk
