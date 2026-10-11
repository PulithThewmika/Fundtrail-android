# Architecture Decision Records

We record significant architectural decisions here so the "why" is not lost.
Each ADR is short (one page) and numbered with four digits (`0001`, `0002`, ...).

## How to add an ADR

1. Copy [`0000-template.md`](0000-template.md) to `NNNN-short-title.md` (next free number, kebab-case).
2. Fill in Context, Decision, Options considered and Consequences. Keep it to one page.
3. Set the status and add a row to the index below.
4. Never edit an accepted ADR to change the decision. Write a new ADR and mark the old one `Superseded by ADR-NNNN`.

## Index

| # | Title | Status | Date |
|---|-------|--------|------|
| [0001](0001-mvvm-usecases-hilt.md) | MVVM with a use-case layer and Hilt | Accepted | 2026-10-08 |
| [0002](0002-firestore-structure.md) | Firestore structure: user subcollections and one `transactions` collection | Accepted | 2026-10-08 |
| [0003](0003-money-and-fx.md) | Money as `Long` minor units, with the LKR value frozen at entry | Accepted | 2026-10-08 |
| [0004](0004-offline-room-mirror.md) | Offline: Firestore cache for writes, Room mirror for reads | Accepted | 2026-10-08 |

> [Scenario analysis](../Scenario%20analysis.md) (A-05) and the [SRS](../srs.md) refer to ADR-004 for the sync decision. ADR-004 means ADR-0004 in our four-digit numbering.