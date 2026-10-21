# S4 — Income sources and freelance invoices wireframe

| | |
|---|---|
| **Document** | `Docs/wireframes/S4-income-invoices.md` |
| **Issue** | EPIC-011 / T4 · Secondary wireframes for M3 screens (#124) |
| **Screens** | SCR-12 Income, SCR-30 Income source edit, SCR-31 Freelance projects, SCR-32 Project detail, SCR-33 Project edit, DLG-01 Confirm pending item, DLG-02 Income estimate (`Docs/ux/screens.md`) |
| **Fidelity** | Low: boxes and labels only. Visual polish is M4. |
| **Failure causes from** | `Docs/Scenario analysis.md` (rows `A-xx`) |
| **Index** | `Docs/wireframes/failure-causes.md` |

**Job of the screens.** Show what has actually arrived against what is expected, and never let an unpaid invoice go quiet. Expected salary and milestones are kept apart from received income (A-12). A milestone becomes *overdue* by itself once its due date passes (A-13, BR-15).

Legend: `[n]` is a callout that matches the annotation table below. Figures are the scenario figures and appear only in documentation (CON-15). Dates are DD/MM/YYYY (NFR-20). Amounts are drawn as whole rupees to keep the frames readable; the app formats them as `LKR 1,000.50` (NFR-20, BR-02), see §12. Today is 07/10/2026. Status is a neutral outlined chip, never a red alarm (NFR-14).

---

## 1. Wireframe — SCR-12 Income (populated)

```text
+----------------------------------------------+
| Income    < October 2026 >                   |
+----------------------------------------------+
| [1] RECEIVED THIS MONTH                      |
| LKR 134,500                                  |
| 3-month average   LKR 150,200                |
| 6-month range     LKR 98,000 - 168,000       |
| Your estimate     LKR 160,000 - 210,000      |
|                              ( Edit )        |
| Only money actually received counts.         |
+----------------------------------------------+
| [2] EXPECTED vs RECEIVED                     |
| Salary (25th)                                |
|  Expected  130,000   Received  134,500       |
|  Variance  +4,500                            |
| ---------------------------------------      |
| Salary (25th), next                          |
|  Expected  130,000   Received  -             |
|                         [ Confirm ]          |
+----------------------------------------------+
| [3] WAITING ON PAYMENT (2)                   |
| Logo design - Milestone 2                    |
|  LKR 30,000  due 6 days ago   ( Overdue )    |
|                    [ Mark received ]         |
| Web app - Milestone 2                        |
|  LKR 45,000  due in 5 days    ( Pending )    |
|                    [ Mark received ]         |
+----------------------------------------------+
| [4] BY SOURCE, this month                    |
| Salary ........................ 134,500      |
| Freelance ......................      0      |
| AdSense ........................      0      |
|   (AdSense LKR 18,000 expected, not counted) |
| Crypto, year to date net      +50,000        |
+----------------------------------------------+
| [5] Income sources >                         |
|     Freelance projects >                     |
+----------------------------------------------+
|                  [ + Record income ]         |
+----------------------------------------------+
|  Insights | History | Income | Goal | More   |
+----------------------------------------------+
```

The income total in [1] is the sum of the rows in [4]. Anything in [2] or [3] that has not been confirmed is shown but not added.

---

## 2. Wireframe — SCR-30 Income source edit

```text
+----------------------------------------------+
| < Income source                              |
+----------------------------------------------+
| Name        [ Salary                    ]    |
| Type        [ Salary                  v ]    |
|             Salary / Freelance / AdSense /   |
|             Crypto / Other                   |
| Cadence     [ Monthly                 v ]    |
| Currency    [ LKR                     v ]    |
| [1] Expected amount (optional)               |
|             [ 130,000                   ]    |
| Expected day (optional)                      |
|             [ 25                        ]    |
| Used to create an expected entry. It is not  |
| counted as income until you confirm what     |
| arrived.                                     |
+----------------------------------------------+
|        [ Save ]            [ Cancel ]        |
+----------------------------------------------+
```

---

## 3. Wireframe — SCR-31 Freelance projects

```text
+----------------------------------------------+
| < Freelance projects                         |
+----------------------------------------------+
| All | Pending | Overdue | Received           |
+----------------------------------------------+
| [1] Logo design        Client: Nova Cafe     |
|     Total agreed LKR 90,000                  |
|     [Received] [Overdue] [Pending]           |
+----------------------------------------------+
|     Web app            Client: Ceylon Tours  |
|     Total agreed LKR 120,000                 |
|     [Received] [Pending] [Pending]           |
+----------------------------------------------+
|     Brand guide        Client: -             |
|     Total agreed LKR 40,000                  |
|     [Received] [Received]                    |
+----------------------------------------------+
|                    [ + New project ]         |
+----------------------------------------------+
```

### Empty state

```text
+----------------------------------------------+
| < Freelance projects                         |
+----------------------------------------------+
| No projects yet. Add one to track what       |
| clients owe you.                             |
|            [ + New project ]                 |
+----------------------------------------------+
```

---

## 4. Wireframe — SCR-32 Project detail

Target of the overdue-milestone notification (FR-53).

```text
+----------------------------------------------+
| < Logo design                  ( Edit )      |
+----------------------------------------------+
| [1] Client        Nova Cafe                  |
|     Total agreed  LKR 90,000                 |
|     [#######...............]                 |
|     Received 30,000 of 90,000                |
+----------------------------------------------+
| [2] MILESTONES                               |
| Milestone 1   LKR 30,000                     |
|   Received 22/09/2026     ( Received )       |
|   View income entry >                        |
| ---------------------------------------      |
| Milestone 2   LKR 30,000                     |
|   Due 01/10/2026 - 6 days ago  ( Overdue )   |
|                    [ Mark received ]         |
| ---------------------------------------      |
| Milestone 3   LKR 30,000                     |
|   Due 30/10/2026               ( Pending )   |
|                    [ Mark received ]         |
+----------------------------------------------+
| One reminder at most each week for an        |
| overdue milestone.                           |
+----------------------------------------------+
```

---

## 5. Wireframe — SCR-33 Project edit

```text
+----------------------------------------------+
| < New project                                |
+----------------------------------------------+
| Project name   [ Logo design            ]    |
| Client (opt.)  [ Nova Cafe              ]    |
| Total agreed   [ 90,000                 ]    |
+----------------------------------------------+
| [1] MILESTONES                               |
| [ Milestone 1 ] [ 30,000 ] [ 22/09/2026 ] (x)|
| [ Milestone 2 ] [ 30,000 ] [ 01/10/2026 ] (x)|
| [ Milestone 3 ] [ 30,000 ] [ 30/10/2026 ] (x)|
|                          [ + Add milestone ] |
| Milestones total 90,000 of 90,000            |
+----------------------------------------------+
|        [ Save ]            [ Cancel ]        |
+----------------------------------------------+
```

---

## 6. Wireframe — DLG-01 Confirm pending item

Opened from SCR-12 (salary, AdSense, milestone), SCR-32 and the Insights pending strip. Shown as a dialog over a dimmed screen.

```text
+----------------------------------------------+
|  Confirm received                            |
|                                              |
|  Logo design - Milestone 2                   |
|  Expected          LKR 30,000                |
|                                              |
|  Amount received   [ 30,000          ]       |
|  Date              [ 07/10/2026      ]       |
|  Account           [ Savings         v ]     |
|                                              |
|  [1] Variance vs expected   0                |
|                                              |
|  Creates an income entry linked to this      |
|  project and milestone.                      |
|                                              |
|       [ Confirm ]        [ Cancel ]          |
+----------------------------------------------+
```

Salary variant: item "Salary (25th)", Expected 130,000, Amount received prefilled and edited to 134,500, Variance **+4,500**. Confirming records 134,500 as income and keeps 130,000 as the expected amount.

---

## 7. Wireframe — DLG-02 Income estimate

Opened from the Edit control in SCR-12 callout [1] (and from SCR-54 Settings). Shown as a dialog over a dimmed SCR-12.

```text
+----------------------------------------------+
|  Your income estimate                        |
|                                              |
|  What you expect to receive each month.      |
|                                              |
|  Low    LKR [ 160,000        ]               |
|  High   LKR [ 210,000        ]               |
|                                              |
|  [1] Computed from what arrived              |
|      3-month average   LKR 150,200           |
|                                              |
|  Your estimate is shown beside the computed  |
|  figure. It is never counted as income.      |
|                                              |
|       [ Save ]           [ Cancel ]          |
+----------------------------------------------+
```

---

## 8. Flow

```text
Overdue notification --> SCR-32
SCR-10 pending strip --> DLG-01
SCR-12 --> SCR-30 (Income sources)
SCR-12 [1] Edit --> DLG-02 (Income estimate)
SCR-12 --> SCR-31 --> SCR-32 --> DLG-01
                 \--> SCR-33 (New)      SCR-32 --> SCR-33 (Edit)
SCR-12 --> SCR-21 Record income
```

---

## 9. Annotations — the failure each element fixes

| Callout | Wireframe element | Cause | Failure cause (from the analysis) | How the element answers it | FRs |
|:-:|---|---|---|---|---|
| SCR-12 [1] | Received this month, 3-month average, 6-month range | A-16 | Needs four apps to compute income; estimate of LKR 160,000–210,000 never validated | Computed income sits beside the user's own estimate. | FR-57, FR-58 |
| SCR-12 [1] | Caption "Only money actually received counts" | A-23 | Impulsive with discretionary spending, optimistic about income, avoidant about expenses | Totals use received income only, in a neutral tone. | FR-56 |
| DLG-02 [1] | Low and high estimate beside the computed 3-month average | A-16 | Estimate of LKR 160,000–210,000 never validated | The user's own range is stored and shown next to the computed figure, and is not income. | FR-58, FR-115 |
| SCR-12 [2] | Expected vs received with variance | A-12 | Cannot recall the exact salary deposited; bonus varies | Expected and received are kept separate. Confirming records the actual amount and shows the variance. | FR-49, FR-56 |
| SCR-12 [3] | Waiting on payment, status chip, Mark received | A-13 | Forgot to chase invoices (twice); could not match payments to projects | Pending and overdue milestones are listed with a one-tap action. | FR-51, FR-52, FR-56, FR-99 |
| SCR-12 [4] | Income by source, AdSense estimate not counted | A-14 | AdSense paid in USD at a variable rate | An `EXPECTED` AdSense entry counts as zero until confirmed. | FR-54, FR-59 |
| SCR-12 [4] | Crypto year-to-date net | A-15 | No running crypto net position; cannot state year-to-date earnings | Signed realised results show a running net, kept separate. | FR-55 |
| SCR-30 | Source type, cadence, currency, expected amount and day | A-06 | Assumed a single income source | Income is several sources, each with its own cadence and currency. | FR-45 |
| SCR-30 [1] | "Not counted until you confirm" | A-12 | Cannot recall the exact salary deposited; bonus varies | An expected amount is a template, not income. | FR-49, FR-56 |
| SCR-31 | Status chip per milestone | A-13 | Forgot to chase invoices (twice); could not match payments to projects | One list shows which milestones are paid, pending or overdue. | FR-50, FR-51 |
| SCR-31 | Empty state | A-03 | After 13 days of diligent entry and a 3-week gap, the incomplete entries felt discouraging, so he never returned | A thin list is calm and helpful, never a failure. | FR-112 |
| SCR-32 [2] | Overdue milestone with Mark received and weekly reminder | A-13 | Forgot to chase invoices (twice); could not match payments to projects | The reminder opens the project and one tap records the payment. | FR-51, FR-52, FR-53 |
| SCR-32 | "View income entry" on a received milestone | A-13 | Could not match payments to projects | Each payment is linked to exactly one project and milestone (BR-15). | FR-52 |
| SCR-33 [1] | Milestone rows with amount and due date | A-13 | Forgot to chase invoices | A due date on every milestone is what makes it overdue on its own. | FR-50, FR-51 |
| DLG-01 [1] | Variance vs expected | A-12 | Cannot recall the exact salary deposited; bonus varies | The actual is entered at confirmation and the difference is shown. | FR-49, FR-52 |

**Lens 2 shared causes addressed:** *a data model that does not fit his life* (SCR-12, 30, 31), *no insight in return for effort* (SCR-12 [1]).

---

## 10. Acceptance criteria (issue #124)

| Criterion | Where it is met |
|---|---|
| Low-fi wireframe covering expected vs received (A-12) | SCR-12 callout 2, SCR-30 callout 1, DLG-01 variance |
| Low-fi wireframe covering overdue (A-13) | SCR-12 callout 3, SCR-31 status chips, SCR-32 callout 2 |
| Income sources and invoices screens | SCR-12, SCR-30, SCR-31, SCR-32, SCR-33, DLG-01, DLG-02 |

## 11. Arithmetic used

| Figure | Value | Rule |
|---|---|---|
| Salary variance | 134,500 − 130,000 = +4,500 | BR-16 |
| Income this month | 134,500 + 0 + 0 = 134,500 (expected AdSense 18,000 excluded) | FR-56, FR-59 |
| Project received | 30,000 of 90,000 (one milestone) | BR-15 |
| Milestones total | 30,000 × 3 = 90,000 = total agreed | FR-50 |
| Overdue milestone | due 01/10/2026, today 07/10/2026, 6 days ago | BR-15 |
| Web app Milestone 2 | due 12/10/2026, today 07/10/2026, in 5 days; Milestone 1 is received (SCR-31 chip order is milestone order) | BR-15 |
| Web app milestones (placeholder) | 30,000 + 45,000 + 45,000 = 120,000 total agreed | FR-50 |
| Freelance 0 this month | Every received milestone on SCR-31 (Logo design 1 on 22/09/2026, Web app 1, both Brand guide milestones) landed before October, so October freelance income is 0 | FR-56, FR-59 |

## 12. Open points

| Point | Detail | Owner action |
|---|---|---|
| Whole-rupee display | NFR-20 and BR-02 specify two decimals (`LKR 1,000.50`). The frames show whole rupees. | Settle with S1–S3, S5 and S6 in `failure-causes.md` §4. |
| Web app amounts | The scenario gives only the 120,000 total. The milestone amounts are placeholders chosen to add up to it. | Replace if the scenario figures are defined. |
