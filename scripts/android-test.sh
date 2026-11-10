#!/usr/bin/env bash
set -euo pipefail
serial="${1:?Usage: scripts/android-test.sh ADB_SERIAL}"
sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"; adb="$sdk_root/platform-tools/adb"
./gradlew testDebugUnitTest lintDebug connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.guerrerocarlos.offlinenotes.AppSmokeTest
"$adb" -s "$serial" logcat -d -v threadtime '*:S' 'OfflineNotes:V' 'AndroidRuntime:E' > "build/offline-notes-${serial}.log"
echo "Logs: build/offline-notes-${serial}.log"
