# Offline Notes

Minimal, native Android research prototype for face-to-face transfer of signed **test-only** notes. It has no INTERNET permission and no service dependency.

## What works

* Mint a local 10-unit test note, persist it with Room, and retain it over app restarts and phone reboots.
* Transfer a signed ownership history over a paired Bluetooth Classic RFCOMM connection.
* Validate every signature before accepting a received note. A received note becomes the recipient's locally transferable note.
* Require both people to confirm a transcript-derived six-digit code before the note travels.
* Report the actual Android API level, StrongBox feature flag, and Android Keystore identity-key security level, with an optional experimental usage-count probe.

## Critical limitations

This is not money, a blockchain, or a double-spend solution. A malicious or compromised sender can create conflicting signed histories for the same note. The recipient validates only the submitted history and its own local database. Bluetooth pairing and a matching code identify the session counterpart, not a globally unique spend.

Android Keystore protects a private key according to a device's actual capabilities. It is not programmable secure-element storage, does not contain a trusted mutable balance, and usage-count experiments do not make an offline note globally single-use. No attestation, APK signature, feature flag, or old device evidence proves that a remote phone is currently uncompromised.

## Build without Android Studio

Install JDK 17 and Android SDK Platform 35, then run:

```bash
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest
```

The debug app is `app/build/outputs/apk/debug/app-debug.apk`.

## Physical-device procedure

Connect the Pixel phones by USB, unlock each, enable Developer options and USB debugging, then report the output of:

```bash
adb devices -l
for serial in $(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }'); do
  adb -s "$serial" shell getprop ro.product.model
  adb -s "$serial" shell getprop ro.build.version.release
  adb -s "$serial" shell getprop ro.build.version.sdk
  adb -s "$serial" shell pm list features | grep -E 'strongbox|bluetooth' || true
done
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Pair two phones in Android Settings. Grant Nearby devices to Offline Notes. On the receiver tap **Receive**. On the sender mint a note, tap **Send to paired phone**, select the same note, choose the paired receiver, compare the displayed code, then tap **Confirm matching code on both phones**. The receiver should show the test note. Repeat from that receiver to a third phone, then force-stop/reopen the app before repeating to demonstrate persistence.
