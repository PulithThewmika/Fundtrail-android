package com.fundtrail.ui.dashboard

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SafeToSpendState
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.UserSettings
import com.fundtrail.domain.model.toMillisRange
import com.fundtrail.domain.repository.GoalRepository
import com.fundtrail.domain.usecase.category.ObserveCategoriesUseCase
import com.fundtrail.domain.usecase.goal.GetGoalProgressUseCase
import com.fundtrail.domain.usecase.goal.ObserveActiveGoalUseCase
import com.fundtrail.domain.usecase.income.GetSalaryStatusUseCase
import com.fundtrail.domain.usecase.income.ObserveSalaryExpectationUseCase
import com.fundtrail.domain.usecase.recurring.ConfirmRecurringUseCase
import com.fundtrail.domain.usecase.recurring.GetDueRecurringUseCase
import com.fundtrail.domain.usecase.recurring.SkipRecurringUseCase
import com.fundtrail.domain.usecase.summary.GetMonthlySummaryUseCase
import com.fundtrail.domain.usecase.summary.GetSafeToSpendUseCase
import com.fundtrail.domain.usecase.summary.GetSpendingBreakdownUseCase
import com.fundtrail.domain.usecase.sync.ObserveSyncStatusUseCase
import com.fundtrail.fakes.FakeCategoryRepository
import com.fundtrail.fakes.FakeRecurringRepository
import com.fundtrail.fakes.FakeSettingsRepository
import com.fundtrail.fakes.FakeSyncStatusRepository
import com.fundtrail.fakes.FakeTransactionRepository
import com.fundtrail.fakes.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

/** What the dashboard exposes for the Safe to spend card and its explainer sheet (#217). */
class DashboardViewModelSafeToSpendTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    /** No active goal, so the figures below are plain sums. */
    private class NoGoal : GoalRepository {
        override fun observeActive(): Flow<Goal?> = MutableStateFlow(null)
        override suspend fun save(goal: Goal): Result<Unit> = Result.Success(Unit)
        override suspend fun archive(id: String): Result<Unit> = Result.Success(Unit)
    }

    private val transactions = FakeTransactionRepository().apply { setTransactions(emptyList()) }
    private val recurring = FakeRecurringRepository().apply { setTemplates(emptyList()) }
    private val settings = FakeSettingsRepository()
    private val goals = NoGoal()
    private val thisMonth = YearMonth.now(APP_ZONE)
    private val fixedClock = Clock.fixed(LocalDate.now(APP_ZONE).atStartOfDay(APP_ZONE).toInstant(), APP_ZONE)

    private fun TestScope.viewModel(): DashboardViewModel {
        val monthly = GetMonthlySummaryUseCase(transactions)
        return DashboardViewModel(
            monthly,
            GetSpendingBreakdownUseCase(transactions),
            ObserveCategoriesUseCase(FakeCategoryRepository()),
            ObserveSyncStatusUseCase(FakeSyncStatusRepository()),
            ObserveActiveGoalUseCase(goals),
            GetGoalProgressUseCase(goals, monthly, fixedClock),
            GetDueRecurringUseCase(recurring, fixedClock),
            ConfirmRecurringUseCase(recurring),
            SkipRecurringUseCase(recurring),
            GetSalaryStatusUseCase(settings, transactions, fixedClock),
            GetSafeToSpendUseCase(
                monthly,
                GetSpendingBreakdownUseCase(transactions),
                GetGoalProgressUseCase(goals, monthly, fixedClock),
                ObserveActiveGoalUseCase(goals),
                recurring,
                GetSalaryStatusUseCase(settings, transactions, fixedClock),
                ObserveSalaryExpectationUseCase(settings),
                fixedClock,
            ),
            fixedClock,
        ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} } }
    }

    private fun dayIn(month: YearMonth) = month.toMillisRange().first + 3_600_000L

    private fun income(id: String, amount: Long, month: YearMonth = thisMonth) = Transaction(
        id = id, type = TransactionType.INCOME, amountLkrMinor = amount, accountId = "acc_main_bank",
        incomeSource = IncomeSource.SALARY, occurredAt = dayIn(month),
    )

    private fun expense(id: String, amount: Long, kind: SpendKind) = Transaction(
        id = id, type = TransactionType.EXPENSE, amountLkrMinor = amount, accountId = "acc_main_bank",
        categoryId = "cat_food", spendKind = kind, occurredAt = dayIn(thisMonth),
    )

    @Test
    fun `exposes the figure and the numbers behind it for the current month`() = runTest {
        transactions.setTransactions(
            listOf(
                income("pay", 10_000_000L),
                expense("rent", 2_000_000L, SpendKind.COMMITTED),
                expense("coffee", 300_000L, SpendKind.DISCRETIONARY),
            ),
        )
        val safe = viewModel().uiState.value.safeToSpend
        assertNotNull(safe)
        assertEquals(7_700_000L, safe!!.amountMinor)
        assertEquals(10_000_000L, safe.parts.receivedMinor)
        assertEquals(2_000_000L, safe.parts.committedMinor)
        assertEquals(0L, safe.parts.goalSavingMinor)
        assertEquals(300_000L, safe.parts.discretionarySpentMinor)
    }

    @Test
    fun `updates after a new entry`() = runTest {
        transactions.setTransactions(listOf(income("pay", 10_000_000L)))
        val vm = viewModel()
        assertEquals(10_000_000L, vm.uiState.value.safeToSpend!!.amountMinor)

        transactions.setTransactions(listOf(income("pay", 10_000_000L), expense("lunch", 400_000L, SpendKind.DISCRETIONARY)))
        assertEquals(9_600_000L, vm.uiState.value.safeToSpend!!.amountMinor)
    }

    @Test
    fun `an empty month with a salary expected shows waiting for salary`() = runTest {
        // Default settings expect a salary, and nothing is recorded yet.
        assertEquals(SafeToSpendState.WAITING_FOR_SALARY, viewModel().uiState.value.safeToSpend?.state)
    }

    @Test
    fun `an empty month with no salary expected shows the first-run state`() = runTest {
        settings.update(UserSettings(expectedSalaryMinor = null, salaryPayday = 25))
        assertEquals(SafeToSpendState.NOT_ENOUGH_DATA, viewModel().uiState.value.safeToSpend?.state)
    }

    @Test
    fun `is hidden while a past month is selected and returns on the current month`() = runTest {
        transactions.setTransactions(listOf(income("pay", 10_000_000L)))
        val vm = viewModel()
        assertNotNull(vm.uiState.value.safeToSpend)

        vm.onPreviousMonth()
        assertNull(vm.uiState.value.safeToSpend)

        vm.onNextMonth()
        assertNotNull(vm.uiState.value.safeToSpend)
    }

    @Test
    fun `the explainer opens and closes`() = runTest {
        transactions.setTransactions(listOf(income("pay", 10_000_000L)))
        val vm = viewModel()
        assertFalse(vm.uiState.value.safeToSpendExplainerOpen)

        vm.onSafeToSpendClick()
        assertTrue(vm.uiState.value.safeToSpendExplainerOpen)

        vm.onSafeToSpendDismiss()
        assertFalse(vm.uiState.value.safeToSpendExplainerOpen)
    }

    @Test
    fun `the explainer closes by itself when the card is hidden`() = runTest {
        transactions.setTransactions(listOf(income("pay", 10_000_000L)))
        val vm = viewModel()
        vm.onSafeToSpendClick()

        vm.onPreviousMonth() // the card, and so its sheet, are not shown on a past month
        assertFalse(vm.uiState.value.safeToSpendExplainerOpen)
    }
}
