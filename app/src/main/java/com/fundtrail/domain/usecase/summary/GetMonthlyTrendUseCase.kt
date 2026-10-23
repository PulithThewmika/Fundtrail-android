package com.fundtrail.domain.usecase.summary

import com.fundtrail.domain.model.MonthlyTrendPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

/**
 * The received/spent/saved trend for the Insights screen (#207, #208). Reuses [GetMonthlySummaryUseCase]
 * rather than re-summing transactions, so the ACTUAL-only and no-transfers rules stay in one place.
 *
 * The window is the last [DEFAULT_MONTHS] months ending with the current month, oldest first. A month with no
 * data still appears, as a zero row. The window re-bases at midnight so an open screen rolls forward.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetMonthlyTrendUseCase @Inject constructor(
    private val monthlySummary: GetMonthlySummaryUseCase,
    private val clock: Clock,
) {
    operator fun invoke(months: Int = DEFAULT_MONTHS): Flow<List<MonthlyTrendPoint>> {
        require(months > 0) { "months must be positive" }
        return clock.dateChanges().flatMapLatest { today ->
            val window = windowEndingAt(YearMonth.from(today), months)
            combine(window.map { month -> monthlySummary(month) }) { summaries ->
                window.mapIndexed { i, month ->
                    val s = summaries[i]
                    MonthlyTrendPoint(month, s.receivedIncomeMinor, s.spentMinor, s.netSavedMinor)
                }
            }
        }
    }

    private fun windowEndingAt(current: YearMonth, months: Int): List<YearMonth> =
        (months - 1 downTo 0).map { current.minusMonths(it.toLong()) }

    companion object {
        const val DEFAULT_MONTHS = 6
    }
}
