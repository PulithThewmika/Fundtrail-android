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
import com.fundtrail.domain.model.GoalProgress
import com.fundtrail.domain.model.GoalStatus
import com.fundtrail.domain.usecase.goal.GetGoalProgressUseCase
import com.fundtrail.domain.usecase.goal.ObserveActiveGoalUseCase
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
            activeGoal = ObserveActiveGoalUseCase(goals),
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

    /** Target 600,000 by 31 Dec; "today" is 20 Oct, so the goal has 3 months left counting October. */
    private fun macBookGoal(createdOn: LocalDate = LocalDate.of(2026, 10, 1), baselineMinor: Long = 0L) = Goal(
        id = "g", name = "MacBook", targetLkrMinor = 60_000_000L, deadline = LocalDate.of(2026, 12, 31),
        baselineSavedMinor = baselineMinor, createdAt = createdOn.atStartOfDay(ZONE).toInstant().toEpochMilli(),
    )

    @Test
    fun `wired - an active goal holds back what it still needs per remaining month`() = runTest {
        transactions.setTransactions(listOf(income("pay", 30_000_000L)))
        goals.goal.value = macBookGoal()

        // Nothing saved before October: 60,000,000 over Oct, Nov and Dec is 20,000,000 a month.
        assertEquals(30_000_000L - 20_000_000L, useCase()().first().amountMinor)
    }

    @Test
    fun `wired - with a goal, an expense lowers safe to spend by exactly its own amount`() = runTest {
        // The goal's own required-per-month moves with this month's flows; safe to spend must not count them twice.
        goals.goal.value = macBookGoal()
        transactions.setTransactions(listOf(income("pay", 30_000_000L)))
        val before = useCase()().first().amountMinor

        transactions.setTransactions(listOf(income("pay", 30_000_000L), expense("coffee", 400_000L, SpendKind.DISCRETIONARY)))
        assertEquals(before - 400_000L, useCase()().first().amountMinor)

        transactions.setTransactions(
            listOf(income("pay", 30_000_000L), income("extra", 1_000_000L, source = IncomeSource.FREELANCE)),
        )
        assertEquals(before + 1_000_000L, useCase()().first().amountMinor) // and extra income raises it by exactly that
    }

    @Test
    fun `wired - last month's saving lowers what the goal still needs`() = runTest {
        // Goal started in September and 5,000,000 net was saved that month.
        goals.goal.value = macBookGoal(createdOn = LocalDate.of(2026, 9, 1))
        val september = LocalDate.of(2026, 9, 10).atTime(12, 0).atZone(ZONE).toInstant().toEpochMilli()
        transactions.setTransactions(
            listOf(
                income("pay", 30_000_000L),
                income("sep", 5_000_000L).copy(occurredAt = september),
            ),
        )

        // Remaining 55,000,000 over 3 months is 18,333,333.33, rounded up.
        assertEquals(30_000_000L - 18_333_334L, useCase()().first().amountMinor)
    }

    // ---- goalSavingFor: the goal's share, from what was saved before this month ----

    private fun progress(savedToDateMinor: Long) = GoalProgress(
        savedToDateMinor = savedToDateMinor, remainingMinor = 0L, requiredMonthlyMinor = 0L, actualPaceMinor = null,
        status = GoalStatus.NOT_ENOUGH_DATA, projectedDate = null, isInfeasible = false,
        suggestedDeadline = null, suggestedTargetMinor = null,
    )

    private fun goalSaving(
        savedToDate: Long,
        thisMonthNet: Long,
        today: LocalDate = LocalDate.of(2026, 10, 20),
        target: Long = 6_000_000L,
        deadline: LocalDate = LocalDate.of(2026, 12, 31),
    ) = GetSafeToSpendUseCase.goalSavingFor(
        goal = Goal("g", "MacBook", target, deadline, baselineSavedMinor = 0L),
        progress = progress(savedToDate),
        thisMonthNetSavedMinor = thisMonthNet,
        today = today,
    )

    @Test
    fun `goal saving is zero without a goal`() {
        assertEquals(0L, GetSafeToSpendUseCase.goalSavingFor(null, progress(1L), 0L, LocalDate.of(2026, 10, 20)))
        assertEquals(
            0L,
            GetSafeToSpendUseCase.goalSavingFor(Goal("g", "MacBook", 6_000_000L, LocalDate.of(2026, 12, 31), 0L), null, 0L, LocalDate.of(2026, 10, 20)),
        )
    }

    @Test
    fun `goal saving leaves this month's net saved out`() {
        // 1,700,000 saved to date includes 200,000 from this month, so 1,500,000 was saved before it.
        // 4,500,000 still needed over Oct, Nov and Dec is 1,500,000 a month.
        assertEquals(1_500_000L, goalSaving(savedToDate = 1_700_000L, thisMonthNet = 200_000L))
    }

    @Test
    fun `goal saving does not change with this month's spending or income`() {
        val saved = 1_500_000L // saved before this month
        val quiet = goalSaving(savedToDate = saved + 0L, thisMonthNet = 0L)
        val spentALot = goalSaving(savedToDate = saved - 900_000L, thisMonthNet = -900_000L)
        val earnedALot = goalSaving(savedToDate = saved + 2_000_000L, thisMonthNet = 2_000_000L)
        assertEquals(quiet, spentALot)
        assertEquals(quiet, earnedALot)
    }

    @Test
    fun `goal saving in the deadline month is the whole remaining amount`() {
        assertEquals(4_500_000L, goalSaving(savedToDate = 1_500_000L, thisMonthNet = 0L, today = LocalDate.of(2026, 12, 5)))
    }

    @Test
    fun `goal saving after the deadline month still needs the whole remaining amount, never divides by zero`() {
        assertEquals(4_500_000L, goalSaving(savedToDate = 1_500_000L, thisMonthNet = 0L, today = LocalDate.of(2027, 1, 10)))
    }

    @Test
    fun `goal saving is zero once last month's saving already reached the target`() {
        assertEquals(0L, goalSaving(savedToDate = 6_200_000L, thisMonthNet = 200_000L))
    }

    @Test
    fun `a goal reached only by this month's saving still holds money back until the month is over`() {
        // 6,500,000 saved to date, 1,000,000 of it this month: 5,500,000 before, so 500,000 is still needed.
        assertEquals(166_667L, goalSaving(savedToDate = 6_500_000L, thisMonthNet = 1_000_000L)) // 500,000 / 3, rounded up
    }

    @Test
    fun `goal saving rounds up so the goal is never short`() {
        assertEquals(34L, goalSaving(savedToDate = 5_999_900L, thisMonthNet = 0L)) // 100 over 3 months
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
