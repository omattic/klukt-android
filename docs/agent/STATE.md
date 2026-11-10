# State

Offline Notes is a local-only Android research prototype. It mints test notes, stores them in Room, and transfers signed ownership histories via paired Bluetooth RFCOMM sockets. No network permission, backend, cloud, consensus, or monetary value is involved.

Local build, lint, JVM tests, and instrumentation APK compilation pass. V1 now requires an explicitly imported issuer key, deterministic issuer and transfer signatures, and a receiver-generated nonce. No USB device is attached to this environment, so actual Pixel OS levels, Bluetooth behavior, Keystore levels, and instrumentation execution remain unverified. Current priority: run the two-device protocol on the supplied physical phones. The single-use Keystore experiment is a diagnostic only and must never be described as double-spend prevention.

Outgoing signing now reserves a note and stores an exact outbox payload first. Send completion without a receipt is uncertain, not failed. Inbox/outbox receipt tables and stated size/session bounds are present; authenticated proof-of-possession handshake, durable acknowledgement recovery, and batch transport remain the next implementation work before any money-like test should be run.
