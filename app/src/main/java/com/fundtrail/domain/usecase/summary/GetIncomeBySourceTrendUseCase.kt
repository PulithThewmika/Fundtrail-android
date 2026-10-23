package com.fundtrail.domain.usecase.summary

import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.IncomeSourceTrendPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

/**
 * Actual income per source per month for the Insights screen (#207, #209). Built on [GetMonthlySummaryUseCase],
 * so it inherits the ACTUAL-only, no-transfers rules. Every month in the window appears, and within each month
 * every [IncomeSource] appears with a zero where it had no income, so the series never shift (A-03).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetIncomeBySourceTrendUseCase @Inject constructor(
    private val monthlySummary: GetMonthlySummaryUseCase,
    private val clock: Clock,
) {
    operator fun invoke(months: Int = GetMonthlyTrendUseCase.DEFAULT_MONTHS): Flow<List<IncomeSourceTrendPoint>> {
        require(months > 0) { "months must be positive" }
        return clock.dateChanges().flatMapLatest { today ->
            val window = (months - 1 downTo 0).map { YearMonth.from(today).minusMonths(it.toLong()) }
            combine(window.map { month -> monthlySummary(month) }) { summaries ->
                window.mapIndexed { i, month ->
                    val present = summaries[i].incomeBySource
                    IncomeSourceTrendPoint(month, IncomeSource.entries.associateWith { present[it] ?: 0L })
                }
            }
        }
    }
}
