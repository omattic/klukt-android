# Hardware-constrained ownership-key experiment

This is separate from the baseline wallet and currently **unsupported/inconclusive** until measured on A, B, and C. A hardware-labelled note must never silently fall back to the baseline key policy.

## Proposed policy

Each ownership state names exactly one spending public key. Its signed successor names both the next owner and the next state spending key. A hardware-constrained successor is accepted only when its attached, independently verified attestation proves the stated key algorithm and usage-count authorization under an explicitly configured trust policy. A peer-supplied label, old certificate, feature flag, or failed second use is not evidence. Reusable-key substitution is rejected because the successor signature commits to the successor key, policy identifier, and attestation digest.

The transport does not currently accept these experimental notes. Baseline notes remain ECDSA P-256 ownership chains without a hardware-use-count claim.

## Critical crash window

`setMaxUsageCount(1)` can consume a key during signing. If the process crashes after the KeyMint operation but before the signed successor is durably written, the prior owner may lose the ability to produce the output and strand the note. No crash-safe transfer claim is made. Device testing must inject a controlled debug-only crash after signing and before the Room transaction, restart, inspect the key's remaining use count where reported, and attempt recovery. A stranded state is a negative result, not a condition to bypass by downgrading.

## What must be tested on each device

* Model, build fingerprint, API level, security patch, StrongBox feature, and generated key security level.
* StrongBox key generation then explicit TEE fallback, recording exception types and software-only mode.
* Usage-count one-use behavior, first signature verification, second-use outcome, restart, reboot, and the crash window above.
* Attestation chain collection and separate validation of root, chain, authorization fields, certificate validity, and trust policy. A stale offline verifier cannot determine revocation freshness or reliable current wall time.
* Fresh key/attestation generation after an extended offline interval. Remote key provisioning resources can be exhausted; cert validity and revocation data can be stale. New arbitrary wallets therefore cannot establish fresh trust offline by assumption.

## Conflicts and privacy

A conflict is valid only when two valid owner signatures spend the same prior-state digest toward different successors. Store both signatures and their state digest, mark that note and descendants conflicted, and exclude them from spendable value. Do not choose a branch by timestamp. Disconnected groups can remain unaware indefinitely, and detection cannot undo a completed trade. Exchange only a bounded proof (the competing transfers and necessary prefix digest), not an entire wallet history. Long chains increase validation and privacy cost; v1 caps transfer history at 64.

## Soak test

Accelerated tests do not prove weeks offline. Keep A, B, and C offline from all networks for a documented real interval, reboot at least once, perform chained transfers and one reconnect-recovery attempt, then retain exported logs and exact elapsed duration. Report duration honestly.
