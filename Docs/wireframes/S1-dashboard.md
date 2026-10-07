# S1 — Dashboard (Insights) wireframe

| | |
|---|---|
| **Document** | `Docs/wireframes/S1-dashboard.md` |
| **Issue** | EPIC-002 / T2 · S1 (#12) · Parent: #11 |
| **Screen** | SCR-10 Insights (`Docs/ux/screens.md`) |
| **Fidelity** | Low: boxes and labels only. Visual polish is M4. |
| **Failure causes from** | `Docs/Scenario analysis.md` (rows `A-xx`) |
| **Index** | `Docs/wireframes/failure-causes.md` |

**Job of the screen.** Answer "where do I stand?" the moment the app opens (FR-90). Income is irregular, so the comparison that matters is *this month vs last month*, not a budget bar (A-07).

Legend: `[n]` is a callout that matches the annotation table below. Figures are the scenario figures and appear only in documentation (CON-15). Dates are DD/MM/YYYY (NFR-20).

---

## 1. Wireframe — populated

```text
+----------------------------------------------+
| Insights   < October 2026 >   * Synced       |
+----------------------------------------------+
| [1] THIS MONTH vs LAST MONTH                 |
|                                              |
| Received   LKR 134,500                       |
|  this  ################                      |
|  last  ####################  168,000         |
| Spent      LKR 96,300                        |
|  this  ###########                           |
|  last  ############  101,400                 |
| Saved      LKR 38,200                        |
| Only money actually received counts.         |
+----------------------------------------------+
| [2] GOAL: MacBook Pro M4                     |
| [##.........................]  2%            |
| LKR 11,200 of LKR 490,000                    |
| Required / month   LKR 39,900                |
| Your pace          LKR 20,000                |
| ( Behind by LKR 19,900 / month )             |
| Projected finish   05/10/2028                |
| Deadline           07/10/2027                |
+----------------------------------------------+
| [3] COMMITTED vs DISCRETIONARY               |
| [###################|##########]             |
|  Committed 62,000   Discretionary 34,300     |
| Top categories                               |
|  1 Rent ....................... 34,000       |
|  2 Coffee and dining .......... 24,000       |
|  3 Groceries .................. 15,000       |
+----------------------------------------------+
| [4] WAITING ON PAYMENT (2)                   |
| Logo design - Milestone 2                    |
|  LKR 30,000  due 6 days ago                  |
|                    [ Mark received ]         |
| Salary expected on the 25th                  |
|                    [ Confirm ]               |
+----------------------------------------------+
| [5] Recurring: LKR 52,400 / month            |
|                          View all >          |
+----------------------------------------------+
|                      [ + Add expense ] [6]   |
+----------------------------------------------+
|  Insights | History | Income | Goal | More   |
+----------------------------------------------+
```

---

## 2. Wireframe — empty state (new account, no data)

```text
+----------------------------------------------+
| Insights   < October 2026 >   * Synced       |
+----------------------------------------------+
| THIS MONTH                                   |
| Nothing recorded this month yet.             |
| Your totals appear here as soon as you       |
| add something.                               |
+----------------------------------------------+
| GOAL                                         |
| Set a goal to see what you need to           |
| save each month.                             |
|            [ Set a goal ]                    |
+----------------------------------------------+
| COMMITTED vs DISCRETIONARY                   |
| [- - - - - - - - - - - - - - - -]            |
| Your spending split appears after            |
| your first expense.                          |
+----------------------------------------------+
| WAITING ON PAYMENT                           |
| No pending payments.                         |
+----------------------------------------------+
|            [ Add your first expense ]        |
+----------------------------------------------+
|  Insights | History | Income | Goal | More   |
+----------------------------------------------+
```

Rules for this state: neutral copy only. No red, no error, no "incomplete" or "failed" wording (FR-97, FR-106, NFR-14). After 7 or more days without an entry a one-line welcome-back banner sits above callout 1 with a one-tap action to catch-up entry (FR-105).

---

## 3. Annotations — the failure each element fixes

| Callout | Wireframe element | Cause | Failure cause (from the analysis) | How the element answers it | FRs |
|:-:|---|---|---|---|---|
| 1 | This month vs last month: received, spent, saved | A-07 | Assumed a fixed monthly budget | Irregular income makes "this month vs last month" the useful comparison. There is no budget limit line. | FR-91, FR-98 |
| 1 | "Received" counts money that has arrived | A-16 | Needs four apps to compute income; estimate of LKR 160,000–210,000 never validated | One computed figure from received entries only. | FR-57, FR-58 |
| 1 | Caption "Only money actually received counts" | A-23 | Impulsive with discretionary spending, optimistic about income, avoidant about expenses | Totals use received income only, in a neutral tone. | FR-56 |
| 1 | Figures change the moment an entry is saved | A-09 | Data went in, but no insight came out | Summaries update instantly after each entry, from the first entry onward. | FR-91, FR-96 |
| 1 | Totals exclude transfers | A-21 | Cash withdrawals, a secondary account and Binance move money between his own pots | Transfers are a separate type and never counted as income or expense. | FR-61 |
| 2 | Goal progress card, required vs pace, projected date | A-19 | Goal is emotionally present and financially invisible; does not know his savings rate | Goal progress sits on the home screen, with required saving, actual saving and status. | FR-92, FR-76, FR-77, FR-78, FR-79 |
| 3 | Committed vs discretionary split and ranked categories | A-18 | No category system; no idea of discretionary vs committed; shocked by LKR 24,000 on coffee and dining | The split view and a ranked breakdown make the pattern visible without effort. | FR-93, FR-94 |
| 4 | "Waiting on payment": overdue invoice with "Mark received" | A-13 | Forgot to chase invoices (twice); could not match payments to projects | Overdue milestones are surfaced with a one-tap action. | FR-99, FR-51 |
| 4 | Expected salary shown apart from received | A-12 | Cannot recall the exact salary deposited; bonus varies | Expected and received are kept separate. Confirming records the actual amount. | FR-99, FR-49 |
| 5 | Recurring costs summary, "View all" | A-17 | Fragmented spending; gym auto-debit; two subscriptions meant to be cancelled | One visible number for recurring costs, linked to the full list. | FR-95 |
| 6 | "+ Add expense" FAB | A-02 | Data-entry overhead exceeded his willingness | Quick-add is one tap from the first screen. | FR-30 |
| Top bar | "Synced" indicator | A-05 | No cloud synchronisation | Sync state is visible, and the app works offline. | FR-110 |
| Empty state | Calm placeholders and "Add your first expense" | A-03 | After 13 days of diligent entry and a 3-week gap, the incomplete entries felt discouraging, so he never returned | The dashboard is useful with partial data and never shows an "incomplete" or "failed" state. | FR-97, FR-105, FR-106, FR-112 |
| Whole screen | One calm screen, nothing to set up first | A-20 | Every tool was too unpleasant to open daily | The first screen answers "where do I stand?" without effort. | FR-90 |

**Lens 2 shared causes addressed:** *no insight in return for effort* (callouts 1, 3), *no connection to the goal* (callout 2), *no tolerance for lapses* (empty state).

---

## 4. Acceptance criteria (issue #12)

| Criterion | Where it is met |
|---|---|
| Shows this month's actual income vs spend | Callout 1, with last month beside each figure |
| Shows goal progress and the projected completion date | Callout 2 |
| Shows the committed vs discretionary split | Callout 3 |
| Shows overdue invoices | Callout 4 |
| Includes an empty state | Section 2 |
| Note: compare this month with last month, not a budget bar | Callout 1, no limit line |

## 5. Arithmetic used

| Figure | Value | Rule |
|---|---|---|
| Saved this month | 134,500 − 96,300 = 38,200 | BR-05 |
| Required per month | (490,000 − 11,200) ÷ 12 = 39,900 | BR-06 |
| Gap | 39,900 − 20,000 = 19,900 per month | BR-09 |
| Projected finish at 20,000 per month | 478,800 ÷ 20,000 = 23.9 months, from 07/10/2026 gives 05/10/2028 | BR-09 |
| Committed + discretionary | 62,000 + 34,300 = 96,300 = spent | BR-13 |
