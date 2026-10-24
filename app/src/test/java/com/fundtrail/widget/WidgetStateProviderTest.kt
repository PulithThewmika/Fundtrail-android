package com.fundtrail.widget

import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SafeToSpendState
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.RecurringRepository
import com.fundtrail.domain.repository.GoalRepository
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.usecase.goal.GetGoalProgressUseCase
import com.fundtrail.domain.usecase.goal.ObserveActiveGoalUseCase
import com.fundtrail.domain.usecase.income.GetSalaryStatusUseCase
import com.fundtrail.domain.usecase.income.ObserveSalaryExpectationUseCase
import com.fundtrail.domain.usecase.summary.GetMonthlySummaryUseCase
import com.fundtrail.domain.usecase.summary.GetSafeToSpendUseCase
import com.fundtrail.domain.usecase.summary.GetSpendingBreakdownUseCase
import com.fundtrail.domain.usecase.summary.GetTodaySpendUseCase
import com.fundtrail.fakes.FakeRecurringRepository
import com.fundtrail.fakes.FakeSettingsRepository
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

private val ZONE = ZoneId.of("Asia/Colombo")

/** What the home-screen widget is handed to draw (#226, #228). */
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetStateProviderTest {

    private class StubAuth(uid: String?) : AuthRepository {
        val uid = MutableStateFlow(uid)
        override val currentUserId: Flow<String?> = this.uid
        override val currentUserDisplayName: String? = null
        override suspend fun signIn(email: String, password: String): Result<Unit> = Result.Success(Unit)
        override suspend fun register(email: String, password: String): Result<Unit> = Result.Success(Unit)
        override suspend fun signInWithGoogle(idToken: String): Result<Unit> = Result.Success(Unit)
        override suspend fun signOut(): Result<Unit> = Result.Success(Unit)
    }

    private class NoGoal : GoalRepository {
        override fun observeActive(): Flow<Goal?> = MutableStateFlow(null)
        override suspend fun save(goal: Goal): Result<Unit> = Result.Success(Unit)
        override suspend fun archive(id: String): Result<Unit> = Result.Success(Unit)
    }

    /** Only the recurring list fails, so Safe to spend breaks but today's spend still works. */
    private class FailingRecurring : RecurringRepository {
        override fun observeAll(): Flow<List<RecurringTemplate>> = flow { throw IOException("disk") }
        override suspend fun add(template: RecurringTemplate): Result<Unit> = Result.Success(Unit)
        override suspend fun update(template: RecurringTemplate): Result<Unit> = Result.Success(Unit)
        override suspend fun pause(id: String): Result<Unit> = Result.Success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.Success(Unit)
        override suspend fun confirm(templateId: String, actualAmountMinor: Long, date: Long, expectedDueDate: LocalDate): Result<Unit> =
            Result.Success(Unit)
        override suspend fun skip(templateId: String, expectedDueDate: LocalDate): Result<Unit> = Result.Success(Unit)
    }

    private val today = LocalDate.of(2026, 10, 20)
    private val clock = Clock.fixed(today.atTime(14, 0).atZone(ZONE).toInstant(), ZONE)
    private val transactions = FakeTransactionRepository().apply { setTransactions(emptyList()) }
    private val settings = FakeSettingsRepository()
    private val goals = NoGoal()

    private fun provider(auth: AuthRepository, recurring: RecurringRepository = FakeRecurringRepository().apply { setTemplates(emptyList()) }): WidgetStateProvider {
        val monthly = GetMonthlySummaryUseCase(transactions)
        return WidgetStateProvider(
            auth,
            GetTodaySpendUseCase(transactions, clock),
            GetSafeToSpendUseCase(
                monthly,
                GetSpendingBreakdownUseCase(transactions),
                GetGoalProgressUseCase(goals, monthly, clock),
                ObserveActiveGoalUseCase(goals),
                recurring,
                GetSalaryStatusUseCase(settings, transactions, clock),
                ObserveSalaryExpectationUseCase(settings),
                clock,
            ),
        )
    }

    private fun at(day: Int) = LocalDate.of(2026, 10, day).atTime(12, 0).atZone(ZONE).toInstant().toEpochMilli()

    private fun income(amount: Long) = Transaction(
        id = "pay", type = TransactionType.INCOME, amountLkrMinor = amount, accountId = "default-main-bank",
        incomeSource = IncomeSource.SALARY, occurredAt = at(5),
    )

    private fun expense(id: String, amount: Long, day: Int) = Transaction(
        id = id, type = TransactionType.EXPENSE, amountLkrMinor = amount, accountId = "default-main-bank",
        categoryId = "cat_food", spendKind = SpendKind.DISCRETIONARY, occurredAt = at(day),
    )

    @Test
    fun `signed out shows the sign-in state`() = runTest {
        assertEquals(WidgetState.SignedOut, provider(StubAuth(null)).load())
    }

    @Test
    fun `signed in shows today's spend and safe to spend`() = runTest {
        transactions.setTransactions(listOf(income(10_000_000L), expense("lunch", 150_000L, day = 20), expense("old", 50_000L, day = 6)))
        val state = provider(StubAuth("u1")).load() as WidgetState.Ready

        assertEquals(150_000L, state.todaySpendMinor) // only today's lunch
        assertEquals(SafeToSpendState.AVAILABLE, state.safeToSpend?.state)
        assertEquals(9_800_000L, state.safeToSpend?.amountMinor) // 10,000,000 - 150,000 - 50,000
    }

    @Test
    fun `safe to spend failing still shows today's spend`() = runTest {
        transactions.setTransactions(listOf(income(10_000_000L), expense("lunch", 150_000L, day = 20)))
        val state = provider(StubAuth("u1"), FailingRecurring()).load() as WidgetState.Ready

        assertEquals(150_000L, state.todaySpendMinor)
        assertNull(state.safeToSpend)
    }

    @Test
    fun `an unreadable today's spend is unavailable, not a crash`() = runTest {
        val broken = object : com.fundtrail.domain.repository.TransactionRepository by transactions {
            override fun observeMonth(yearMonth: java.time.YearMonth) = flow<List<Transaction>> { throw IOException("disk") }
        }
        val failing = WidgetStateProvider(
            StubAuth("u1"),
            GetTodaySpendUseCase(broken, clock),
            GetSafeToSpendUseCaseStub.create(broken, clock, settings, goals),
        )
        assertEquals(WidgetState.Unavailable, failing.load())
    }

    @Test
    fun `sign out and sign in change the state live`() = runTest {
        val auth = StubAuth("u1")
        val seen = mutableListOf<WidgetState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { provider(auth).observe().collect { seen += it } }

        assertTrue(seen.last() is WidgetState.Ready)
        auth.uid.value = null
        assertEquals(WidgetState.SignedOut, seen.last())
        auth.uid.value = "u1"
        assertTrue(seen.last() is WidgetState.Ready)
    }

    @Test
    fun `a new entry changes the state live`() = runTest {
        val seen = mutableListOf<WidgetState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { provider(StubAuth("u1")).observe().collect { seen += it } }
        assertEquals(0L, (seen.last() as WidgetState.Ready).todaySpendMinor)

        transactions.setTransactions(listOf(expense("lunch", 150_000L, day = 20)))
        assertEquals(150_000L, (seen.last() as WidgetState.Ready).todaySpendMinor)
    }
}

/** Builds a real [GetSafeToSpendUseCase] over the given fakes (the failing test only needs it to exist). */
private object GetSafeToSpendUseCaseStub {
    fun create(
        transactions: com.fundtrail.domain.repository.TransactionRepository,
        clock: Clock,
        settings: FakeSettingsRepository,
        goals: GoalRepository,
    ): GetSafeToSpendUseCase {
        val monthly = GetMonthlySummaryUseCase(transactions)
        return GetSafeToSpendUseCase(
            monthly,
            GetSpendingBreakdownUseCase(transactions),
            GetGoalProgressUseCase(goals, monthly, clock),
            ObserveActiveGoalUseCase(goals),
            FakeRecurringRepository().apply { setTemplates(emptyList()) },
            GetSalaryStatusUseCase(settings, transactions, clock),
            ObserveSalaryExpectationUseCase(settings),
            clock,
        )
    }
}
