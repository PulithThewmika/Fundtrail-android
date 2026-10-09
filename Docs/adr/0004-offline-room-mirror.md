# 0004. Offline: Firestore cache for writes, Room mirror for reads

- **Status:** Accepted
- **Date:** 2026-10-08
- **Deciders:** @PulithThewmika

## Context

Kavindu logs spending wherever he is, often with patchy signal, and his previous budgeting app had no sync at all (A-05). A save must never fail or wait because he is offline, and no screen may go blank on a network failure (CON-11, CON-13). Firestore is the primary store (CON-09), and the app must read his whole history fast enough for an instant dashboard (A-09, NFR-02).

## Decision

We will treat Firestore as the source of truth and write only to Firestore, relying on its local cache as the offline write queue. The UI reads from a Room mirror that one-way sync listeners keep up to date (Firestore → Room), as defined in `Docs/architecture.md` §4.

## Options considered

| Option | Pros | Cons |
|--------|------|------|
| A (chosen): Firestore cache + Room mirror | Writes are queued on disk and survive restarts with no custom queue. Room gives typed, indexed SQL reads (month windows, sums, joins), so screens work offline. One write path keeps Room from becoming a second source of truth. | Two stores means a sync path to build and test. Room schema changes are destructive (refilled from Firestore). |
| B: Firestore offline cache alone | Least code, a single store. | No joins and no SQL aggregation over the cache: month summaries, balances and goal progress must load the raw documents and sum them in memory on every change. |
| C: Room as the source of truth, synced up to Firestore | Full offline control and fast reads. | Needs a hand-built outbound queue, retries and conflict resolution between devices, which is the hardest part of sync, all to replace what Firestore already provides. Data entered on another device has no clear owner. |

## Consequences

- **Positive:** Entry works the same online and offline (A-05). Screens always render from Room, with no blank screen. Firestore's last-write-wins per field resolves conflicts.
- **Negative / trade-offs:** Two stores means a sync path to keep correct: one listener per subcollection, applied to Room in one transaction per snapshot. Room's tables mirror Firestore and need updating with every schema change.
- **Follow-ups:** Logout must clear the mirror, not only sign out: stop the listeners, sign out, clear Firestore's cache, then clear Room (`Docs/architecture.md` §4.7), implemented in #51.
