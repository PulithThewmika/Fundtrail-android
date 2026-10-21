# Income & invoices — expected vs received

**Epic:** EPIC-014 · **Requirements:** A-12, A-13, A-16, A-07 · **Schema:** [§3.5 invoices](../schema.md), [§6.4 writes](../schema.md)

> **Status:** the invoice data layer, including the atomic *mark received*, is merged (#183). The Income screen (#186), the expected-salary card (#189), overdue reminders (#191) and actual income vs estimate (#194) are in progress. The owners update this section and add screenshots when they merge.

## Problem

Kavindu forgot to chase two invoices and could not match payments to projects (**A-13**). He cannot recall the exact salary deposited, because the bonus varies (**A-12**). His "LKR 160,000–210,000 a month" estimate was never checked against reality (**A-16**). His income varies, so nothing should assume a fixed budget (**A-07**).

## Design decision

- **Invoices are their own collection** (`users/{uid}/invoices/{id}`): client, project, milestone, amount and currency, due date, and status `PENDING` or `RECEIVED`.
- **Overdue is derived, never stored**: an invoice is overdue when `PENDING` and `dueDate < today`. Nothing has to flip a flag at midnight, and offline is always consistent.
- **Marking an invoice received is one atomic batch** (schema §6.4). The invoice becomes `RECEIVED` with `receivedTxId`, and the settling `ACTUAL` `INCOME` transaction (with `invoiceId`) is written, or neither is. The invoice side uses `update()`, so a missing invoice fails the whole batch instead of creating a blank one. A payment links to exactly one milestone (BR-15).
- **Only received money counts.** Expected salary, pending invoices and estimates are shown separately and never enter the month's income or the goal pace (A-07, architecture §3.5).
- **Expected salary on the 25th** (#189) comes from settings: the expected amount and the payday. `GetSalaryStatusUseCase` returns one of:
  - `NOT_DUE`;
  - `DUE`, which shows a "Salary day: confirm the amount" card; or
  - `RECEIVED`, which shows the difference neutrally, e.g. "LKR +8,000 vs expected".
- **Reminders** (#191): a daily WorkManager job reads **Room**, not Firestore, so it works offline. It notifies about overdue invoices at most every 3 days, recorded in `lastNotifiedAt`.
- **Actual vs estimate** (#194): the trailing 3 complete months of received income against his own min/max estimate, reported as BELOW, WITHIN or ABOVE.

## Data flow

```text
Income screen ─ IncomeViewModel (#186) ─┬─ invoices use case (#186) ── InvoiceRepository ── Room invoices
                                        ├─ GetSalaryStatusUseCase (#189) ── TransactionRepository, SettingsRepository
                                        └─ GetCryptoYtdUseCase (#180)
Mark received ─ mark-received use case (#186) ── InvoiceRepository.markReceived ── Firestore WriteBatch (invoice + income)
Reminder ─ InvoiceReminderWorker (#191) ── InvoiceRepository (Room) ── notification ── deep link to the invoice
```

## Edge cases handled

| Case | Behaviour |
|---|---|
| Marking an invoice received twice (double tap) | The repository refuses an invoice that Room already shows as `RECEIVED`. A second device is a known gap: there's a TODO in the rules for a PENDING→RECEIVED-only transition. |
| A foreign-currency invoice | It must settle with the original amount and rate in that currency (ADR-0003). |
| Editing an invoice from a stale copy | The update map leaves out `status`, `receivedTxId`, `receivedAt` and `lastNotifiedAt`, so an edit can't un-receive an invoice. |
| An invoice without `dueAt` | Rejected on read and logged. It is never given a made-up date that would hide it from "overdue". |
| Two salary entries in one month (bonus paid separately) | Both count as received; the salary status compares their sum (#189 AC). |
| Fewer than 3 complete months | Income vs estimate averages what exists and says so (#194 AC; gap-tolerant, A-03). |

## Screenshots

| |
|---|
| ![Income screen with pending, overdue and received invoices](img/income-invoices.png) |
| Income screen on seeded data: one received and one overdue invoice. Captured once #186 merges. Until then the figure is the S4 wireframe ([S4-income-invoices.md](../wireframes/S4-income-invoices.md)). |

## Tests

- `InvoiceRepositoryImplTest`: mark received, the double-tap guard, and the foreign-currency rule.
- `DtoMappingTest` `Invoice*`: schema field names, the create and update maps, and a missing `dueAt`.
- `SyncManagerTest`: invoices count toward the initial-sync gate.
