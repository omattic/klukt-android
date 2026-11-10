# Security

## Scope

The baseline wallet stores non-exportable Android Keystore P-256 identity keys, verifies issuer and ownership signatures, uses Bluetooth RFCOMM, and records local durable state. It is research software for test notes only.

## It provides

* Integrity checks for the submitted note history and explicit issuer trust.
* Local duplicate rejection, note reservation before transmission, and persistence over app restart/reboot while app data and keystore keys remain present.
* A manually compared Bluetooth session code, which reduces accidental peer mismatch only.

## It does not provide

* Global double-spend prevention, consensus, finality, atomic exchange, guaranteed delivery, fresh remote integrity, or recovery from permanently lost devices.
* Proof that pairing, a feature flag, APK signature, or old attestation means a remote phone is currently uncompromised.
* Safe hardware single-use payment semantics. A use-limited key can be consumed before its signed output is persisted, stranding a note after a crash.

Hardware-constrained notes are an unsupported separate experiment. They must never downgrade into the baseline policy. Fresh offline attestation can be limited by remote key provisioning, exhausted resources, cert validity/revocation staleness, trust-root policy, and unreliable clocks.

## Data loss

Android backup is disabled. Clearing app data, reinstalling, losing the phone, or losing its Keystore key can lose access to notes. USB is only for installation, diagnostics, logs, and provisioning, never a payment transport.
