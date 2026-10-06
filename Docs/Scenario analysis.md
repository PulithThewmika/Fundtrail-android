# FundTrail — Scenario Analysis (Kavindu Silva)

**Issue:** EPIC-001 / T1 (#2) — sub-tasks S1 (#3) and S2 (#4)
**Source:** SE3092 Assignment 01 scenario. Section references (§) point to the scenario sections.
**Use:** The SRS cites this document. Row ids `A-xx` are stable; functional requirements (`FR-xx`) will reference them.
**Scope note:** This document states needs, not technology. Technology choices are recorded in the ADRs.

---

## Part 1 — Pain-point and tool-failure → requirement table (S1)

Each row reads: **failure cause → what FundTrail must do differently.**

### Tool 1 — Google Sheet "MacBook Fund" (§3.5.1, §3.4)

| ID | Failure cause | What FundTrail must do differently | Source |
|---|---|---|---|
| A-01 | Hand-built formulas broke after row insertions | The user never maintains formulas. All totals, ratios and progress figures are computed by the app from stored records | §3.5.1 |
| A-02 | Data-entry overhead exceeded his willingness | Quick-add entry: amount first, smart defaults (date = now, last-used account, suggested category), optional fields stay optional | §3.5.1 |
| A-03 | After 13 days of diligent entry and a 3-week gap, the incomplete entries felt discouraging, so he never returned | Gap-tolerant design: no "incomplete" or failure states, back-dating and fast catch-up, and the dashboard stays useful with partial data | §3.4, §3.5.1 |

### Tool 2 — Play Store budgeting app (§3.5.2)

| ID | Failure cause | What FundTrail must do differently | Source |
|---|---|---|---|
| A-04 | USD only | Multi-currency entries. Store original currency and amount plus the LKR value actually received, with the rate locked at entry | §3.5.2, §3.2 |
| A-05 | No cloud synchronisation | Data syncs to the cloud automatically, and entry works fully offline and syncs later. Technology choice recorded in ADR-004 | §3.5.2 |
| A-06 | Assumed a single income source | Income modelled as multiple sources (salary, freelance, AdSense, crypto), each with its own cadence and currency | §3.5.2, §3.2 |
| A-07 | Assumed a fixed monthly budget | No fixed-budget assumption. Plans and reports are driven by actual received income, which varies month to month | §3.5.2, §3.2 |

### Tool 3 — Notes app with pasted bank exports (§3.5.3)

| ID | Failure cause | What FundTrail must do differently | Source |
|---|---|---|---|
| A-08 | Pasted text became an unreadable block of unlabelled figures | Every record is structured: amount, date, category, account, type (committed or discretionary) | §3.5.3 |
| A-09 | Data went in, but no insight came out | Summaries update instantly after each entry, from the first entry onward | §3.5.3, §3.3 |

### Tool 4 — Bank's native spending summary (§3.5.4)

| ID | Failure cause | What FundTrail must do differently | Source |
|---|---|---|---|
| A-10 | Captured only card transactions; omitted cash and secondary-account activity | Account field (main bank, secondary bank, cash, Binance, other) so every channel is recorded in one place and can be reconciled per account | §3.5.4, §3.3 |
| A-11 | Miscategorised a PickMe fare as "online retail" | User-controlled categories with sensible local defaults (ride-hailing, food delivery, coffee and dining). The app suggests, the user confirms, and corrections are remembered | §3.5.4 |

### Pain points beyond the four tools

| ID | Pain point | What FundTrail must do differently | Source |
|---|---|---|---|
| A-12 | Cannot recall the exact salary deposited; bonus varies | Record the *received* amount against an expected salary on the 25th (expected vs received) | §3.2 |
| A-13 | Forgot to chase invoices (twice); could not match payments to projects | Freelance income linked to a project and milestone, with status pending, received or overdue, and a follow-up reminder once a due date passes | §3.2 |
| A-14 | AdSense paid in USD at a variable rate; never recorded | Monthly AdSense entry capturing USD amount, rate and LKR received | §3.2 |
| A-15 | No running crypto net position; cannot state year-to-date earnings | Signed entries (gain or loss) for realised results only, and a running year-to-date net position. Moving money into or out of Binance is a transfer, not income or expense | §3.2 |
| A-16 | Needs four apps to compute income; estimate of LKR 160,000–210,000 never validated | One computed "actual monthly income" figure from received entries, shown beside his own estimate | §3.2 |
| A-17 | Fragmented spending; gym auto-debit; two subscriptions meant to be cancelled | Recurring expenses (rent LKR 34,000, gym, subscriptions) as templates, with a visible list of recurring costs and a periodic "still using this?" review for subscriptions | §3.3 |
| A-18 | No category system; no idea of discretionary vs committed; shocked by LKR 24,000 on coffee and dining | Committed or discretionary tag per category (overridable), with a split view and a ranked category breakdown | §3.3 |
| A-19 | Goal is emotionally present and financially invisible; does not know his savings rate | Goal entity with computed required monthly saving, actual saving and status | §3.4 |
| A-20 | Every tool was too unpleasant to open daily | Low-friction, calm UI where the first screen answers "where do I stand?" without effort | §3.5 (closing) |
| A-21 | Cash withdrawals, a secondary account and Binance move money between his own pots | Transfers between the user's own accounts are a separate type, excluded from income and expense totals so nothing is counted twice | §3.2, §3.3 |
| A-22 | Utilities make the effective monthly rent variable | Recurring committed costs support an expected vs actual amount, the same pattern as salary | §3.3 |
| A-23 | Impulsive with discretionary spending, optimistic about income, avoidant about expenses | Neutral, non-judgemental tone; projections use received income only; the goal cost of a discretionary spend is shown when it is entered | Persona |
| A-24 | The required saving may exceed what he can realistically save | Detect an infeasible goal, show the gap, and offer to adjust the deadline or target instead of a permanent "behind" status | §3.4, Persona |

**Coverage check:** Tool 1 → A-01 to A-03; Tool 2 → A-04 to A-07; Tool 3 → A-08, A-09; Tool 4 → A-10, A-11. All four tools covered. Pain points not tied to a single tool: A-12 to A-20, plus A-21 to A-24 (transfers, variable utilities, persona traits, infeasible goal).

---

## Part 2 — The five analysis lenses (S2)

Scenario figures used throughout:

| Figure | Value |
|---|---|
| Goal (MacBook Pro M4) | LKR 490,000 |
| Saved so far | LKR 11,200 |
| Remaining | 490,000 − 11,200 = **LKR 478,800** |
| Required saving | 478,800 / 12 = **≈ LKR 39,900 per month** |
| Earned in 12 months | **over** LKR 1.6M, so at least about LKR 133,000 per month on average (a floor, not an exact figure) |
| Saved as share of earnings | 11,200 / 1,600,000 ≈ 0.7% or less |

### Lens 1 — Income structure

Kavindu has four channels with different cycles, currencies and certainty:

| Channel | Cycle | Currency | Certainty |
|---|---|---|---|
| Salary | Monthly, the 25th | LKR | High date, variable amount (LKR 120,000–140,000) |
| Freelance | Milestone-based, irregular | LKR | Low (LKR 20,000–90,000 per project) |
| AdSense | Monthly | USD converted to LKR at transfer | Medium (about LKR 3,500–19,000) |
| Crypto | Ad hoc | USDT, ETH, altcoins | Very low; can be a loss |

To report what he actually earns rather than what he estimates, the system must:

1. Treat each source as its own entity with a type, cadence and currency.
2. Count only **received** income toward actual earnings, and show **expected or pending** amounts separately (A-12, A-13).
3. Store the original amount and currency together with the **LKR value actually received** and the rate used, and never recompute history with a newer rate (A-04, A-14).
4. Allow negative crypto results, count realised results only, and keep a running net position. Moving money into or out of Binance is a transfer, not income (A-15, A-21).
5. Present a range or trailing average rather than a single number, and compare it with his estimate. The scenario gives only a floor (over LKR 1.6M, so at least about LKR 133,000 per month), which neither confirms nor refutes his LKR 160,000–210,000 estimate. That is exactly why the app must compute the real figure (A-16).

**Implication for FundTrail:** Income is a multi-source, multi-currency collection of dated entries with an expected/received status, and the app derives actual monthly income from received entries only.

### Lens 2 — Tool failures

The four failures share five behavioural and UX causes:

1. **High friction per entry** (Sheet, notes app).
2. **A data model that does not fit his life**: USD only, one income source, fixed budget, card-only (Play Store app, bank summary).
3. **No insight in return for effort** (notes app).
4. **No tolerance for lapses**: a three-week gap turned the sheet into a source of discouragement (Sheet).
5. **No connection to the goal**, so nothing made the effort feel worthwhile.

Design decisions that prevent a repeat:

- Quick-add with smart defaults (A-02).
- Flexible multi-source, multi-currency model (A-04 to A-07).
- Dashboard that responds the moment data is entered (A-09).
- Entry that works offline and syncs later (A-05).
- Gap-tolerant states and fast back-fill (A-03).
- Goal progress on the home screen (A-19).

Persona traits make this harder: he is *impulsive* with discretionary spending, *optimistic* about income and *avoidant* about expenses. So FundTrail must keep a neutral, non-judgemental tone (no red failure states or guilt), base projections on received income only, and show the goal cost of a discretionary spend at the moment it is entered (A-23).

**Implication for FundTrail:** The product's core promise is that entry costs seconds and immediately returns insight.

### Lens 3 — Savings goal

The goal is LKR 490,000 in 12 months with LKR 11,200 saved. The arithmetic the Goal screens must reproduce:

- Remaining: 490,000 − 11,200 = **LKR 478,800**
- Required: 478,800 / 12 = **≈ LKR 39,900 per month**
- Against an earning pace of at least about LKR 133,000 per month, that is **up to roughly 30% of income**, compared with the roughly 0.7% or less he has actually saved.

To make progress feel real:

1. Show the required monthly saving, recalculated as months pass and the saved amount changes (A-19).
2. Show the actual monthly saving (received income minus expenses) next to the required figure, with a status: on track, behind or ahead, and by how much.
3. Show a projected completion date at the current pace compared with the 12-month deadline.
4. Express spending in goal terms. For example, LKR 24,000 on coffee and dining in one month is about 4.9% of the MacBook, or about 60% of one month's required saving (A-23).
5. Let the user mark explicit contributions to the goal so saving feels deliberate. Actual saving is always measured as received income − expenses (transfers excluded, A-21); contributions show how much of that net is earmarked for the MacBook.
6. Detect an infeasible goal. If the required saving exceeds what he can realistically save, show the gap and offer to adjust the deadline or target instead of a permanent "behind" status (A-24).

**Implication for FundTrail:** The goal is a core entity, not a side feature. The home screen shows the required rate, the actual rate, and the gap, and every expense can be related back to the goal.

### Lens 4 — Expense entry without bank import

Because the app cannot import bank data, adherence depends on entry being almost free.

Friction reducers:

- Amount-first quick-add reachable from the home screen, with a numeric keypad.
- Category chips matching his real spending (ride-hailing, food delivery, coffee and dining, groceries, subscriptions).
- Defaults: current date and time, last-used account, suggested category.
- Recurring templates for rent (LKR 34,000, with an expected vs actual amount because utilities vary, A-22), gym and subscriptions, with a periodic "still using this?" review (A-17).
- Back-dating and batch catch-up (A-03).
- Offline entry synced later (A-05).
- Optional reminder at a time he chooses.

An expense record must capture the following to be useful at month end:

| Field | Purpose |
|---|---|
| Amount in LKR (plus original currency if foreign) | Totals and goal arithmetic |
| Date and time | Monthly grouping and patterns |
| Category | Distribution analysis |
| Expense type (committed or discretionary) | The ratio he has never seen |
| Account (main bank, secondary bank, cash, Binance, other) | Reconciliation with bank statements, coverage of channels banks miss, and transfers between accounts |
| Optional note or merchant | Recall and trust |
| Recurring flag and template id | Subscription and fixed-cost audit |

**Implication for FundTrail:** The quick-add flow is the most important screen, and the expense record carries type and account from day one so later analysis does not depend on retroactive tagging.

### Lens 5 — Financial awareness (discretionary vs committed)

Kavindu does not think in financial categories, so the views must answer plain questions. The minimum set that gives him self-awareness in the first week:

| View | Plain question it answers |
|---|---|
| Committed vs discretionary split (donut or stacked bar) | "How much of my money is spent by choice?" |
| Ranked category breakdown | "What is my biggest leak?" It would have surfaced the LKR 24,000 coffee and dining month |
| Month summary: received, spent, saved | "What did I actually earn, and what is left?" |
| Goal progress card: required vs actual saving | "Am I on track for the MacBook?" |
| Recurring costs list (secondary) | "What is quietly draining me?" |

The first four live on one dashboard that updates instantly after each entry. Anything beyond that is an enhancement, not core.

**Implication for FundTrail:** The dashboard is the product. It must work with one day of data, answer these four questions at a glance, and update instantly after each entry.

---

## Open questions

- Does crypto income count toward the goal, given it can turn negative?
- If the MacBook price changes, does the user edit the goal or create a new one?
- ~~Can an AdSense entry be saved with an estimated LKR amount until the actual credited amount is known?~~ **Resolved:** yes. It is saved as `EXPECTED` with the estimate, then flipped to `ACTUAL` with the credited LKR amount. Only `ACTUAL` counts toward totals (A-14, A-16; see `Docs/schema.md`, #17).

---

## Traceability

Scenario fact (§) → tool failure or pain point (`A-xx`, Part 1) → lens implication (Part 2) → `FR-xx` in the SRS.
