# Offline Notes

Native Android research prototype for **test value only**. It transfers fixed-denomination, issuer-signed notes between paired phones with Bluetooth Classic RFCOMM. It has no internet permission, cloud, backend, Google Nearby, Wi-Fi router, mining, consensus, or blockchain framework.

## Security boundary and protocol

This authenticates an issuer and append-only ownership histories. It does not prevent a compromised sender from giving conflicting histories to different offline recipients. A matching code identifies the Bluetooth session only. Android Keystore, APK signatures, feature flags, and old attestation are not proof that a remote phone is currently uncompromised.

A v1 note includes version, `offline-notes-test-v1`, issuer public key, issuer-scoped serial, positive integer denomination, initial owner key, issuer signature, and transfers. Transfers commit to domain, version, network, issuer, serial, denomination, previous-state digest, recipient key, transfer ID, recipient-generated cryptographic nonce, and sender key. Canonical signed bytes are deterministic UTF-8 `name:byteLength:value\n` fields, never JSON. The JVM vector test verifies this and rejects altered/untrusted notes.

V1 pays exact existing notes only. It has no splitting, merging, change-making, or atomic multi-note payment.

## Pinned macOS command-line stack

| Component | Pin |
| --- | --- |
| JDK | Temurin/OpenJDK 17 |
| Gradle wrapper | 8.10.2, SHA-256 pinned in `gradle-wrapper.properties` |
| Android Gradle Plugin | 8.8.2 |
| Kotlin and Compose compiler | 2.0.21 |
| SDK and build tools | Android 35, 35.0.0 |
| Compose BOM / Room | 2025.02.00 / 2.6.1 |

The repository includes `gradlew`, the wrapper JAR, and a distribution checksum. Neither global Gradle nor Android Studio is required. `scripts/android-env.sh` supports Apple Silicon and Intel and never changes shell settings or accepts licenses. First dependency downloads need internet. Offline wallet operation does not. Offline builds require cached dependencies and SDK packages, then use `./gradlew --offline`.

Dependencies are deliberately limited: Compose Material 3 renders the native UI, Lifecycle/ViewModel and coroutines keep UI work off the main thread, Room supplies transactional durable state, Kotlin serialization handles bounded transport/storage JSON, and Android platform Bluetooth/Keystore provide local connection and non-exportable keys. There is no DI framework, backend SDK, analytics, Firebase, web view, or advertising dependency.

## Setup, build, and test

Install JDK 17 by a method you trust, for example `brew install --cask temurin17`. Download Android's macOS command-line tools from the official Android developer site and place `sdkmanager` at `$HOME/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager`.

```bash
scripts/android-env.sh
# Review and explicitly accept this yourself:
$HOME/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root="$HOME/Library/Android/sdk" --licenses
scripts/android-sdk-install.sh
scripts/android-doctor.sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

GitHub Actions runs the same host checks and creates a fresh debug-APK workflow artifact for every push and pull request. It does not replace physical-device testing.

For an attached, authorised USB device:

```bash
scripts/android-device.sh ADB_SERIAL
scripts/android-install.sh ADB_SERIAL
scripts/android-test.sh ADB_SERIAL
```

The final command runs instrumentation coverage and saves filtered logcat output in `build/offline-notes-ADB_SERIAL.log`.

## Local development issuer and USB provisioning

Issuer private keys never belong in the APK or repository. Create one outside the repository, copy a wallet public key from developer controls, then issue a file:

```bash
tools/issue-test-note.sh init "$HOME/.offline-notes-test-issuer.pem"
tools/issue-test-note.sh issue "$HOME/.offline-notes-test-issuer.pem" WALLET_PUBLIC_KEY_BASE64 10 ./note.note.json
```

Provision the JSON by USB or a local file transfer. Explicitly import the issuer public key in developer controls before importing its note. Unknown issuers, duplicate notes, malformed data, wrong networks, bad signatures, changed histories, and wrongly addressed notes are rejected.

## Physical transfer

Pair phones in Android Settings. Receiver grants Nearby devices and taps Receive. Sender selects a spendable note and a paired phone. Both compare and explicitly confirm the same six-digit code. Restart the app before the next transfer to check persistence. Hardware diagnostics remain experiments, not proof of payment security.
