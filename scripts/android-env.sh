#!/usr/bin/env bash
# Terminal-first Android setup. This script never edits shell profiles or accepts licenses.
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
case "$(uname -s)" in Darwin) ;; *) echo 'Run this setup on macOS.'; exit 1;; esac
arch="$(uname -m)"; case "$arch" in arm64|x86_64) ;; *) echo "Unsupported Mac architecture: $arch"; exit 1;; esac
printf 'Mac architecture: %s\nSDK root: %s\n' "$arch" "$sdk_root"
command -v java >/dev/null || { echo 'Install JDK 17 first, for example: brew install --cask temurin17'; exit 1; }
java -version 2>&1 | head -1
if [ ! -x "$sdk_root/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "Android command-line tools are missing. Download the macOS command-line tools from developer.android.com/studio#command-tools, then unzip so this exists:"
  echo "  $sdk_root/cmdline-tools/latest/bin/sdkmanager"
  exit 1
fi
echo 'Next, review and manually accept licenses with:'
echo "  $sdk_root/cmdline-tools/latest/bin/sdkmanager --sdk_root=$sdk_root --licenses"
echo 'Then install packages with: scripts/android-sdk-install.sh'
