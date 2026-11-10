#!/usr/bin/env bash
# Does not accept licenses. Run sdkmanager --licenses yourself first.
set -euo pipefail
sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
sdk="$sdk_root/cmdline-tools/latest/bin/sdkmanager"
[ -x "$sdk" ] || { echo 'Run scripts/android-env.sh first.'; exit 1; }
"$sdk" --sdk_root="$sdk_root" 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'
