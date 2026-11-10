#!/usr/bin/env bash
set -euo pipefail
serial="${1:?Usage: scripts/android-device.sh ADB_SERIAL}"
sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"; adb="$sdk_root/platform-tools/adb"
"$adb" -s "$serial" shell getprop ro.product.model
"$adb" -s "$serial" shell getprop ro.build.version.release
"$adb" -s "$serial" shell getprop ro.build.version.sdk
"$adb" -s "$serial" shell pm list features | grep -E 'bluetooth|strongbox' || true
