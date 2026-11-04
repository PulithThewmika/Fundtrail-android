package com.fundtrail.ui.insights

import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.IncomeVsEstimateComparison
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.UserSettings
import com.fundtrail.domain.repository.SettingsRepository
import com.fundtrail.domain.usecase.income.GetIncomeVsEstimateUseCase
import com.fundtrail.domain.usecase.recurring.ObserveRecurringUseCase
import com.fundtrail.domain.usecase.summary.GetIncomeBySourceTrendUseCase
import com.fundtrail.domain.usecase.summary.GetMonthlySummaryUseCase
import com.fundtrail.domain.usecase.summary.GetMonthlyTrendUseCase
import com.fundtrail.fakes.FakeRecurringRepository
import com.fundtrail.fakes.FakeSettingsRepository
import com.fundtrail.fakes.FakeTransactionRepository
import com.fundtrail.fakes.MainDispatcherRule
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InsightsViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Colombo")
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 15).atStartOfDay(zone).toInstant(), zone)
    private val transactions = FakeTransactionRepository().apply { setTransactions(emptyList()) }
    private val recurring = FakeRecurringRepository().apply { setTemplates(emptyList()) }
    private val settings = FakeSettingsRepository()

    private fun income(id: String, month: YearMonth, amount: Long, source: IncomeSource = IncomeSource.SALARY) =
        Transaction(
            id = id, type = TransactionType.INCOME, amountLkrMinor = amount, accountId = "default-main-bank",
            incomeSource = source, occurredAt = month.atDay(25).atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
        )

    private fun template(id: String, amount: Long, paused: Boolean = false) =
        RecurringTemplate(
            id = id, name = id, categoryId = "cat_rent", accountId = "default-main-bank",
            expectedAmountMinor = amount, dayOfMonth = 1, nextDueDate = LocalDate.of(2026, 10, 1), isPaused = paused,
        )

    private fun TestScope.viewModel() = InsightsViewModel(
        GetMonthlyTrendUseCase(GetMonthlySummaryUseCase(transactions), clock),
        GetIncomeBySourceTrendUseCase(GetMonthlySummaryUseCase(transactions), clock),
        ObserveRecurringUseCase(recurring),
        GetIncomeVsEstimateUseCase(settings, transactions, clock),
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} } }

    @Test
    fun oneMonthOfDataIsNotEnoughForTrends() = runTest {
        transactions.setTransactions(listOf(income("s", YearMonth.of(2026, 10), 14_000_000L)))
        assertFalse(viewModel().uiState.value.hasEnoughData)
    }

    @Test
    fun twoMonthsOfDataShowTheTrend() = runTest {
        transactions.setTransactions(
            listOf(
                income("s9", YearMonth.of(2026, 9), 12_000_000L),
                income("s10", YearMonth.of(2026, 10), 14_000_000L),
            ),
        )
        val state = viewModel().uiState.value
        assertTrue(state.hasEnoughData)
        assertEquals(6, state.trend.size)
    }

    @Test
    fun recurringMonthlyTotalExcludesPaused() = runTest {
        recurring.setTemplates(listOf(template("rent", 3_400_000L), template("cloud", 120_000L, paused = true)))
        assertEquals(3_400_000L, viewModel().uiState.value.recurringMonthlyTotalMinor)
    }

    private suspend fun estimate(min: Long?, max: Long?) {
        settings.update(UserSettings(incomeEstimateMinMinor = min, incomeEstimateMaxMinor = max))
    }

    /** Three complete months before the fixed October date, each with the same received income. */
    private fun threeMonthsReceiving(amount: Long) = listOf(
        income("s7", YearMonth.of(2026, 7), amount),
        income("s8", YearMonth.of(2026, 8), amount),
        income("s9", YearMonth.of(2026, 9), amount),
    )

    @Test
    fun anAverageInsideTheEstimateIsWithinIt() = runTest {
        estimate(16_000_000L, 21_000_000L)
        transactions.setTransactions(threeMonthsReceiving(18_500_000L))

        val vsEstimate = viewModel().uiState.value.incomeVsEstimate
        assertNotNull(vsEstimate)
        assertEquals(IncomeVsEstimateComparison.WITHIN, vsEstimate!!.comparison)
        assertEquals(18_500_000L, vsEstimate.actualAverageMinor)
        assertEquals(16_000_000L, vsEstimate.estimateMinMinor)
        assertEquals(21_000_000L, vsEstimate.estimateMaxMinor)
        assertEquals(3, vsEstimate.monthsCount)
    }

    @Test
    fun anAverageBelowOrAboveTheEstimateSaysSo() = runTest {
        estimate(16_000_000L, 21_000_000L)
        transactions.setTransactions(threeMonthsReceiving(12_000_000L))
        assertEquals(IncomeVsEstimateComparison.BELOW, viewModel().uiState.value.incomeVsEstimate!!.comparison)

        transactions.setTransactions(threeMonthsReceiving(25_000_000L))
        assertEquals(IncomeVsEstimateComparison.ABOVE, viewModel().uiState.value.incomeVsEstimate!!.comparison)
    }

    @Test
    fun withNoEstimateSetTheAverageIsStillShownWithoutAComparison() = runTest {
        estimate(null, null)
        transactions.setTransactions(threeMonthsReceiving(18_500_000L))

        val vsEstimate = viewModel().uiState.value.incomeVsEstimate!!
        assertNull(vsEstimate.comparison)
        assertNull(vsEstimate.estimateMinMinor)
        assertEquals(18_500_000L, vsEstimate.actualAverageMinor)
    }

    @Test
    fun withNoCompleteMonthYetThereIsNothingToCompare() = runTest {
        estimate(16_000_000L, 21_000_000L)
        transactions.setTransactions(emptyList())

        val vsEstimate = viewModel().uiState.value.incomeVsEstimate!!
        assertEquals(0, vsEstimate.monthsCount)
        assertNull(vsEstimate.comparison)
    }

    @Test
    fun theEstimateCountsReceivedMoneyOnlyNotExpectedOrEstimatedIncome() = runTest {
        estimate(16_000_000L, 21_000_000L)
        transactions.setTransactions(
            threeMonthsReceiving(18_500_000L) +
                income("est", YearMonth.of(2026, 9), 99_000_000L).copy(status = com.fundtrail.domain.model.TransactionStatus.EXPECTED),
        )

        assertEquals(18_500_000L, viewModel().uiState.value.incomeVsEstimate!!.actualAverageMinor)
    }

    private class FailingSettings : SettingsRepository {
        override fun observe(): Flow<UserSettings> = flow { throw java.io.IOException("disk") }
        override suspend fun update(settings: UserSettings): Result<Unit> = Result.Success(Unit)
    }

    @Test
    fun anEstimateThatCannotBeReadHidesOnlyTheEstimateCard() = runTest {
        transactions.setTransactions(
            listOf(
                income("s9", YearMonth.of(2026, 9), 12_000_000L),
                income("s10", YearMonth.of(2026, 10), 14_000_000L),
            ),
        )
        val vm = InsightsViewModel(
            GetMonthlyTrendUseCase(GetMonthlySummaryUseCase(transactions), clock),
            GetIncomeBySourceTrendUseCase(GetMonthlySummaryUseCase(transactions), clock),
            ObserveRecurringUseCase(recurring),
            GetIncomeVsEstimateUseCase(FailingSettings(), transactions, clock),
        ).also { v -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { v.uiState.collect {} } }

        val state = vm.uiState.value
        assertNull(state.incomeVsEstimate)
        assertTrue("the trend must still show", state.hasEnoughData)
        assertEquals(6, state.trend.size)
    }
}
