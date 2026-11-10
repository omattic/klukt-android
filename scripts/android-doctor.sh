#!/usr/bin/env bash
set -euo pipefail
sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
adb="$sdk_root/platform-tools/adb"
java -version 2>&1 | head -1
./gradlew --version | sed -n '1,18p'
[ -x "$adb" ] || { echo "adb missing under $sdk_root"; exit 1; }
"$adb" devices -l
