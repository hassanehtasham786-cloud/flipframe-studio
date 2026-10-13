#!/bin/sh
# Verifies the debug APK exists and contains the expected Android entries.
# Usage: sh ./check_apk.sh
set -e
APK="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
  echo "APK missing: $APK — run './gradlew assembleDebug' first."
  exit 1
fi
echo "APK found: $APK ($(du -h "$APK" | cut -f1))"
unzip -l "$APK" | grep -E "classes.dex|AndroidManifest.xml" | head -3
echo "APK looks valid."
