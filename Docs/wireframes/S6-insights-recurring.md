# S6 — Insights trends, categories and recurring costs wireframe

| | |
|---|---|
| **Document** | `Docs/wireframes/S6-insights-recurring.md` |
| **Issue** | EPIC-011 / T4 · Secondary wireframes for M3 screens (#124) |
| **Screens** | SCR-10 Insights (trends and category detail), SCR-50 Recurring costs, SCR-51 Recurring edit, DLG-06 Subscription review (`Docs/ux/screens.md`) |
| **Fidelity** | Low: boxes and labels only. Visual polish is M4. |
| **Failure causes from** | `Docs/Scenario analysis.md` (rows `A-xx`) |
| **Index** | `Docs/wireframes/failure-causes.md` |
| **Builds on** | [S1-dashboard.md](S1-dashboard.md), which holds the main SCR-10 wireframe |

**Job of the screens.** Show how the months compare and where the money goes, and put every recurring cost in one visible list. Income is irregular, so the comparison is *this month vs last month*, with a short trend, not a budget bar (A-07). Spending is split into committed and discretionary so the pattern is clear (A-18). Recurring costs, including subscriptions, are listed with one monthly total (A-17).

Legend: `[n]` is a callout that matches the annotation table below. Figures are the scenario figures and appear only in documentation (CON-15). Dates are DD/MM/YYYY (NFR-20). Amounts are drawn as whole rupees to keep the frames readable; the app formats them as `LKR 1,000.50` (NFR-20, BR-02), see §11. Today is 07/10/2026. Status is a neutral outlined chip, never a red alarm (NFR-14).

---

## 1. Wireframe — SCR-10 Insights with trends

Callouts 1, 3, 4 and 5 are the same cards as S1. Only the trends card [2] is new. It needs axes, ranges and a touch marker, which is the case ADR-0005 reserved for review (see §11 and ADR-0006).

```text
+----------------------------------------------+
| Insights   < October 2026 >                  |
+----------------------------------------------+
| [1] THIS MONTH vs LAST MONTH                 |
| Received   LKR 134,500   (last 168,000)      |
| Spent      LKR 96,300    (last 101,400)      |
| Saved      LKR 38,200                        |
| Only money actually received counts.         |
+----------------------------------------------+
| [2] TRENDS                                   |
| ( 3 months ) ( 6 months ) ( 12 months )      |
| 3 months selected                            |
| [ Received ] [ Spent ] [ Saved ]             |
|  200k|                                       |
|  150k|  #           #                        |
|  100k|  #  =        #  =        #  =         |
|   50k|  #  =  o     #  =  o     #  =  o      |
|      +---Aug---------Sep---------Oct--       |
|  # received   = spent   o saved              |
| Spending has been steady over 3 months.      |
+----------------------------------------------+
| [3] COMMITTED vs DISCRETIONARY               |
| [###################|##########]             |
|  Committed 62,000   Discretionary 34,300     |
+----------------------------------------------+
| [4] TOP CATEGORIES                           |
|  1 Rent ....................... 34,000       |
|  2 Coffee and dining .......... 24,000       |
|  3 Groceries .................. 15,000       |
|                         See all >            |
+----------------------------------------------+
| [5] Recurring: LKR 52,400 / month            |
|                          View all >          |
+----------------------------------------------+
|                      [ + Add expense ]       |
+----------------------------------------------+
|  Insights | History | Income | Goal | More   |
+----------------------------------------------+
```

### Trends detail

Tapping a month on the chart shows its figures and keeps the rest of the screen unchanged.

```text
+----------------------------------------------+
| [2] TRENDS                                   |
| ...chart as above, September selected...     |
| +------------------------------------------+ |
| | September                                | |
| | Received 168,000   Spent 101,400         | |
| | Saved 66,600                             | |
| +------------------------------------------+ |
| Toggling a chip hides or shows that series.  |
+----------------------------------------------+
```

Months with no entries are drawn as a thin empty slot, with no warning or "incomplete" label (FR-97, FR-106).

---

## 2. Wireframe — Category breakdown

Opened from "See all" in callout [4]. The list is for the selected month.

```text
+----------------------------------------------+
| < Where the money went   October 2026        |
+----------------------------------------------+
| [1]  1 Rent                    34,000   35%  |
|      [#######..............]  (Committed)    |
|      2 Coffee and dining       24,000   25%  |
|      [#####................]  (Discretionary)|
|      3 Groceries               15,000   16%  |
|      [###..................]  (Committed)    |
|      4 Ride-hailing             9,000    9%  |
|      [##...................]  (Discretionary)|
|      5 Other                   14,300   15%  |
+----------------------------------------------+
| Transfers are not included.                  |
+----------------------------------------------+
```

Tapping a row opens a bottom sheet (Coffee and dining shown):

```text
+==============================================+
|  Coffee and dining                           |
|  LKR 24,000 this month                       |
|  [2] That is 4.9% of your MacBook goal and   |
|      60.2% of one month's required saving.   |
|                                              |
|  [ See these entries in History ]            |
+----------------------------------------------+
```

---

## 3. Wireframe — SCR-10 Insights, empty state

The full empty state is in S1 section 2. With a single entry the cards show that one figure and no warning.

```text
+----------------------------------------------+
| Insights   < October 2026 >                  |
+----------------------------------------------+
| THIS MONTH                                   |
| Spent  LKR 1,200                             |
| Your totals grow as you add more.            |
+----------------------------------------------+
| TRENDS                                       |
| [- - - - - - - - - - - - - - - -]            |
| Your trend appears after a second month.     |
+----------------------------------------------+
```

---

## 4. Wireframe — SCR-50 Recurring costs

Reached from More, from "View all" in callout [5] and from the Insights pending strip.

```text
+----------------------------------------------+
| < Recurring costs                    ( + )   |
+----------------------------------------------+
| [1] EVERY MONTH                              |
| LKR 52,400                                   |
| Weekly and yearly items are shown as a       |
| monthly amount.                              |
+----------------------------------------------+
| [2] DUE NOW (1)                              |
| Internet - due 05/10/2026                    |
|  Expected LKR 3,500                          |
|                       [ Confirm ]            |
+----------------------------------------------+
| [3] ALL RECURRING                            |
| Rent        monthly, 1st        34,000  ...  |
|  (Committed)                                 |
| Gym         monthly, 15th        9,500  ...  |
|  (Subscription)                              |
| Video streaming monthly, 20th    3,500  ...  |
|  (Subscription)   Still using this? >        |
| Music streaming monthly, 20th    1,900  ...  |
|  (Subscription)                              |
| Internet    monthly, 5th         3,500  ...  |
|  (Committed)                                 |
|                                              |
| ( ... ) menu on a row: Edit, Pause, End      |
+----------------------------------------------+
| PAUSED                                       |
| Cloud storage  monthly           1,200       |
|  (Paused)                                    |
+----------------------------------------------+
| HISTORY    Rent                              |
|  Month   Expected  Actual   Variance         |
|  Oct      34,000   34,000         0          |
|  Sep      34,000   37,500    +3,500          |
|  Aug      34,000   36,200    +2,200          |
|  Only confirmed amounts are counted.         |
+----------------------------------------------+
```

The monthly total in [1] is the sum of active templates only. The paused row is excluded.

### Empty state

```text
+----------------------------------------------+
| < Recurring costs                    ( + )   |
+----------------------------------------------+
| Nothing recurring yet. Add rent, a gym or a  |
| subscription to see your fixed costs in one  |
| place.                                       |
|            [ Add a recurring cost ]          |
+----------------------------------------------+
```

---

## 5. Wireframe — SCR-51 Recurring edit

```text
+----------------------------------------------+
| < Edit recurring                             |
+----------------------------------------------+
| Name             [ Rent                  ]   |
| Category         [ Rent                v ]   |
| Expected amount  [ 34,000                ]   |
| Cadence          [ Monthly             v ]   |
|                  Weekly / Monthly / Yearly   |
| Due day          [ 1                     ]   |
| Account          [ Current account     v ]   |
| Type             ( Committed ) ( Discretionary )|
| [1] The actual amount is entered when you    |
| confirm, so a bill that varies is fine.      |
+----------------------------------------------+
|  [ Pause ]   [ End ]                         |
|  Past entries are kept if you pause or end.  |
+----------------------------------------------+
|        [ Save ]            [ Cancel ]        |
+----------------------------------------------+
```

---

## 6. Wireframe — DLG-06 Subscription review

Shown every 90 days by default (FR-69). A dialog over a dimmed SCR-50.

```text
+----------------------------------------------+
|  Still using this?                           |
|                                              |
|  Video streaming                             |
|  LKR 3,500 / month                           |
|                                              |
|  [ Keep ]                                    |
|  [ Remind me to cancel ]                     |
|  [ End ]                                     |
|                                              |
|  Ending stops future prompts and keeps your  |
|  history.                                    |
+----------------------------------------------+
```

---

## 7. Flow

```text
SCR-10 [5] "View all" --> SCR-50 --> SCR-51 (Add / Edit)
SCR-50 Confirm --> DLG-01 Confirm pending item
SCR-50 "Still using this?" --> DLG-06
SCR-10 [4] "See all" --> Category breakdown --> History (filtered)
More --> SCR-50
```

---

## 8. Annotations — the failure each element fixes

| Callout | Wireframe element | Cause | Failure cause (from the analysis) | How the element answers it | FRs |
|:-:|---|---|---|---|---|
| SCR-10 [1] | This month vs last month, no limit line | A-07 | Assumed a fixed monthly budget | Irregular income makes the month-on-month comparison the useful one. | FR-91, FR-98 |
| SCR-10 [2] | Trends: received, spent, saved over 3, 6 or 12 months | A-09 | Data went in, but no insight came out | The chart gives a return for the effort of entering data. | FR-91, FR-98 |
| SCR-10 [2] | Empty slot with no warning for a quiet month | A-03 | After 13 days of diligent entry and a 3-week gap, the incomplete entries felt discouraging, so he never returned | A gap in the chart is not shown as a failure. | FR-97, FR-106 |
| SCR-10 [3] | Committed vs discretionary split | A-18 | No category system; no idea of discretionary vs committed | The split makes the pattern visible without effort. | FR-93 |
| SCR-10 [4] / Category breakdown [1] | Ranked categories with type chips and share | A-18 | Shocked by LKR 24,000 on coffee and dining | The biggest categories are ranked and labelled, and transfers are absent. | FR-94, BR-13 |
| Category breakdown [2] | Goal cost of a spend | A-18, A-19 | Shocked by LKR 24,000 on coffee and dining; goal invisible | The amount is shown against the goal in plain, neutral words (BR-12). | FR-94, FR-76 |
| SCR-10 [5] | Recurring summary, View all | A-17 | Fragmented spending; gym auto-debit; two subscriptions meant to be cancelled | One visible number links to the full list. | FR-95 |
| SCR-50 [1] | Combined monthly total | A-17 | Fragmented spending | The total equals the sum of active templates, normalised to monthly. | FR-68 |
| SCR-50 [2] | Due now with one-tap Confirm | A-22 | Utilities make the effective monthly rent variable | A due item is confirmed with its actual amount; unconfirmed items are not counted. | FR-66 |
| SCR-50 [3] | All recurring list, subscription chips, pause or end | A-17 | Gym auto-debit; two subscriptions meant to be cancelled | Every fixed cost is visible and can be paused or ended without losing history. | FR-65, FR-68, FR-70 |
| SCR-50 History | Expected, actual and variance | A-22 | Utilities make the effective monthly rent variable | September rent shows 34,000 expected against 37,500 actual, the same pattern as salary. | FR-67 |
| SCR-51 [1] | Actual entered at confirmation | A-22 | Utilities make the effective monthly rent variable | The template holds the expected amount only. | FR-65, FR-66 |
| DLG-06 | "Still using this?" keep, remind, end | A-17 | Two subscriptions meant to be cancelled | A gentle review every 90 days, with no pressure. | FR-69 |
| Whole set | Calm wording, no red, no streaks | A-20 | Every tool was too unpleasant to open daily | The screens invite a look rather than avoidance. | FR-106, NFR-14 |

**Lens 2 shared causes addressed:** *no insight in return for effort* (SCR-10 [2], [3], [4]), *a data model that does not fit his life* (SCR-50, SCR-51), *no tolerance for lapses* (empty states).

---

## 9. Acceptance criteria (issue #124)

| Criterion | Where it is met |
|---|---|
| Low-fi wireframe covering trends | SCR-10 callout 2 and the trends detail |
| Low-fi wireframe covering the recurring costs list (A-17) | SCR-10 callout 5, SCR-50 list and total, SCR-51, DLG-06 |
| Insights category view (A-18) | SCR-10 callouts 3 and 4, the category breakdown and the goal-cost sheet |

## 10. Arithmetic used

| Figure | Value | Rule |
|---|---|---|
| Saved this month | 134,500 − 96,300 = 38,200 | BR-05 |
| Saved in September | 168,000 − 101,400 = 66,600 | BR-05 |
| Committed + discretionary | 62,000 + 34,300 = 96,300 = spent | BR-13 |
| Recurring total | 34,000 + 9,500 + 3,500 + 1,900 + 3,500 = 52,400 | FR-68 |
| Goal cost of 24,000 | 24,000 ÷ 490,000 = 4.9%, and 24,000 ÷ 39,900 = 60.2% | BR-12 |
| Rent variance (September) | 37,500 − 34,000 = +3,500 | BR-16 |
| Rent in October | confirmed at 34,000, so the category row and the 96,300 total stand | BR-16, FR-66 |
| Due now | Internet is due 05/10/2026, two days before today (07/10/2026); unconfirmed, so not counted | FR-66 |

## 11. Open points

| Point | Detail | Owner action |
|---|---|---|
| Trends card: requirement | The SRS lists the month summary, the committed split, the categories and month switching (FR-91 to FR-98) but has no requirement for a multi-month trend chart. | Confirm the trends card with the team and add an FR if it stays. |
| Trends card: chart approach | The card has a value axis, a month axis, 3/6/12-month ranges, three toggleable series and a touch marker. That is the case [ADR-0005](../adr/0005-chart-approach.md) names under *When to revisit* (axes, scrolling or touch markers). | Settle [ADR-0006](../adr/0006-trend-chart-library.md) (Proposed) before M3 build starts. It supersedes ADR-0005 if accepted. |
| Whole-rupee display | NFR-20 and BR-02 specify two decimals (`LKR 1,000.50`). The frames show whole rupees. | Settle in `failure-causes.md` §4. |
| Category list share and amounts | The Ride-hailing and Other rows are placeholders. Only Rent, Coffee and dining and Groceries are scenario figures, and they sum to 73,000 of the 96,300 spent. | Replace with real category names when the category list is fixed. |
| Gym, streaming and internet amounts | The scenario gives only rent (34,000) and the 52,400 total. The other amounts are placeholders chosen to add up to that total. | Replace if the scenario figures are defined. |
