# Runbook

Prerequisites: JDK 17, Android SDK platform 35 and build tools 35.0.0, a connected phone with USB debugging, and Android platform tools. On macOS run `scripts/android-env.sh`; it never edits shell configuration or accepts licenses. Manually run the printed `sdkmanager --licenses`, then `scripts/android-sdk-install.sh`.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
scripts/android-doctor.sh
scripts/android-device.sh SERIAL
scripts/android-install.sh SERIAL
scripts/android-test.sh SERIAL
```

Use `tools/issue-test-note.sh` outside the APK repository to make a provisioned test note for a wallet public key. Explicitly trust that issuer in developer controls, then import the note file. On both phones, pair phones in Android Settings and grant Nearby devices. On the receiver tap **Receive**, then on the sender select a local note and paired receiver. Compare and explicitly confirm the six-digit code on both phones before the note travels.

The diagnostic card reports the actual OS API level, StrongBox feature flag, and security level of an experimental ECDSA key. It does not establish a trusted balance, current device integrity, or double-spend prevention.

Every GitHub push and pull request runs `.github/workflows/android.yml`: wrapper validation, JVM tests, lint, APK packaging, and debug APK/report artifacts. Successful push builds also create a uniquely tagged GitHub Release containing `app-debug.apk`.
