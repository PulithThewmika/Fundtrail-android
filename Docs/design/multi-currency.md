# Multi-currency — income that arrives in USD and USDT

**Epic:** EPIC-013 · **Requirements:** A-04, A-14, A-15, A-06 · **Decisions:** [ADR-0003 money and FX](../adr/0003-money-and-fx.md), schema [§4](../schema.md)

> **Status:** the data model and storage are merged (M2, ADR-0003). The entry UI (#174), the confirm-estimate flow (#177), the last-used rate (#179) and crypto year-to-date (#180) are in progress. The owner updates this section and adds screenshots when they merge.

## Problem

Kavindu's earlier tracker was "USD only" (**A-04**). His AdSense arrives in USD at a rate that changes every month, and he never recorded it (**A-14**). His Binance results are in USDT and can be losses (**A-15**). Income comes from several sources, each with its own currency (**A-06**). He needs every figure in LKR without any history changing when rates move.

## Design decision

- **The LKR amount is the truth.** Every transaction stores `amountLkrMinor`, the LKR actually received, in minor units (`Long`). An optional `original` block stores the original currency, the amount in its minor units, and the rate as `fxRateMicros` (rate × 10⁶, an integer).
- **The rate is locked at entry.** Totals, summaries and the goal only ever read `amountLkrMinor`. History is never recalculated with a newer rate (ADR-0003). Correcting an estimate edits that one entry only (#177).
- **Supported currencies:** LKR, USD and USDT. The Firestore rules enforce this set (`validOriginal`).
- **Two ways to enter a foreign amount** (#174):
  - type the **LKR received**, and the rate is derived; or
  - type the **rate**, and the LKR value is derived.

  Both go through a domain `FxConversion` helper in `BigDecimal` with HALF_UP, never `Double`.
- **Estimates:** an AdSense figure entered before the payout is known is an estimate. When the actual LKR arrives, editing it with "LKR received" makes it ACTUAL (#177). Until #177 lands, the demo seeder stores the estimate as `EXPECTED`.
- **Crypto results are signed.** A realised gain is positive and a realised loss is negative; crypto income is the only income allowed below zero. Moving money into or out of Binance is a **transfer**, never income (A-21).
- **Last-used rate per currency** goes in Preferences DataStore (#179). It is small, non-sensitive settings data that needs no sync or query, so it doesn't belong in Room or Firestore.

## Data flow

```text
Quick add (Income tab) ── QuickAddViewModel ── AddIncomeUseCase ── FxConversion (pure) ── TransactionRepository ── Firestore (write)
                                                                                                    │
Dashboard / Insights ─── *ViewModel ── GetMonthlySummaryUseCase ── TransactionRepository ── Room ◄──┘ sync listener
Income screen ────────── IncomeViewModel ── GetCryptoYtdUseCase (#180) ── TransactionRepository ── Room
```

## Edge cases handled

| Case | Behaviour |
|---|---|
| Rate changes next month | Earlier entries keep their locked `amountLkrMinor`; nothing is recomputed. |
| A crypto loss | Negative `amountLkrMinor` and negative original amount; validation allows a negative only for crypto income. |
| Rounding | Half-cent results round HALF_UP once, at entry. For example, USD 48.20 × 298.40 = LKR 14,382.88. |
| An LKR entry | No `original` block; the rate is implicitly 1.0 and the status ACTUAL (#174 AC). |
| A foreign-currency invoice settles | It must carry the original amount and rate in the same currency as the invoice (`InvoiceRepositoryImpl.markReceived`). |

## Screenshots

| |
|---|
| `TODO(M4)` img/multi-currency-income-entry.png — Quick add, Income tab with USD |
| Quick add, Income: an AdSense entry in USD with its locked rate. Captured on seeded data once #174 merges. Until then the figure is the S2 wireframe ([S2-quick-add.md](../wireframes/S2-quick-add.md)). |

## Tests

- `DtoMappingTest` `OriginalAmount`: the map and round-trip.
- `TransactionTest`: crypto sign rules.
- #174: `FxConversion` tests (52.30 USD × 297.45 = LKR 15,556.64; rate derived from LKR; half-cent rounding).
- #180: year-to-date tests.
