package com.fundtrail.domain.model

import java.time.YearMonth

/**
 * One month on the Insights received/spent/saved trend (#207, #208). Every figure is in LKR minor units and
 * counts only ACTUAL entries, excluding transfers — the same rules as [MonthlySummary], which it is built on.
 * A month with no entries is a zero row, so the chart has no gaps (A-03).
 */
data class MonthlyTrendPoint(
    val month: YearMonth,
    val receivedMinor: Long,
    val spentMinor: Long,
    val netSavedMinor: Long,
)

/**
 * One month's actual income split by source (#207, #209). [bySource] carries every [IncomeSource], zero where
 * that source had no income this month, so stacked or grouped series stay aligned across the window.
 */
data class IncomeSourceTrendPoint(
    val month: YearMonth,
    val bySource: Map<IncomeSource, Long>,
)
