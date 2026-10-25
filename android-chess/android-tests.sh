#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell svc wifi disable
adb shell svc data disable
adb shell settings put global airplane_mode_on 1
mkdir -p test-results
for phase in seed restore; do
  adb shell am force-stop io.github.ae6820dc.chesssamples3
  adb shell am instrument -w -e phase "$phase" io.github.ae6820dc.chesssamples3.test/io.github.ae6820dc.studio.SmokeInstrumentation | tee "test-results/$phase.txt"
  grep -Eq 'PASS [0-9]+ checks across [0-9]+ samples' "test-results/$phase.txt"
  if grep -Eq 'FAIL|INSTRUMENTATION_FAILED|Process crashed' "test-results/$phase.txt"; then exit 1; fi
done
adb exec-out screencap -p > test-results/home.png
