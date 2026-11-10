# Decisions

* A note is issuer-signed fixed-denomination data plus an append-only sequence of signed ownership transfers. JSON only transports/stores the object. Signatures cover deterministic, byte-length-prefixed UTF-8 fields using ECDSA P-256 with SHA-256.
* The locally held note is immediately spendable after receive because each outgoing transfer adds a new signature by the receiver's Android Keystore identity key.
* Bluetooth Classic RFCOMM is the only transaction transport. Users pair devices through Android Settings first, compare a short transcript-derived code, and explicitly confirm it on both devices before a note is sent.
* This does not prevent a compromised sender from signing conflicting histories. The receiver validates only the submitted chain and local conflict state.
* A payment is an explicitly persisted outbox batch. Missing acknowledgement is an uncertain delivery outcome, not a failed payment or a license to re-spend.
* Android Keystore single-use support is exposed solely as a device diagnostic. It is not a mutable-value store and cannot make an offline payment single-spend globally.
