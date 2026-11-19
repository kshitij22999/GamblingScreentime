#!/usr/bin/env bash
# Builds the debug APK, installs it on the connected device and launches it.
set -euo pipefail
source "$(dirname "$0")/env.sh"
PACKAGE=com.timegamblerapp

adb get-state >/dev/null 2>&1 || { echo "No device connected (adb devices is empty)." >&2; exit 1; }

cd "$APP_DIR/android"
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb reverse tcp:8081 tcp:8081

echo "Waiting for Metro on :8081..."
for _ in $(seq 1 60); do
  curl -s http://localhost:8081/status | grep -q packager-status:running && break
  sleep 1
done

adb shell am start -n "$PACKAGE/.MainActivity"
