# Protocol v1

`offline-notes-test-v1` is a test-only, fixed-denomination bearer-note experiment. It is neither production money nor a double-spend-proof system.

## Encoding and validation

Issuer and ownership records serialize as JSON only for storage/transport. Signatures cover canonical UTF-8 records: `name:byteLength:value\n`, fixed field order, ECDSA P-256, SHA-256. Each note has a trusted issuer key, issuer serial, denomination, initial owner key, and issuer signature. Each transfer commits to protocol/network, note identity, denomination, previous state digest, sender, recipient, recipient-generated nonce, and transfer ID.

Reject wrong network/version, untrusted issuer, malformed input, invalid signature, changed denomination/owner/history, duplicate note, repeated transfer ID, wrong recipient, or more than 64 transfers. V1 pays one exact existing note only. It has no change, splitting, merging, or atomic multi-note payment.

## State and recovery

`SPENDABLE -> RESERVED -> SIGNED -> SENT -> ACKED` is one-way. Before Bluetooth transmission, Room reserves the note and stores its exact signed payload in the outbox. A retry must resend the same payload and transfer ID. No acknowledgement means `UNCERTAIN`, not failed, and does not restore spending ability. Receiver acceptance is idempotent and must bind a durable acknowledgement to the payload digest. Device loss can leave an uncertain outcome permanently.

Bluetooth pairing is only a transport prerequisite. Wallet-key proof and a transcript-derived short authentication string are required design goals, but the current implementation is not yet a production-reviewed authenticated handshake. Do not use it for value.

## Privacy and conflicts

A conflict requires two valid owner signatures over the same previous-state digest and different successors. A report without both signatures is not evidence. A known conflict excludes that note and descendants from spendable balance. No timestamp selects a winner; disconnected groups can stay unaware indefinitely, and detection cannot undo a trade.
