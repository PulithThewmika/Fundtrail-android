# FundTrail — Firestore Schema

**Issue:** EPIC-003 / T2 (#17) — sub-tasks S1 collections & fields (#18), S2 multi-currency model (#19), S3 composite index plan (#20)
**Use:** Every feature reads and writes data exactly as defined here. Changing this after M2 starts means migrating live data, so §8 (evolution rules) applies from M2 onward.
**Related:** `Docs/architecture.md` (#16) for layering; offline and sync behaviour is finalised in #22; security rules are written in #21.

---

## 1. Schema diagram

![FundTrail Firestore schema](https://github.com/user-attachments/assets/636ddfac-a13b-421e-8c73-314d8566da05)

---

## 2. Collection tree

All data lives under the signed-in user. There are no top-level shared collections.

```
users/{uid}
├── accounts/{id}
├── categories/{id}
├── transactions/{id}      one collection; `type` discriminates EXPENSE | INCOME | TRANSFER
├── invoices/{id}
├── recurring/{id}
└── goals/{id}
```

Income and expenses share one `transactions` collection with a type discriminator, not separate collections, so a month summary or per-account history is one query over one collection.

---

## 3. Collections and fields

**Legend:** **Req** = required on every document of that kind; **Opt** = may be absent. Types are Firestore types as seen from Kotlin: `String`, `Long` (int64), `Int`, `Boolean`, `Timestamp`, `Map`. Enums are stored as their name string.

### 3.0 Fields on every document

| Field | Type | Req/Opt | Notes |
|---|---|---|---|
| `createdAt` | Timestamp | Req | `serverTimestamp()` on create. Immutable afterwards. |
| `updatedAt` | Timestamp | Req | `serverTimestamp()` on every write. |
| `deletedAt` | Timestamp | Opt | Set instead of deleting (soft delete, §6.4). |

### 3.1 `users/{uid}` — profile

| Field | Type | Req/Opt | Notes |
|---|---|---|---|
| `displayName` | String | Req | |
| `incomeEstimateMinLkrMinor` | Long | Req | The user's own monthly income estimate (low end), shown beside actual income (A-16). |
| `incomeEstimateMaxLkrMinor` | Long | Req | High end of the estimate. ≥ min. |
| `schemaVersion` | Int | Req | Schema version of this user's data. Only place a version is stored (§8). |

### 3.2 `accounts/{id}`

| Field | Type | Req/Opt | Notes |
|---|---|---|---|
| `name` | String | Req | e.g. "Main bank", "Cash", "MacBook fund" |
| `kind` | Enum | Req | `BANK` \| `CASH` \| `SAVINGS` \| `CRYPTO_EXCHANGE` \| `OTHER` |
| `currency` | String | Req | ISO-style code of the account's own currency (e.g. `LKR`, `USDT`). |
| `openingBalanceLkrMinor` | Long | Req | Balance when tracking started. Kavindu's LKR 11,200 goes here on his savings account. |
| `openingBalanceAt` | Timestamp | Req | Only transactions on or after this time affect the balance. |
| `archived` | Boolean | Req | Hidden from pickers, kept for history. |

### 3.3 `categories/{id}`

| Field | Type | Req/Opt | Notes |
|---|---|---|---|
| `name` | String | Req | User-controlled (A-11). |
| `defaultSpendKind` | Enum | Req | `COMMITTED` \| `DISCRETIONARY` (A-18). Copied onto each expense at entry. |
| `icon` | String | Req | Icon key. |
| `sortOrder` | Int | Req | Order in the quick-add chips. |
| `archived` | Boolean | Req | |

### 3.4 `transactions/{id}`

Common fields (all types):

| Field | Type | Req/Opt | Notes |
|---|---|---|---|
| `type` | Enum | Req | `EXPENSE` \| `INCOME` \| `TRANSFER` |
| `status` | Enum | Req | `EXPECTED` \| `ACTUAL`. Only `ACTUAL` counts toward totals (§6.2). Transfers are always `ACTUAL`. |
| `occurredAt` | Timestamp | Req | Set by the **client**, never a server timestamp — back-dating is allowed (A-03). |
| `accountId` | String | Req | Account the money came from or went to. For a transfer, the **source** account. |
| `amountLkrMinor` | Long | Req | The stored truth, in LKR minor units (§4). Positive, except signed crypto results (§6.1). |
| `original` | Map | Opt | Present only when the entry was in another currency (§4). |
| `original.currency` | String | Req if `original` | e.g. `USD`, `USDT` |
| `original.amountMinor` | Long | Req if `original` | Amount in that currency's minor units. |
| `original.fxRateMicros` | Long | Req if `original` | LKR per 1 unit × 10⁶. Display only. |
| `note` | String | Opt | Free text, merchant or memo. Not indexed (§7). |

`EXPENSE` only:

| Field | Type | Req/Opt | Notes |
|---|---|---|---|
| `categoryId` | String | Req | → `categories/{id}` |
| `spendKind` | Enum | Req | `COMMITTED` \| `DISCRETIONARY`. Copied from the category at entry; the user may override per entry. |
| `recurringId` | String | Opt | → `recurring/{id}` when generated from a template. |
| `expectedAmountLkrMinor` | Long | Opt | The expected amount, kept when an `EXPECTED` entry is confirmed with a different actual amount (e.g. rent + variable utilities, A-22). |

`INCOME` only:

| Field | Type | Req/Opt | Notes |
|---|---|---|---|
| `incomeSource` | Enum | Req | `SALARY` \| `FREELANCE` \| `ADSENSE` \| `CRYPTO` \| `OTHER` |
| `invoiceId` | String | Opt | → `invoices/{id}` when this income settles an invoice. |
| `recurringId` | String | Opt | → `recurring/{id}` (e.g. salary on the 25th). |
| `expectedAmountLkrMinor` | Long | Opt | Expected amount kept after confirming (e.g. salary with a variable bonus, A-12). |

`TRANSFER` only:

| Field | Type | Req/Opt | Notes |
|---|---|---|---|
| `toAccountId` | String | Req | Destination account. Must differ from `accountId`. |

### 3.5 `invoices/{id}`

| Field | Type | Req/Opt | Notes |
|---|---|---|---|
| `clientName` | String | Req | |
| `projectName` | String | Req | |
| `milestone` | String | Req | |
| `amountLkrMinor` | Long | Req | Invoiced amount. |
| `issuedAt` | Timestamp | Req | |
| `dueAt` | Timestamp | Req | Drives the follow-up reminder (A-13). |
| `status` | Enum | Req | `PENDING` \| `RECEIVED`. **Overdue is not stored** — it is `PENDING` and `dueAt` < now. |
| `receivedTxId` | String | Opt | → the `INCOME` transaction that settled it. Set when `RECEIVED`. |

### 3.6 `recurring/{id}`

| Field | Type | Req/Opt | Notes |
|---|---|---|---|
| `name` | String | Req | e.g. "Rent", "Salary", "Gym" |
| `type` | Enum | Req | `EXPENSE` \| `INCOME` |
| `categoryId` | String | Opt | Required when `type` = `EXPENSE`. |
| `accountId` | String | Req | |
| `expectedAmountLkrMinor` | Long | Req | |
| `cadence` | Enum | Req | `MONTHLY` \| `YEARLY` |
| `dayOfMonth` | Int | Req | 1–31; clamped to the month's last day. |
| `monthOfYear` | Int | Opt | 1–12. Required when `cadence` = `YEARLY`. |
| `active` | Boolean | Req | |
| `lastReviewedAt` | Timestamp | Opt | Last "still using this?" review for subscriptions (A-17). |

The next due date is **derived** from `cadence`, `dayOfMonth` and `monthOfYear`, not stored — a stored value goes stale and conflicts across devices.

### 3.7 `goals/{id}`

| Field | Type | Req/Opt | Notes |
|---|---|---|---|
| `name` | String | Req | e.g. "MacBook Pro M4" |
| `targetLkrMinor` | Long | Req | 49,000,000 for LKR 490,000. |
| `savingsAccountId` | String | Req | → an account with `kind` = `SAVINGS`. |
| `startedAt` | Timestamp | Req | |
| `deadline` | Timestamp | Req | |

Goal progress is the **balance of `savingsAccountId`** (§6.3), so withdrawing from savings lowers progress instead of hiding it.

---

## 4. Multi-currency model

### 4.1 Fields

| Field | Meaning |
|---|---|
| `amountLkrMinor` | LKR actually received or spent, in minor units. **The stored truth.** All totals use only this. |
| `original.currency` | Currency the user entered (`USD`, `USDT`, …). |
| `original.amountMinor` | Amount in that currency, in its minor units. |
| `original.fxRateMicros` | Rate used at entry: LKR per 1 unit × 10⁶. **Display only.** |
| `status` | `EXPECTED` while the LKR figure is an estimate; `ACTUAL` once the credited amount is known. |

These replace the field names drafted in #19: `amountMinor` / `currency` / `fxRateToLkr` live inside `original` (rate renamed `fxRateMicros` to make the scaling explicit), and `fxStatus` (`ESTIMATED` \| `ACTUAL`) is folded into the generic `status` (`EXPECTED` \| `ACTUAL`), which also covers expected salary and rent.

### 4.2 Currency decimal places

Fixed per currency in **one** code table; never stored per document.

| Currency | Decimal places | Minor units per 1 |
|---|---|---|
| `LKR` | 2 | 100 |
| `USD` | 2 | 100 |
| `USDT` | 2 | 100 |

### 4.3 Examples

| Entry | `original` | `amountLkrMinor` | `status` |
|---|---|---|---|
| Coffee, LKR 850.00 | — | `85000` | `ACTUAL` |
| AdSense estimate, USD 50.00 at 298.45 | `{USD, 5000, 298450000}` | `1492250` (LKR 14,922.50) | `EXPECTED` |
| Same AdSense, credited LKR 14,870.00 | `{USD, 5000, 298450000}` | `1487000` | `ACTUAL` |
| Crypto realised loss, USDT −12.40 | `{USDT, -1240, 300100000}` | `-372124` | `ACTUAL` |

The AdSense entry is saved as `EXPECTED` with an estimate, then flipped to `ACTUAL` with the credited LKR amount. The rate stays as entered; the credited LKR amount is what counts.

### 4.4 Crypto

- Crypto is `INCOME` with `incomeSource` = `CRYPTO`, recording **realised** profit or loss only (A-15). Unrealised holdings are not tracked.
- Results are recorded in USDT, never raw coin quantities (ETH's 18 decimals would overflow a `Long` above ~9.2 ETH).
- A loss is a negative `amountLkrMinor`. Moving money into or out of Binance is a `TRANSFER`, not income (A-21).

### 4.5 Rationale

- **Why minor units in a `Long`:** floating-point types can't represent most decimal amounts exactly, so sums drift (0.1 + 0.2 ≠ 0.3). Integer minor units add exactly. No floats for money, anywhere.
- **Why the LKR amount is frozen at entry:** the LKR value is what actually reached the account. Recomputing history with a newer rate would change past months' totals and the goal progress after the fact. Freezing it makes historical totals stable; the rate is kept only to show how the figure was reached.

---

## 5. Document IDs

| Documents | ID | Why |
|---|---|---|
| Entries generated from a recurring template | `{recurringId}_{yyyyMM}` of the due month, e.g. `rec_salary_202610` | Two devices (or a reinstall while offline) generating the same month write the **same** document instead of duplicates. Also how the app checks what has already been generated. |
| Default categories seeded at onboarding | Fixed: `cat_ride_hailing`, `cat_food_delivery`, `cat_coffee_dining`, `cat_groceries`, `cat_subscriptions`, `cat_rent`, … | Seeding twice is idempotent. |
| Default accounts seeded at onboarding | Fixed: `acc_main_bank`, `acc_secondary_bank`, `acc_cash`, `acc_binance` | Same. |
| Everything else | Firestore auto-ID | |

---

## 6. Invariants

### 6.1 Amounts
- `amountLkrMinor` > 0, **except** `INCOME` with `incomeSource` = `CRYPTO`, which may be negative.
- If `original` is present, all three of its fields are present.
- `TRANSFER`: `toAccountId` ≠ `accountId`, and `status` = `ACTUAL`.

### 6.2 What counts toward totals
- Only `status` = `ACTUAL` counts toward the month summary, actual income and actual saving (Scenario analysis, Lens 1 / A-16).
- `EXPECTED` entries and `PENDING` invoices are shown separately as upcoming or pending.
- `TRANSFER` never counts as income or expense (A-21).
- Documents with `deletedAt` set never count.

### 6.3 Account balance and goal progress

```
balance(account) = openingBalanceLkrMinor
                 + Σ ACTUAL INCOME   where accountId   = account
                 + Σ ACTUAL TRANSFER where toAccountId = account
                 − Σ ACTUAL EXPENSE  where accountId   = account
                 − Σ ACTUAL TRANSFER where accountId   = account
                 (only occurredAt ≥ openingBalanceAt, deletedAt absent)

goal progress    = balance(goal.savingsAccountId)
```

To earmark money for the MacBook without moving it between banks, Kavindu creates a "MacBook fund" `SAVINGS` account and transfers into it.

### 6.4 Writes
- **Invoice receipt is one batch:** the invoice update (`status` = `RECEIVED`, `receivedTxId`) and the new `ACTUAL` `INCOME` transaction (`invoiceId`) are written together or not at all.
- **Soft delete only:** deleting sets `deletedAt` (for undo and an audit trail). Hard deletes are forbidden by the rules. When the sync listener sees `deletedAt` set, it deletes the Room row; a hard delete arriving as `REMOVED` is handled the same way.
- `createdAt` and the document's owner never change after creation.

### 6.5 Timestamps
- `createdAt` / `updatedAt` use `serverTimestamp()`. While a write is pending (always, offline), they read back as `null` by default. Snapshots are therefore read with `ServerTimestampBehavior.ESTIMATE`, and mappers must be null-safe, so a pending write never crashes a NOT NULL Room column.
- `occurredAt` is always client-set (back-dating, A-03).

---

## 7. Index plan

### 7.1 Strategy
- **The app reads from Room, not Firestore** (`Docs/architecture.md`). Firestore is queried only by the sync listeners, which listen to each subcollection **unfiltered** (optionally `updatedAt` > last sync, which uses Firestore's automatic single-field index).
- Therefore **no composite index is used at runtime**, and none is created up front. Unused composites cost write latency and storage.
- Free-text fields are exempted from single-field indexing, since they are never queried.
- A composite index is added only when a real Firestore query needs one; the Firestore error message gives its exact definition, which is then added here and to the file below.

### 7.2 Indexes

| Index | Query it serves |
|---|---|
| Automatic single-field on every non-exempt field | Sync listener per subcollection (unfiltered, or `updatedAt` > last sync) |
| *(no composites)* | — |

### 7.3 Draft `firestore.indexes.json`

Deployed with the Firebase CLI in EPIC-006 / T2 (#40).

```json
{
  "indexes": [],
  "fieldOverrides": [
    { "collectionGroup": "transactions", "fieldPath": "note",        "indexes": [] },
    { "collectionGroup": "invoices",     "fieldPath": "clientName",  "indexes": [] },
    { "collectionGroup": "invoices",     "fieldPath": "projectName", "indexes": [] },
    { "collectionGroup": "invoices",     "fieldPath": "milestone",   "indexes": [] }
  ]
}
```

An empty `indexes` array in a field override exempts that field from indexing.

---

## 8. Evolution rules (from M2 onward)

- **Additive only.** Never rename or retype a field; add a new one instead.
- **Every new field is optional**, with a default in the mapper.
- **Enums are stored as name strings**, never ordinals. An unknown value maps to a fallback, so an older app version doesn't crash on a newer value.
- `schemaVersion` lives on the user document only; per-document versions aren't needed at this scale.

---

## 9. Security constraints (implemented in #21)

- Owner-only: every read and write under `users/{uid}` requires `request.auth.uid == uid`.
- `allow delete: if false` on every subcollection — soft delete only.
- `createdAt` is immutable on update.
