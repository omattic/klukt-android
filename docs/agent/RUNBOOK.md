# Runbook

Prerequisites: JDK 17, Android SDK platform 35, a connected phone with USB debugging, and Android platform tools.

```bash
./gradlew testDebugUnitTest assembleDebug connectedDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb devices -l
adb -s SERIAL shell getprop ro.build.version.release
adb -s SERIAL shell getprop ro.product.model
```

On both phones, pair the phones in Android Settings. Grant Nearby devices when Offline Notes asks. On the receiver tap **Receive**, then on the sender select a local note and the paired receiver. Compare the six-digit pairing code on both phones before accepting the transfer.

The diagnostic card reports the actual OS API level, StrongBox feature flag, and security level of an experimental ECDSA key. It does not establish a trusted balance, current device integrity, or double-spend prevention.
