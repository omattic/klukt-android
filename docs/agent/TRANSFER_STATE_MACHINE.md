# Persisted transfer state machine

`RESERVED -> SIGNED -> SENT -> ACKED` is one-way. `SENT` without a matching durable acknowledgement is `UNCERTAIN`, never “failed” and never spendable again.

1. A Room transaction reserves all selected note IDs.
2. The app constructs, signs, serializes, hashes, and persists the exact wire payload and transfer IDs in an outbox row before opening Bluetooth.
3. A retry reuses only that payload and IDs. It never signs another ownership transfer.
4. The receiver validates the entire batch in one Room transaction. It either writes all unseen notes plus an inbox/receipt acknowledgement for the batch digest, or writes none. Replaying the same digest returns the durable acknowledgement and cannot credit twice.
5. The sender marks `ACKED` only after verifying a receiver acknowledgement over the exact batch digest. A lost acknowledgement leaves an `UNCERTAIN` outbox record that reconnect recovery may query.

No outcome is atomic across two devices. A permanently lost device can leave an uncertain payment forever. A recipient can spend its durably accepted note while offline; the sender must not infer that it still owns a note merely because its own acknowledgement is missing.

Bounds: protocol sessions expire after 120 seconds, attempts are capped at three per outbox item, handshake frames are at most 8 KiB, payment frames at most 256 KiB, batches at most 16 notes, and individual histories at most 64 transfers. Peer bytes are length-bounded before parsing.
