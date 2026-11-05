package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.model.MonthlySummary
import org.junit.Assert.assertEquals
import org.junit.Test

/** FR-57. */
class GetIncomeSummaryUseCaseTest {

    private fun summary(received: Long) =
        MonthlySummary(receivedIncomeMinor = received, spentMinor = 0L, netSavedMinor = received, incomeBySource = emptyMap())

    @Test
    fun `this month, 3-month average and 6-month range match hand calculation`() {
        // index 0 = this month, 1..6 = trailing complete months, oldest last.
        val monthly = listOf(
            summary(20_000_000L), // this month
            summary(18_000_000L), // last month
            summary(14_000_000L),
            summary(16_000_000L),
            summary(10_000_000L), // lowest
            summary(22_000_000L), // highest
            summary(12_000_000L),
        )
        val result = GetIncomeSummaryUseCase.compute(monthly)
        assertEquals(20_000_000L, result.thisMonthReceivedMinor)
        assertEquals((18_000_000L + 14_000_000L + 16_000_000L) / 3, result.averageMinor)
        assertEquals(10_000_000L, result.lowestMinor)
        assertEquals(22_000_000L, result.highestMinor)
    }

    @Test
    fun `a month with no income counts as zero, not skipped`() {
        val monthly = listOf(summary(5_000_000L)) + List(6) { summary(0L) }
        val result = GetIncomeSummaryUseCase.compute(monthly)
        assertEquals(0L, result.averageMinor)
        assertEquals(0L, result.lowestMinor)
        assertEquals(0L, result.highestMinor)
    }
}
