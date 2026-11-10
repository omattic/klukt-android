#!/usr/bin/env bash
set -euo pipefail
serial="${1:?Usage: scripts/android-install.sh ADB_SERIAL}"
sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"; adb="$sdk_root/platform-tools/adb"
./gradlew assembleDebug
"$adb" -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb" -s "$serial" shell monkey -p com.guerrerocarlos.offlinenotes 1
