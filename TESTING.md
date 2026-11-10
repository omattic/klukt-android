# Testing

## Host checks actually run

`./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` passed locally. This compiles the instrumentation APK but is not a physical-device result.

## Three-phone procedure

Label detected serials only after `adb devices -l`: A is Pixel 9, B is first Pixel 4, C is second Pixel 4. Use sequential USB connections if needed. Never pass an unspecified serial to an install, test, log, clear-data, or reboot command.

1. Provision an exact test note to A with `tools/issue-test-note.sh`; explicitly trust the issuer and import the file.
2. On all phones enable airplane mode, manually enable Bluetooth, leave Wi-Fi/cellular off, turn USB tethering off, and disconnect USB.
3. A transfers the note to B, then B transfers the same note to C. Compare and confirm each on-screen code.
4. Confirm normal UI no longer offers the note to A or B. Reconnect USB only for logs/tests.
5. Restart apps, then reboot each phone, and recheck durable state.
6. Run disconnect-before-send, disconnect-after-receive, and lost-acknowledgement recovery tests. Report uncertain outcomes honestly.
7. A separate debug-only adversarial fixture may create conflicting signed branches. It must never run in release. Detection shows evidence and blocks locally known conflicts, but cannot prevent a disconnected trade.

## Soak test

Keep all phones offline for a real documented interval, including reboot and chained transfer. Record actual elapsed duration. Accelerated simulation does not prove weeks.
