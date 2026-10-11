# 0002. Firestore structure: user subcollections and one `transactions` collection

- **Status:** Accepted
- **Date:** 2026-10-08
- **Deciders:** @PulithThewmika

## Context

Kavindu's money moves through several channels: salary, freelance milestones, AdSense, crypto, cash and transfers between his own pots (A-06, A-10, A-21). His dashboard needs one month summary and per-account balances that include all of them (A-01, A-09). The data is private to him, the rules file is a marked deliverable (CON-10), and Firestore is the primary store (CON-09).

## Decision

We will store all data under `users/{uid}` in subcollections (`accounts`, `categories`, `transactions`, `invoices`, `recurring`, `goals`), with income, expenses and transfers in one `transactions` collection discriminated by `type`, as defined in `Docs/schema.md`.

## Options considered

| Option | Pros | Cons |
|--------|------|------|
| A (chosen): `users/{uid}/…` subcollections, one `transactions` collection | Ownership is in the path, so one recursive owner rule (`users/{uid}/{path=**}`) secures everything. A month summary or account history is one query over one collection. | Cross-user queries are impossible, which this app never needs. One collection holds three record shapes, so the mapper must branch on `type`. |
| B: Top-level collections with a `uid` field | Simple flat paths. | Every rule must check `resource.data.uid` on reads and `request.resource.data.uid` on writes, and every query must filter on `uid`. One missed filter or rule leaks another user's money data. |
| C: Separate `incomes`, `expenses` and `transfers` collections | Each collection has a single shape. | The month summary and account balance need three queries merged in code. A transfer touches two accounts across collections. Converting an entry's type means moving it between collections. |

## Consequences

- **Positive:** The security rules stay short and easy to test, because owner-only is one recursive match under `users/{uid}` (#21). Balance and goal progress are one formula over one collection (`Docs/schema.md` §6.3). Sync uses one listener per subcollection (ADR-0004).
- **Negative / trade-offs:** Type-specific fields are optional at the database level, so invariants (`toAccountId` ≠ `accountId`, positive amounts) are enforced in the mapper, the use cases and the rules.
- **Follow-ups:** Composite indexes are added only when a real query needs one (`Docs/schema.md` §7).
