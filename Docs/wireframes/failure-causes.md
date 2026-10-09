# FundTrail — Wireframes: Failure-Cause Index

| | |
|---|---|
| **Document** | `Docs/wireframes/failure-causes.md` |
| **Version / status** | 1.1 — Draft for team review |
| **Date** | 2026-10-08 |
| **Issue** | EPIC-002 / T2 · Parent: #11 |
| **Source of causes** | `Docs/Scenario analysis.md`, Part 1 rows `A-01`…`A-24` and Part 2 Lens 2 |
| **Related** | `Docs/srs.md` v1.2 (FR ids), `Docs/ux/screens.md` (screen ids) |

**Purpose.** Each wireframe page is annotated with the failure cause it fixes. The page files hold the wireframe and its full annotation table. This file is the index: it links the pages and shows, at a glance, which causes each one answers.

**Note on the file name.** The issue refers to `Docs/analysis.md`. In this repository the analysis is `Docs/Scenario analysis.md`.

---

## 1. Wireframe pages

| Page | Issue | Screen | Wireframe and annotations |
|---|---|---|---|
| S1 Dashboard | #12 | SCR-10 Insights | [S1-dashboard.md](S1-dashboard.md) |
| S2 Quick-add bottom sheet | #13 | SCR-20 Quick-add expense | [S2-quick-add.md](S2-quick-add.md) |
| S3 Goal detail | #14 | SCR-13 Goal | [S3-goal-detail.md](S3-goal-detail.md) |

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

Not addressed by these three pages, and left to later wireframes: **A-06** (single income source), **A-14** (AdSense), **A-22** (variable utilities). They belong to the Income and Recurring screens.

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
| What-if control (S3) | No analysis row names it and no FR in SRS v1.1 requires it. It extends A-19 and A-24. | Add an FR to the SRS (for example FR-85, Should, tracing to A-19) or drop the control. |
| Quick-add as a sheet (S2) | `Docs/ux/screens.md` lists SCR-20 as a full-screen route. Issue #13 specifies a bottom sheet. | Change SCR-20 to a sheet (`dialog`) destination in `screens.md`. |
| Dashboard projected date (S1) | The same pace of LKR 20,000 per month gives 05/10/2028 on S1 and S3. | Keep both pages in step if the pace figure changes. |

---

## Change log

| Version | Date | Change |
|---|---|---|
| 1.0 | 2026-10-07 | First draft: failure causes for S1, S2 and S3 in one file. |
| 1.1 | 2026-10-08 | Split the per-screen tables into `S1-dashboard.md`, `S2-quick-add.md` and `S3-goal-detail.md`. This file is now the index. |
