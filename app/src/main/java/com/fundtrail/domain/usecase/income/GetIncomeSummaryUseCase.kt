package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.IncomeSummary
import com.fundtrail.domain.model.MonthlySummary
import com.fundtrail.domain.usecase.summary.GetMonthlySummaryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

/** FR-57: this month's received income, trailing 3-month average, and 6-month low-high range. */
class GetIncomeSummaryUseCase @Inject constructor(
    private val monthlySummary: GetMonthlySummaryUseCase,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<IncomeSummary> {
        val currentMonth = YearMonth.now(clock.withZone(APP_ZONE))
        // index 0 = this month, 1..6 = the trailing 6 complete months.
        val months = (0..6).map { currentMonth.minusMonths(it.toLong()) }
        return combine(months.map { monthlySummary(it) }) { it.toList() }.map { compute(it) }
    }

    companion object {
        internal fun compute(monthly: List<MonthlySummary>): IncomeSummary {
            val thisMonth = monthly[0].receivedIncomeMinor
            val trailing6 = monthly.drop(1).map { it.receivedIncomeMinor }
            val average3 = trailing6.take(3).sum() / 3
            return IncomeSummary(
                thisMonthReceivedMinor = thisMonth,
                averageMinor = average3,
                lowestMinor = trailing6.min(),
                highestMinor = trailing6.max(),
            )
        }
    }
}
