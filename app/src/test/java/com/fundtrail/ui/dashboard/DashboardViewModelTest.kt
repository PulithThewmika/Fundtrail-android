package com.fundtrail.ui.dashboard

import com.fundtrail.R
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.SyncStatus
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.toMillisRange
import com.fundtrail.domain.repository.GoalRepository
import com.fundtrail.domain.repository.TransactionRepository
import com.fundtrail.domain.usecase.category.ObserveCategoriesUseCase
import com.fundtrail.domain.usecase.goal.GetGoalProgressUseCase
import com.fundtrail.domain.usecase.goal.ObserveActiveGoalUseCase
import com.fundtrail.domain.usecase.summary.GetMonthlySummaryUseCase
import com.fundtrail.domain.usecase.summary.GetSpendingBreakdownUseCase
import com.fundtrail.domain.usecase.sync.ObserveSyncStatusUseCase
import com.fundtrail.fakes.FakeCategoryRepository
import com.fundtrail.fakes.FakeSyncStatusRepository
import com.fundtrail.fakes.MainDispatcherRule
import com.fundtrail.ui.common.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class DashboardViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    /** Starts empty, unlike the Kavindu fake, so tests don't depend on today's date. */
    private class InMemoryTransactions : TransactionRepository {
        val all = MutableStateFlow<List<Transaction>>(emptyList())
        var failing = false

        override fun observeMonth(yearMonth: YearMonth): Flow<List<Transaction>> {
            if (failing) return flow { throw IOException("disk") }
            val (start, end) = yearMonth.toMillisRange()
            return all.map { list -> list.filter { it.occurredAt in start until end } }
        }

        override fun observeById(id: String) = all.map { list -> list.find { it.id == id } }
        override fun observeLastUsedAccountId(): Flow<String?> = all.map { null }
        override suspend fun add(transaction: Transaction): Result<Unit> {
            all.value += transaction
            return Result.Success(Unit)
        }
        override suspend fun update(transaction: Transaction): Result<Unit> = Result.Success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.Success(Unit)
        override suspend fun restore(id: String): Result<Unit> = Result.Success(Unit)
    }

    /** Starts with no active goal, like InMemoryTransactions, so tests don't depend on today's date. */
    private class InMemoryGoals : GoalRepository {
        val active = MutableStateFlow<Goal?>(null)
        override fun observeActive(): Flow<Goal?> = active
        override suspend fun save(goal: Goal): Result<Unit> {
            active.value = goal
            return Result.Success(Unit)
        }
        override suspend fun archive(id: String): Result<Unit> {
            active.value = null
            return Result.Success(Unit)
        }
    }

    private val transactions = InMemoryTransactions()
    private val sync = FakeSyncStatusRepository()
    private val goals = InMemoryGoals()
    private val thisMonth = YearMonth.now(APP_ZONE)

    // Floored to midnight so it stays inside thisMonth regardless of the time of day the suite runs.
    private val fixedClock = Clock.fixed(LocalDate.now(APP_ZONE).atStartOfDay(APP_ZONE).toInstant(), APP_ZONE)

    private fun TestScope.viewModel() = DashboardViewModel(
        GetMonthlySummaryUseCase(transactions),
        GetSpendingBreakdownUseCase(transactions),
        ObserveCategoriesUseCase(FakeCategoryRepository()),
        ObserveSyncStatusUseCase(sync),
        ObserveActiveGoalUseCase(goals),
        GetGoalProgressUseCase(goals, GetMonthlySummaryUseCase(transactions), fixedClock),
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} } }

    private fun dayIn(month: YearMonth) = month.toMillisRange().first + 3_600_000L

    private fun expense(id: String, amount: Long, category: String, kind: SpendKind, month: YearMonth = thisMonth) =
        Transaction(id = id, type = TransactionType.EXPENSE, amountLkrMinor = amount, accountId = "acc_main_bank",
            categoryId = category, spendKind = kind, occurredAt = dayIn(month))

    private fun income(id: String, amount: Long, source: IncomeSource, month: YearMonth = thisMonth) =
        Transaction(id = id, type = TransactionType.INCOME, amountLkrMinor = amount, accountId = "acc_main_bank",
            incomeSource = source, occurredAt = dayIn(month))

    private fun goal(
        target: Long,
        baseline: Long = 0L,
        createdMonthsAgo: Long = 1,
        deadline: LocalDate = LocalDate.now(APP_ZONE).plusYears(1),
    ) = Goal(
        id = "g1",
        name = "MacBook Pro M4",
        targetLkrMinor = target,
        deadline = deadline,
        baselineSavedMinor = baseline,
        createdAt = thisMonth.minusMonths(createdMonthsAgo).atDay(1).atStartOfDay(APP_ZONE).toInstant().toEpochMilli(),
    )

    private fun DashboardViewModel.data() = (uiState.value.content as UiState.Success).data
    private fun DashboardViewModel.goalData() = (uiState.value.goal as UiState.Success).data

    @Test
    fun `no data after the first sync is the empty state`() = runTest {
        assertEquals(UiState.Empty, viewModel().uiState.value.content)
    }

    @Test
    fun `no data while the first sync runs is loading`() = runTest {
        sync.status.value = SyncStatus(initialSyncDone = false, isOnline = true)
        assertEquals(UiState.Loading, viewModel().uiState.value.content)
    }

    @Test
    fun `never synced and offline shows the offline message, not an empty screen`() = runTest {
        sync.status.value = SyncStatus(initialSyncDone = false, isOnline = false)
        assertEquals(UiState.Error(R.string.offline_no_data), viewModel().uiState.value.content)
    }

    @Test
    fun `cached data wins over being offline`() = runTest {
        sync.status.value = SyncStatus(initialSyncDone = false, isOnline = false)
        transactions.add(expense("e1", 100_000L, "cat_rent", SpendKind.COMMITTED))
        assertTrue(viewModel().uiState.value.content is UiState.Success)
    }

    @Test
    fun `totals, split and ranking come from the use cases and transfers are ignored`() = runTest {
        transactions.add(income("salary", 13_000_000L, IncomeSource.SALARY))
        transactions.add(income("gig", 2_000_000L, IncomeSource.FREELANCE))
        transactions.add(expense("rent", 3_400_000L, "cat_rent", SpendKind.COMMITTED))
        transactions.add(expense("coffee", 2_400_000L, "cat_coffee_dining", SpendKind.DISCRETIONARY))
        transactions.add(expense("mystery", 200_000L, "cat_not_synced_yet", SpendKind.DISCRETIONARY))
        transactions.add(
            Transaction(id = "move", type = TransactionType.TRANSFER, amountLkrMinor = 5_000_000L, accountId = "acc_main_bank",
                toAccountId = "acc_secondary_bank", occurredAt = dayIn(thisMonth)),
        )

        val d = viewModel().data()
        assertEquals(15_000_000L, d.receivedMinor)
        assertEquals(6_000_000L, d.spentMinor)
        assertEquals(9_000_000L, d.savedMinor)
        assertEquals(listOf(IncomeSource.SALARY to 13_000_000L, IncomeSource.FREELANCE to 2_000_000L), d.incomeBySource)
        assertEquals(3_400_000L, d.committedMinor)
        assertEquals(2_600_000L, d.discretionaryMinor)
        assertEquals(
            listOf(
                RankedCategory("Rent", SpendKind.COMMITTED, 3_400_000L, 57),
                RankedCategory("Coffee & Dining", SpendKind.DISCRETIONARY, 2_400_000L, 40),
                RankedCategory(null, SpendKind.DISCRETIONARY, 200_000L, 3),
            ),
            d.topCategories,
        )
    }

    @Test
    fun `negative saving is passed through as is`() = runTest {
        transactions.add(income("gig", 1_000_000L, IncomeSource.FREELANCE))
        transactions.add(expense("rent", 3_400_000L, "cat_rent", SpendKind.COMMITTED))
        assertEquals(-2_400_000L, viewModel().data().savedMinor)
    }

    @Test
    fun `only the top five categories are listed`() = runTest {
        (1..7).forEach { transactions.add(expense("e$it", it * 1_000L, "cat_$it", SpendKind.DISCRETIONARY)) }
        assertEquals(listOf(7_000L, 6_000L, 5_000L, 4_000L, 3_000L), viewModel().data().topCategories.map { it.amountMinor })
    }

    @Test
    fun `a new entry shows up without a refresh`() = runTest {
        val vm = viewModel()
        assertEquals(UiState.Empty, vm.uiState.value.content)
        transactions.add(expense("e1", 150_000L, "cat_rent", SpendKind.COMMITTED))
        assertEquals(150_000L, vm.data().spentMinor)
    }

    @Test
    fun `month switcher goes back but never past the current month`() = runTest {
        val lastMonth = thisMonth.minusMonths(1)
        transactions.add(expense("old", 90_000L, "cat_rent", SpendKind.COMMITTED, month = lastMonth))
        val vm = viewModel()
        assertFalse(vm.uiState.value.canGoNext)

        vm.onNextMonth()
        assertEquals(thisMonth, vm.uiState.value.month)

        vm.onPreviousMonth()
        assertEquals(lastMonth, vm.uiState.value.month)
        assertTrue(vm.uiState.value.canGoNext)
        assertEquals(90_000L, vm.data().spentMinor)

        vm.onNextMonth()
        assertEquals(UiState.Empty, vm.uiState.value.content)
    }

    @Test
    fun `a failed read shows an error and retry recovers`() = runTest {
        transactions.failing = true
        val vm = viewModel()
        assertEquals(UiState.Error(R.string.error_network), vm.uiState.value.content)

        transactions.failing = false
        vm.onRetry()
        assertEquals(UiState.Empty, vm.uiState.value.content)
    }

    @Test
    fun `an active goal shows required pace percent and status`() = runTest {
        val lastMonth = thisMonth.minusMonths(1)
        transactions.add(income("salary", 3_000_000L, IncomeSource.SALARY, month = lastMonth))
        transactions.add(expense("rent", 1_000_000L, "cat_rent", SpendKind.COMMITTED, month = lastMonth))
        goals.active.value = goal(target = 100_000_000L, baseline = 0L)

        val d = viewModel().goalData()
        assertEquals(2_000_000L, d.actualPaceMinor) // last month's net saving: 3,000,000 - 1,000,000
        assertEquals(2, d.percent) // 2,000,000 saved of a 100,000,000 target
        assertTrue(d.requiredMonthlyMinor > 0L)
    }

    @Test
    fun `no active goal shows the set-a-goal empty state`() = runTest {
        assertEquals(UiState.Empty, viewModel().uiState.value.goal)
    }

    @Test
    fun `the goal card does not reset when the month changes`() = runTest {
        val lastMonth = thisMonth.minusMonths(1)
        transactions.add(income("salary", 3_000_000L, IncomeSource.SALARY, month = lastMonth))
        goals.active.value = goal(target = 100_000_000L)

        val vm = viewModel()
        val before = vm.goalData()
        vm.onPreviousMonth()
        assertEquals(before, vm.goalData())
    }

    @Test
    fun `an infeasible goal maps to needs adjusting, not behind`() = runTest {
        val lastMonth = thisMonth.minusMonths(1)
        // Spent more than received: pace is negative, so the gap can never close (ADR-0006).
        transactions.add(income("gig", 1_000_000L, IncomeSource.FREELANCE, month = lastMonth))
        transactions.add(expense("rent", 3_000_000L, "cat_rent", SpendKind.COMMITTED, month = lastMonth))
        goals.active.value = goal(target = 100_000_000L)

        assertEquals(GoalCardStatus.NEEDS_ADJUSTING, viewModel().goalData().status)
    }

    @Test
    fun `a feasible but slow pace maps to behind, not needs adjusting`() = runTest {
        val lastMonth = thisMonth.minusMonths(1)
        // Pace (500,000) is positive and required (1,000,000) is well under half of income (5,000,000):
        // feasible, but pace is under 90% of required, so BEHIND, not NEEDS_ADJUSTING (ADR-0006).
        transactions.add(income("salary", 5_000_000L, IncomeSource.SALARY, month = lastMonth))
        transactions.add(expense("rent", 4_500_000L, "cat_rent", SpendKind.COMMITTED, month = lastMonth))
        goals.active.value = goal(target = 2_500_000L, deadline = thisMonth.plusMonths(1).atEndOfMonth())

        assertEquals(GoalCardStatus.BEHIND, viewModel().goalData().status)
    }

    @Test
    fun `saved past the target clamps percent to 100`() = runTest {
        goals.active.value = goal(target = 100_000_000L, baseline = 200_000_000L)
        assertEquals(100, viewModel().goalData().percent)
    }

    @Test
    fun `a non-positive target reads as 0 percent, not a crash`() = runTest {
        goals.active.value = goal(target = 0L)
        assertEquals(0, viewModel().goalData().percent)
    }
}
