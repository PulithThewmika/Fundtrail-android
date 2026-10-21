# Insights — how the months compare

**Epic:** EPIC-016 · **Requirements:** A-09, A-16, A-18, A-07, A-03 · **Decisions:** [ADR-0005 charts](../adr/0005-chart-approach.md), [ADR-0006 trend chart library](../adr/0006-trend-chart-library.md)

> **Status:** the trend use cases (#207) and the Insights screen (#210) are in progress. This section records the agreed design. The owner updates it and adds the screenshot when they merge.

## Problem

Kavindu's data "went in, but no insight came out" (**A-09**). He was shocked to find LKR 24,000 a month on coffee and dining, because nothing split committed from discretionary spending (**A-18**). His own income estimate was never validated (**A-16**). His income is irregular, so a fixed budget bar is meaningless, and the useful comparison is month against month (**A-07**).

## Design decision

- **Pure, tested trend use cases** (#207):
  - `GetMonthlyTrendUseCase(months = 6)` returns received, spent and net saved per month. It reuses the M2 monthly-summary rules: only `ACTUAL` counts, and transfers are excluded.
  - `GetIncomeBySourceTrendUseCase(months = 6)` returns the amount per income source per month.
  - Months with no data return zeros, so the chart has no gaps.
- **One screen, one state** (#210). `InsightsViewModel` combines four inputs: the trend, income by source, the monthly recurring total (EPIC-015) and actual vs estimate (#194).
- **Charts.** Simple shapes stay plain Compose (ADR-0005). The trends card needs axes, 3/6/12-month ranges and a touch marker, so it uses Vico (ADR-0006, trend chart library). That's one dependency for one card.
- **Gap-tolerant (A-03).** With fewer than 2 months of data the screen says "Trends appear after your second month", not an empty or failing chart.
- **Neutral tone (A-23).** Comparisons are stated, not judged, and nothing is shown in red.

## Data flow

```text
Insights screen ─ InsightsViewModel (#210) ─┬─ GetMonthlyTrendUseCase (#207) ───────┐
                                            ├─ GetIncomeBySourceTrendUseCase (#207) ├─ TransactionRepository ── Room
                                            ├─ GetIncomeVsEstimateUseCase (#194) ────┘   + SettingsRepository
                                            └─ recurring monthly total (EPIC-015) ── RecurringRepository ── Room
```

Every input is a Room-backed `Flow`, so the screen updates the moment an entry is saved, online or offline (A-09).

## Edge cases handled

| Case | Behaviour |
|---|---|
| A month with no entries | A zero row, so the series stay aligned (#207 AC). |
| Fewer than 2 months of data | A neutral message instead of the trend chart (#210 AC). |
| Foreign-currency income | Uses the locked `amountLkrMinor`, so moving rates never change past months (ADR-0003). |
| Transfers between own accounts | Excluded from received and spent (A-21). |
| Month boundaries | `Asia/Colombo`, the same as every other monthly figure. |

## Screenshots

| |
|---|
| ![Insights trends on seeded data](img/insights-trends.png) |
| Insights on seeded data (three months, Aug–Oct 2026). Captured once #210 merges. Until then the figure is the S6 wireframe ([S6-insights-recurring.md](../wireframes/S6-insights-recurring.md) §1). |

## Tests

- #207: unit tests with the seeded-data shape, including zero months.
- #210: ViewModel tests for the fewer-than-2-months message.
