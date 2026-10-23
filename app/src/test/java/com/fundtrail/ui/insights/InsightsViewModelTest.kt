package com.fundtrail.ui.insights

import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.usecase.recurring.ObserveRecurringUseCase
import com.fundtrail.domain.usecase.summary.GetIncomeBySourceTrendUseCase
import com.fundtrail.domain.usecase.summary.GetMonthlySummaryUseCase
import com.fundtrail.domain.usecase.summary.GetMonthlyTrendUseCase
import com.fundtrail.fakes.FakeRecurringRepository
import com.fundtrail.fakes.FakeTransactionRepository
import com.fundtrail.fakes.MainDispatcherRule
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InsightsViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Colombo")
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 15).atStartOfDay(zone).toInstant(), zone)
    private val transactions = FakeTransactionRepository().apply { setTransactions(emptyList()) }
    private val recurring = FakeRecurringRepository().apply { setTemplates(emptyList()) }

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

    @Test
    fun theEstimateCardStaysUnavailableUntil194() = runTest {
        assertFalse(viewModel().uiState.value.estimateCardAvailable)
    }
}
