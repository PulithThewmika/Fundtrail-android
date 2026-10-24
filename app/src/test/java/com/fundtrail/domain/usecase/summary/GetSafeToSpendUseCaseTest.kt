package com.fundtrail.domain.usecase.summary

import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.MonthlySummary
import com.fundtrail.domain.model.NatureShare
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SafeToSpend
import com.fundtrail.domain.model.SafeToSpendParts
import com.fundtrail.domain.model.SafeToSpendState
import com.fundtrail.domain.model.SalaryStatus
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.SpendingBreakdown
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.UserSettings
import com.fundtrail.domain.repository.GoalRepository
import com.fundtrail.domain.usecase.goal.GetGoalProgressUseCase
import com.fundtrail.domain.usecase.income.GetSalaryStatusUseCase
import com.fundtrail.domain.usecase.income.ObserveSalaryExpectationUseCase
import com.fundtrail.fakes.FakeRecurringRepository
import com.fundtrail.fakes.FakeSettingsRepository
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val ZONE = ZoneId.of("Asia/Colombo")

/** Formula, states and edge cases of #215 (A-07, A-19, A-21, A-22, A-23). */
@OptIn(ExperimentalCoroutinesApi::class)
class GetSafeToSpendUseCaseTest {

    // ---- pure maths: GetSafeToSpendUseCase.compute ----

    private fun summary(received: Long, hasIncome: Boolean = received != 0L) = MonthlySummary(
        receivedIncomeMinor = received,
        spentMinor = 0L,
        netSavedMinor = received,
        incomeBySource = if (hasIncome) mapOf(IncomeSource.SALARY to received) else emptyMap(),
    )

    private fun breakdown(committed: Long = 0L, discretionary: Long = 0L) = SpendingBreakdown(
        totalSpentMinor = committed + discretionary,
        byNature = listOf(
            NatureShare(SpendKind.COMMITTED, committed, 0.0),
            NatureShare(SpendKind.DISCRETIONARY, discretionary, 0.0),
        ),
        categories = emptyList(),
    )

    private fun template(
        id: String = "rent",
        nextDue: LocalDate,
        amount: Long = 3_400_000L,
        paused: Boolean = false,
    ) = RecurringTemplate(
        id = id, name = id, categoryId = "cat_rent", accountId = "default-main-bank",
        expectedAmountMinor = amount, dayOfMonth = 1, nextDueDate = nextDue, isPaused = paused,
    )

    private fun compute(
        received: Long = 0L,
        committed: Long = 0L,
        discretionary: Long = 0L,
        goalSaving: Long = 0L,
        templates: List<RecurringTemplate> = emptyList(),
        salary: SalaryStatus = SalaryStatus.NotDue,
        salaryExpected: Boolean = true,
        hasIncome: Boolean = received != 0L,
        today: LocalDate = LocalDate.of(2026, 10, 24), // 8 days left including today
    ) = GetSafeToSpendUseCase.compute(
        summary = summary(received, hasIncome),
        breakdown = breakdown(committed, discretionary),
        goalSavingMinor = goalSaving,
        templates = templates,
        salaryStatus = salary,
        salaryExpected = salaryExpected,
        today = today,
    )

    @Test
    fun `available is received minus committed goal saving and discretionary`() {
        // The card copy example: LKR 18,400 safe to spend, approx LKR 2,300/day for 8 days.
        val result = compute(received = 3_000_000L, committed = 500_000L, goalSaving = 400_000L, discretionary = 260_000L)
        val parts = SafeToSpendParts(
            receivedMinor = 3_000_000L, committedMinor = 500_000L, goalSavingMinor = 400_000L, discretionarySpentMinor = 260_000L,
        )
        assertEquals(SafeToSpend(1_840_000L, 230_000L, SafeToSpendState.AVAILABLE, daysLeft = 8, parts = parts), result)
        assertEquals(0L, result.overByMinor)
    }

    @Test
    fun `fully used once the month is spent, with how far over`() {
        val result = compute(
            received = 1_000_000L, committed = 600_000L, discretionary = 720_000L, salary = SalaryStatus.Received(1_000_000L, null),
        )
        assertEquals(SafeToSpendState.FULLY_USED, result.state)
        assertEquals(-320_000L, result.amountMinor)
        assertEquals(320_000L, result.overByMinor)
        assertNull(result.perDayMinor)
    }

    @Test
    fun `exactly zero is fully used, not available`() {
        val result = compute(received = 1_000_000L, committed = 1_000_000L, salary = SalaryStatus.Received(1_000_000L, null))
        assertEquals(SafeToSpendState.FULLY_USED, result.state)
        assertEquals(0L, result.overByMinor)
        assertNull(result.perDayMinor)
    }

    @Test
    fun `waiting for salary while it is not due yet and nothing is left`() {
        // Early in the month: bills are reserved but the salary has not landed.
        val result = compute(
            received = 0L, hasIncome = false, salary = SalaryStatus.NotDue,
            templates = listOf(template(nextDue = LocalDate.of(2026, 10, 28))),
        )
        assertEquals(SafeToSpendState.WAITING_FOR_SALARY, result.state)
        assertNull(result.perDayMinor)
    }

    @Test
    fun `waiting for salary also when it is due but not recorded`() {
        val result = compute(
            received = 200_000L, committed = 500_000L, salary = SalaryStatus.Due(13_000_000L, 25),
        )
        assertEquals(SafeToSpendState.WAITING_FOR_SALARY, result.state)
    }

    @Test
    fun `a recorded salary ends the waiting state`() {
        val result = compute(received = 200_000L, committed = 500_000L, salary = SalaryStatus.Received(200_000L, null))
        assertEquals(SafeToSpendState.FULLY_USED, result.state)
    }

    @Test
    fun `positive amount is available even before the salary arrives`() {
        val result = compute(received = 900_000L, committed = 100_000L, salary = SalaryStatus.NotDue)
        assertEquals(SafeToSpendState.AVAILABLE, result.state)
    }

    @Test
    fun `no income and no salary expected is not enough data`() {
        val result = compute(received = 0L, hasIncome = false, salaryExpected = false)
        assertEquals(SafeToSpendState.NOT_ENOUGH_DATA, result.state)
        assertNull(result.perDayMinor)
    }

    @Test
    fun `no income but a salary expected is waiting, not first-run`() {
        val result = compute(received = 0L, hasIncome = false, salaryExpected = true, salary = SalaryStatus.NotDue)
        assertEquals(SafeToSpendState.WAITING_FOR_SALARY, result.state)
    }

    @Test
    fun `no expected salary and other income already used is fully used, not waiting`() {
        // A freelancer with no salary must not be told to wait for one.
        val result = compute(received = 500_000L, discretionary = 800_000L, salaryExpected = false, salary = SalaryStatus.Due(null, 25))
        assertEquals(SafeToSpendState.FULLY_USED, result.state)
        assertEquals(300_000L, result.overByMinor)
    }

    @Test
    fun `no goal means no goal saving is held back`() {
        assertEquals(1_000_000L, compute(received = 1_000_000L, goalSaving = 0L).amountMinor)
        assertEquals(600_000L, compute(received = 1_000_000L, goalSaving = 400_000L).amountMinor)
    }

    @Test
    fun `a recurring bill due later this month is reserved`() {
        val rent = template(nextDue = LocalDate.of(2026, 10, 28), amount = 3_400_000L)
        val result = compute(received = 10_000_000L, templates = listOf(rent))
        assertEquals(6_600_000L, result.amountMinor)
        assertEquals(3_400_000L, result.parts.committedMinor) // shown to the user as part of "committed"
    }

    @Test
    fun `a bill due today or overdue within the month is still reserved`() {
        val dueToday = template(id = "a", nextDue = LocalDate.of(2026, 10, 24), amount = 100_000L)
        val overdue = template(id = "b", nextDue = LocalDate.of(2026, 10, 3), amount = 200_000L)
        assertEquals(700_000L, compute(received = 1_000_000L, templates = listOf(dueToday, overdue)).amountMinor)
    }

    @Test
    fun `paused and other-month templates are not reserved`() {
        val paused = template(id = "p", nextDue = LocalDate.of(2026, 10, 28), paused = true)
        val nextMonth = template(id = "n", nextDue = LocalDate.of(2026, 11, 1))
        val lastMonthCatchUp = template(id = "c", nextDue = LocalDate.of(2026, 9, 25))
        assertEquals(
            1_000_000L,
            compute(received = 1_000_000L, templates = listOf(paused, nextMonth, lastMonthCatchUp)).amountMinor,
        )
    }

    @Test
    fun `a confirmed bill moves from reserved to recorded without being counted twice`() {
        val unconfirmed = template(nextDue = LocalDate.of(2026, 10, 28), amount = 3_400_000L)
        val before = compute(received = 10_000_000L, templates = listOf(unconfirmed))

        // Confirming writes the committed expense and advances the cursor to next month.
        val after = compute(received = 10_000_000L, committed = 3_400_000L, templates = listOf(unconfirmed.advanced()))

        assertEquals(6_600_000L, before.amountMinor)
        assertEquals(before.amountMinor, after.amountMinor)
    }

    @Test
    fun `last day of the month leaves one day and the whole amount per day`() {
        val result = compute(received = 1_000_000L, today = LocalDate.of(2026, 10, 31))
        assertEquals(1, result.daysLeft)
        assertEquals(1_000_000L, result.perDayMinor)
    }

    @Test
    fun `first day counts the whole month`() {
        assertEquals(31, compute(received = 1L, today = LocalDate.of(2026, 10, 1)).daysLeft)
        assertEquals(29, compute(received = 1L, today = LocalDate.of(2028, 2, 1)).daysLeft) // leap year
    }

    @Test
    fun `per day rounds half up`() {
        // 10 minor units over 4 days (28-31 Oct) is 2.5, which rounds up to 3.
        assertEquals(3L, compute(received = 10L, today = LocalDate.of(2026, 10, 28)).perDayMinor)
        // 9 over 4 is 2.25, which rounds down to 2.
        assertEquals(2L, compute(received = 9L, today = LocalDate.of(2026, 10, 28)).perDayMinor)
    }

    // ---- wired through the real use cases, so live updates and transaction filtering are covered ----

    private val clock = Clock.fixed(LocalDate.of(2026, 10, 20).atTime(10, 0).atZone(ZONE).toInstant(), ZONE)
    private val transactions = FakeTransactionRepository().apply { setTransactions(emptyList()) }
    private val recurring = FakeRecurringRepository().apply { setTemplates(emptyList()) }
    private val settings = FakeSettingsRepository()
    private val goals = StubGoalRepository()

    private fun useCase(clock: Clock = this.clock): GetSafeToSpendUseCase {
        val monthly = GetMonthlySummaryUseCase(transactions)
        return GetSafeToSpendUseCase(
            monthlySummary = monthly,
            spendingBreakdown = GetSpendingBreakdownUseCase(transactions),
            goalProgress = GetGoalProgressUseCase(goals, monthly, clock),
            recurring = recurring,
            salaryStatus = GetSalaryStatusUseCase(settings, transactions, clock),
            salaryExpectation = ObserveSalaryExpectationUseCase(settings),
            clock = clock,
        )
    }

    private fun at(day: Int) = LocalDate.of(2026, 10, day).atTime(12, 0).atZone(ZONE).toInstant().toEpochMilli()

    private fun income(id: String, amount: Long, source: IncomeSource = IncomeSource.SALARY, day: Int = 5) = Transaction(
        id = id, type = TransactionType.INCOME, amountLkrMinor = amount, accountId = "default-main-bank",
        incomeSource = source, occurredAt = at(day),
    )

    private fun expense(id: String, amount: Long, kind: SpendKind, day: Int = 6) = Transaction(
        id = id, type = TransactionType.EXPENSE, amountLkrMinor = amount, accountId = "default-main-bank",
        categoryId = "cat_food", spendKind = kind, occurredAt = at(day),
    )

    private fun transfer(id: String, amount: Long) = Transaction(
        id = id, type = TransactionType.TRANSFER, amountLkrMinor = amount, accountId = "default-main-bank",
        toAccountId = "default-savings", occurredAt = at(7),
    )

    @Test
    fun `wired - the month's entries feed the formula`() = runTest {
        transactions.setTransactions(
            listOf(
                income("pay", 10_000_000L),
                expense("rent", 2_000_000L, SpendKind.COMMITTED),
                expense("coffee", 300_000L, SpendKind.DISCRETIONARY),
            ),
        )
        val result = useCase()().first()
        assertEquals(7_700_000L, result.amountMinor)
        assertEquals(SafeToSpendState.AVAILABLE, result.state)
        assertEquals(12, result.daysLeft) // 20 Oct to 31 Oct
        assertEquals(641_667L, result.perDayMinor) // 7,700,000 / 12 = 641,666.67
    }

    @Test
    fun `wired - transfers never change the amount`() = runTest {
        val base = listOf(income("pay", 10_000_000L), expense("coffee", 300_000L, SpendKind.DISCRETIONARY))
        transactions.setTransactions(base)
        val without = useCase()().first().amountMinor

        transactions.setTransactions(base + transfer("move", 4_000_000L))
        assertEquals(without, useCase()().first().amountMinor)
    }

    @Test
    fun `wired - a crypto loss reduces income`() = runTest {
        transactions.setTransactions(
            listOf(income("pay", 10_000_000L), income("loss", -2_000_000L, source = IncomeSource.CRYPTO)),
        )
        assertEquals(8_000_000L, useCase()().first().amountMinor)
    }

    @Test
    fun `wired - a confirmed recurring entry is not counted twice`() = runTest {
        val pay = income("pay", 10_000_000L)
        transactions.setTransactions(listOf(pay))
        recurring.setTemplates(listOf(template(nextDue = LocalDate.of(2026, 10, 25), amount = 3_400_000L)))

        val before = useCase()().first()
        assertEquals(6_600_000L, before.amountMinor) // reserved, not yet paid

        recurring.confirm("rent", 3_400_000L, at(25), LocalDate.of(2026, 10, 25))
        transactions.setTransactions(listOf(pay) + recurring.recordedTransactions)

        assertEquals(before.amountMinor, useCase()().first().amountMinor) // now recorded, no longer reserved
    }

    @Test
    fun `wired - an active goal holds back its required monthly saving`() = runTest {
        transactions.setTransactions(listOf(income("pay", 10_000_000L)))
        goals.goal.value = Goal(
            id = "g", name = "MacBook", targetLkrMinor = 60_000_000L, deadline = LocalDate.of(2026, 12, 31),
            baselineSavedMinor = 0L, createdAt = LocalDate.of(2026, 10, 1).atStartOfDay(ZONE).toInstant().toEpochMilli(),
        )
        val required = GetGoalProgressUseCase(goals, GetMonthlySummaryUseCase(transactions), clock)().first()!!.requiredMonthlyMinor

        assertEquals(10_000_000L - required, useCase()().first().amountMinor)
    }

    @Test
    fun `wired - no goal holds nothing back`() = runTest {
        transactions.setTransactions(listOf(income("pay", 10_000_000L)))
        goals.goal.value = null
        assertEquals(10_000_000L, useCase()().first().amountMinor)
    }

    @Test
    fun `wired - waiting for salary before any income, first-run without an expected salary`() = runTest {
        settings.update(UserSettings(expectedSalaryMinor = 13_000_000L, salaryPayday = 25))
        recurring.setTemplates(listOf(template(nextDue = LocalDate.of(2026, 10, 28))))
        assertEquals(SafeToSpendState.WAITING_FOR_SALARY, useCase()().first().state)

        settings.update(UserSettings(expectedSalaryMinor = null, salaryPayday = 25))
        recurring.setTemplates(emptyList())
        assertEquals(SafeToSpendState.NOT_ENOUGH_DATA, useCase()().first().state)
    }

    @Test
    fun `wired - updates live after an entry, a recurring change and a goal change`() = runTest {
        val seen = mutableListOf<SafeToSpend>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase()().collect { seen += it } }

        transactions.setTransactions(listOf(income("pay", 10_000_000L)))
        assertEquals(10_000_000L, seen.last().amountMinor)

        transactions.setTransactions(listOf(income("pay", 10_000_000L), expense("lunch", 400_000L, SpendKind.DISCRETIONARY)))
        assertEquals(9_600_000L, seen.last().amountMinor)

        recurring.setTemplates(listOf(template(nextDue = LocalDate.of(2026, 10, 28), amount = 1_000_000L)))
        assertEquals(8_600_000L, seen.last().amountMinor)

        goals.goal.value = Goal(
            id = "g", name = "MacBook", targetLkrMinor = 60_000_000L, deadline = LocalDate.of(2026, 12, 31),
            baselineSavedMinor = 0L, createdAt = LocalDate.of(2026, 10, 1).atStartOfDay(ZONE).toInstant().toEpochMilli(),
        )
        assertTrue(seen.last().amountMinor < 8_600_000L)
    }

    @Test
    fun `wired - the month rolls over at midnight in Colombo, not UTC`() = runTest {
        // 19:00 UTC on 31 Oct is 00:30 on 1 Nov in Colombo.
        val colomboNovember = Clock.fixed(Instant.parse("2026-10-31T19:00:00Z"), ZONE)
        assertEquals(30, useCase(colomboNovember)().first().daysLeft)
    }

    private class StubGoalRepository : GoalRepository {
        val goal = MutableStateFlow<Goal?>(null)
        override fun observeActive(): Flow<Goal?> = goal
        override suspend fun save(goal: Goal): Result<Unit> = Result.Success(Unit)
        override suspend fun archive(id: String): Result<Unit> = Result.Success(Unit)
    }
}
