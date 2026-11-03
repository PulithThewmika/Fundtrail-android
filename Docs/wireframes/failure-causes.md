# FundTrail — Wireframes: Failure-Cause Index

| | |
|---|---|
| **Document** | `Docs/wireframes/failure-causes.md` |
| **Version / status** | 1.5 — Draft for team review |
| **Date** | 2026-10-21 |
| **Issue** | EPIC-002 / T2 · Parent: #11 |
| **Source of causes** | `Docs/Scenario analysis.md`, Part 1 rows `A-01`…`A-24` and Part 2 Lens 2 |
| **Related** | `Docs/srs.md` v1.4 (FR ids), `Docs/ux/screens.md` v1.2 (screen ids) |

**Purpose.** Each wireframe page is annotated with the failure cause it fixes. The page files hold the wireframe and its full annotation table. This file is the index: it links the pages and shows, at a glance, which causes each one answers.

**Note on the file name.** The issue refers to `Docs/analysis.md`. In this repository the analysis is `Docs/Scenario analysis.md`.

---

## 1. Wireframe pages

| Page | Issue | Screen | Wireframe and annotations |
|---|---|---|---|
| S1 Dashboard | #12 | SCR-10 Insights | [S1-dashboard.md](S1-dashboard.md) |
| S2 Quick-add bottom sheet | #13 | SCR-20 Quick-add expense | [S2-quick-add.md](S2-quick-add.md) |
| S3 Goal detail | #14 | SCR-13 Goal | [S3-goal-detail.md](S3-goal-detail.md) |
| S4 Income sources and invoices | #124 | SCR-12, SCR-30, SCR-31, SCR-32, SCR-33, DLG-01, DLG-02 | [S4-income-invoices.md](S4-income-invoices.md) |
| S5 Goal setup and adjustment | #124 | SCR-40, SCR-41, DLG-05, SCR-13 (infeasible goal, no income) | [S5-goal-setup.md](S5-goal-setup.md) |
| S6 Insights trends and recurring costs | #124 | SCR-10 (trends, categories), SCR-50, SCR-51, DLG-06 | [S6-insights-recurring.md](S6-insights-recurring.md) |

Each page file contains: the low-fi wireframe (populated and empty or variant states), a table of *callout → analysis row → failure cause → how the element answers it → FRs*, and the acceptance criteria from its issue with where each is met.

---

## 2. At a glance: causes addressed per page

| Cause | S1 Dashboard | S2 Quick-add | S3 Goal detail |
|---|:-:|:-:|:-:|
| A-01 Hand-built formulas broke | ✓ | | ✓ |
| A-02 Data-entry overhead too high | ✓ | ✓ | |
| A-03 Gap made incomplete entries discouraging | ✓ | ✓ | ✓ |
| A-04 USD only | | ✓ | |
| A-05 No cloud sync | ✓ | ✓ | |
| A-07 Fixed monthly budget assumed | ✓ | | |
| A-08 Unstructured, unlabelled figures | | ✓ | |
| A-09 Data in, no insight out | ✓ | | |
| A-10 Cash and secondary accounts missing | | ✓ | |
| A-11 Miscategorised PickMe fare | | ✓ | |
| A-12 Salary received amount not recorded | ✓ | | |
| A-13 Forgot to chase invoices | ✓ | | |
| A-15 No crypto net position | | | ✓ |
| A-16 Income never computed or validated | ✓ | | |
| A-17 Fragmented spending, subscriptions | ✓ | | |
| A-18 No committed vs discretionary view | ✓ | ✓ | |
| A-19 Goal financially invisible | ✓ | | ✓ |
| A-20 Too unpleasant to open daily | ✓ | | ✓ |
| A-21 Transfers counted twice | ✓ | | |
| A-23 Impulsive, optimistic, avoidant | ✓ | ✓ | ✓ |
| A-24 Required saving may be unrealistic | | | ✓ |

### Causes addressed by the M3 secondary pages (S4 to S6)

| Cause | S4 Income and invoices | S5 Goal setup | S6 Insights and recurring |
|---|:-:|:-:|:-:|
| A-01 Hand-built formulas broke | | ✓ | |
| A-03 Gap made incomplete entries discouraging | ✓ | ✓ | ✓ |
| A-06 Single income source assumed | ✓ | | |
| A-07 Fixed monthly budget assumed | | | ✓ |
| A-09 Data in, no insight out | | | ✓ |
| A-12 Salary received amount not recorded | ✓ | | |
| A-13 Forgot to chase invoices | ✓ | | |
| A-14 AdSense paid in USD at a variable rate | ✓ | | |
| A-15 No crypto net position | ✓ | | |
| A-16 Income never computed or validated | ✓ | | |
| A-17 Fragmented spending, subscriptions | | | ✓ |
| A-18 No committed vs discretionary view | | | ✓ |
| A-19 Goal financially invisible | | ✓ | ✓ |
| A-20 Too unpleasant to open daily | | | ✓ |
| A-22 Variable utilities | | | ✓ |
| A-23 Impulsive, optimistic, avoidant | ✓ | ✓ | |
| A-24 Required saving may be unrealistic | | ✓ | |

With S4 to S6, **A-06**, **A-14** and **A-22**, left open after S1 to S3, now have a page.

---

## 3. Lens 2 crosswalk: the five shared causes

The analysis groups the four tool failures into five behavioural and UX causes. The table shows where each is answered.

| # | Shared cause (Lens 2) | S1 Dashboard | S2 Quick-add | S3 Goal detail |
|:-:|---|---|---|---|
| 1 | High friction per entry | FAB, one tap to add | Amount first, defaults, chips, three taps | |
| 2 | A data model that does not fit his life | Received vs expected, transfers excluded | Foreign currency, account, structured record | Crypto kept separate |
| 3 | No insight in return for effort | This month vs last month, split, categories | Goal cost shown on save | Required vs pace, projected date |
| 4 | No tolerance for lapses | Empty state, welcome-back | Back-dating, "Save and add another" | "No projection yet" state |
| 5 | No connection to the goal | Goal card | Goal-cost message | Whole screen |

---

## 4. Open points

| Point | Detail | Owner action |
|---|---|---|
| Quick-add as a sheet (S2) | `Docs/ux/screens.md` lists SCR-20 as a full-screen route. Issue #13 specifies a bottom sheet. | Decide how SCR-20 is hosted as a bottom sheet, then update `screens.md` to match. Navigation Compose's `dialog()` destination cannot host a bottom sheet, so it is not an option as written. Follow the decision on #65. |
| Dashboard projected date (S1) | The same pace of LKR 20,000 per month gives 05/10/2028 on S1 and S3. | Keep both pages in step if the pace figure changes. |
| Trend chart library (S6) | The library question is settled: [ADR-0006](../adr/0006-trend-chart-library.md) is Accepted and supersedes ADR-0005 in part. The SRS still has no FR for the trends card. | `TODO(M3)`: add the FR before M3 build starts, or drop the card. |
| Currency display (S1 to S6) | NFR-20 and BR-02 specify two decimals (`LKR 1,000.50`). Every wireframe draws whole rupees. S4 to S6 say so in their legend; S1 to S3 do not. | `TODO(M3)`: either keep whole rupees in the frames as a drawing convention (the app already formats with two decimals), or amend NFR-20 to allow whole rupees in lists and summary cards. M4 inherits whichever is chosen. |
| Goal status set (S3, S5) | SRS v1.4 adds *needs adjusting* to BR-09 and FR-78, shown while the infeasibility banner is up. | `TODO(M3)`: confirm the label in review. |

---

## Change log

| Version | Date | Change |
|---|---|---|
| 1.0 | 2026-10-07 | First draft: failure causes for S1, S2 and S3 in one file. |
| 1.1 | 2026-10-08 | Split the per-screen tables into `S1-dashboard.md`, `S2-quick-add.md` and `S3-goal-detail.md`. This file is now the index. |
| 1.2 | 2026-10-20 | Add `S4-income-invoices.md`, `S5-goal-setup.md` and `S6-insights-recurring.md` (EPIC-011 / T4, #124) and their cause matrix. |
| 1.3 | 2026-10-21 | Review fixes on S4 to S6: add DLG-02 to the S4 row; rename the S5 SCR-13 states to *infeasible goal* and *no income*; add the trend-chart (ADR-0006), currency-display and goal-status open points. |
| 1.4 | 2026-10-21 | Second review pass on S4 to S6: SCR-13 status set carried into `screens.md` v1.2; deadline offers use DD/MM/YYYY (S3, S5); October freelance total and the trends chart's August column explained (S4, S6); the trends row no longer traces to FR-91 and FR-98 as if they covered the chart. |
| 1.5 | 2026-10-21 | ADR-0006 accepted: the trend-chart library question is closed, and the open point now carries only the missing FR. CodeRabbit round: deferred owner actions carry `TODO(M3)` markers per CONTRIBUTING, and the goal-status row names the dismissal condition. |
