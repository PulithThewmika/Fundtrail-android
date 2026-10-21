package com.fundtrail.domain.usecase.goal

import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.GoalStatus
import com.fundtrail.domain.model.MonthlySummary
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeGoalRepository
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private val ZONE = ZoneId.of("Asia/Colombo")

/** Scenario and S1-S3 acceptance figures (#162-#165; ADR-0006). */
class GetGoalProgressUseCaseTest {

    private fun goal(
        target: Long = 490_000L,
        baseline: Long = 11_200L,
        deadline: LocalDate,
        createdAt: Long,
    ) = Goal(id = "g1", name = "MacBook", targetLkrMinor = target, deadline = deadline, baselineSavedMinor = baseline, createdAt = createdAt)

    private fun createdAt(month: YearMonth) = month.atDay(1).atStartOfDay(ZONE).toInstant().toEpochMilli()

    private fun summary(net: Long, income: Long = net.coerceAtLeast(0)) =
        MonthlySummary(receivedIncomeMinor = income, spentMinor = income - net, netSavedMinor = net, incomeBySource = emptyMap())

    @Test
    fun `scenario figures give required monthly 39900`() {
        val today = LocalDate.of(2026, 1, 1) // goal just created, 12 months (Jan-Dec) to the deadline
        val g = goal(deadline = LocalDate.of(2026, 12, 15), createdAt = createdAt(YearMonth.from(today)))
        val progress = GetGoalProgressUseCase.compute(g, summaries = emptyMap(), today = today)
        assertEquals(39_900L, progress.requiredMonthlyMinor)
        assertEquals(478_800L, progress.remainingMinor)
        assertEquals(GoalStatus.NOT_ENOUGH_DATA, progress.status)
    }

    @Test
    fun `no complete month yet is NOT_ENOUGH_DATA`() {
        val today = LocalDate.of(2026, 3, 10)
        val g = goal(deadline = LocalDate.of(2027, 1, 1), createdAt = createdAt(YearMonth.from(today)))
        val progress = GetGoalProgressUseCase.compute(g, emptyMap(), today)
        assertEquals(GoalStatus.NOT_ENOUGH_DATA, progress.status)
        assertNull(progress.actualPaceMinor)
        assertNull(progress.projectedDate)
    }

    @Test
    fun `pace at or above 110 percent of required is AHEAD`() {
        val today = LocalDate.of(2026, 4, 1)
        val creationMonth = YearMonth.of(2026, 1)
        // savedToDate already counts Jan-Mar's net saved, so target is sized to leave required = 10,000
        // (remaining 70,000 over the 7 months April-October) against a 12,000 pace (>= 1.1x).
        val g = goal(target = 106_000L, baseline = 0L, deadline = YearMonth.of(2026, 10).atEndOfMonth(), createdAt = createdAt(creationMonth))
        val summaries = mapOf(
            YearMonth.of(2026, 1) to summary(12_000L),
            YearMonth.of(2026, 2) to summary(12_000L),
            YearMonth.of(2026, 3) to summary(12_000L),
        )
        val progress = GetGoalProgressUseCase.compute(g, summaries, today)
        assertEquals(10_000L, progress.requiredMonthlyMinor)
        assertEquals(GoalStatus.AHEAD, progress.status)
        assertEquals(12_000L, progress.actualPaceMinor)
    }

    @Test
    fun `pace between 90 and 110 percent is ON_TRACK`() {
        val today = LocalDate.of(2026, 4, 1)
        val creationMonth = YearMonth.of(2026, 1)
        // required ends up at 10,000 (as above); pace 10,500 sits within [0.9x, 1.1x) of it.
        val g = goal(target = 101_500L, baseline = 0L, deadline = YearMonth.of(2026, 10).atEndOfMonth(), createdAt = createdAt(creationMonth))
        val summaries = mapOf(
            YearMonth.of(2026, 1) to summary(10_500L),
            YearMonth.of(2026, 2) to summary(10_500L),
            YearMonth.of(2026, 3) to summary(10_500L),
        )
        val progress = GetGoalProgressUseCase.compute(g, summaries, today)
        assertEquals(10_000L, progress.requiredMonthlyMinor)
        assertEquals(GoalStatus.ON_TRACK, progress.status)
    }

    @Test
    fun `pace below 90 percent of required is BEHIND`() {
        val today = LocalDate.of(2026, 4, 1)
        val creationMonth = YearMonth.of(2026, 1)
        // required ends up at 10,000 again; pace 5,000 is well below 0.9x of it.
        val g = goal(target = 85_000L, baseline = 0L, deadline = YearMonth.of(2026, 10).atEndOfMonth(), createdAt = createdAt(creationMonth))
        val summaries = mapOf(
            YearMonth.of(2026, 1) to summary(5_000L),
            YearMonth.of(2026, 2) to summary(5_000L),
            YearMonth.of(2026, 3) to summary(5_000L),
        )
        val progress = GetGoalProgressUseCase.compute(g, summaries, today)
        assertEquals(10_000L, progress.requiredMonthlyMinor)
        assertEquals(GoalStatus.BEHIND, progress.status)
        assertNotNull(progress.projectedDate) // BEHIND still has a (distant) projection; pace > 0
    }

    @Test
    fun `zero income month is a valid complete month`() {
        val today = LocalDate.of(2026, 3, 1)
        val creationMonth = YearMonth.of(2026, 1)
        val g = goal(target = 50_000L, baseline = 0L, deadline = YearMonth.of(2026, 12).atEndOfMonth(), createdAt = createdAt(creationMonth))
        val summaries = mapOf(
            YearMonth.of(2026, 1) to summary(0L, income = 0L),
            YearMonth.of(2026, 2) to summary(10_000L),
        )
        val progress = GetGoalProgressUseCase.compute(g, summaries, today)
        assertEquals(5_000L, progress.actualPaceMinor) // (0 + 10000) / 2
    }

    @Test
    fun `negative net saving lowers pace and can make goal infeasible`() {
        val today = LocalDate.of(2026, 3, 1)
        val creationMonth = YearMonth.of(2026, 1)
        val g = goal(target = 50_000L, baseline = 0L, deadline = YearMonth.of(2026, 12).atEndOfMonth(), createdAt = createdAt(creationMonth))
        val summaries = mapOf(
            YearMonth.of(2026, 1) to summary(-2_000L, income = 30_000L),
            YearMonth.of(2026, 2) to summary(-2_000L, income = 30_000L),
        )
        val progress = GetGoalProgressUseCase.compute(g, summaries, today)
        assertEquals(-2_000L, progress.actualPaceMinor)
        assertEquals(GoalStatus.BEHIND, progress.status)
        assertNull(progress.projectedDate)
        assertTrue(progress.isInfeasible)
        assertNull(progress.suggestedTargetMinor) // pace <= 0, no realistic target to suggest
        assertNotNull(progress.suggestedDeadline) // fallback 30% of trailing income used instead
    }

    @Test
    fun `goal already reached has zero remaining and is not infeasible`() {
        val today = LocalDate.of(2026, 3, 1)
        val creationMonth = YearMonth.of(2026, 1)
        val g = goal(target = 10_000L, baseline = 20_000L, deadline = YearMonth.of(2026, 12).atEndOfMonth(), createdAt = createdAt(creationMonth))
        val progress = GetGoalProgressUseCase.compute(g, emptyMap(), today)
        assertEquals(0L, progress.remainingMinor)
        assertEquals(0L, progress.requiredMonthlyMinor)
        assertEquals(false, progress.isInfeasible)
    }

    @Test
    fun `a reached goal at a positive pace projects today, at zero pace projects nothing`() {
        val today = LocalDate.of(2026, 3, 1)
        val creationMonth = YearMonth.of(2026, 1)
        val reached = goal(target = 10_000L, baseline = 20_000L, deadline = YearMonth.of(2026, 12).atEndOfMonth(), createdAt = createdAt(creationMonth))

        val flat = GetGoalProgressUseCase.compute(reached, emptyMap(), today)
        assertEquals(0L, flat.actualPaceMinor)
        assertNull(flat.projectedDate) // pace <= 0 never projects, even with nothing left to save (#162 step 5)

        val saving = mapOf(YearMonth.of(2026, 1) to summary(5_000L), YearMonth.of(2026, 2) to summary(5_000L))
        val positive = GetGoalProgressUseCase.compute(reached, saving, today)
        assertEquals(0L, positive.remainingMinor)
        assertEquals(today, positive.projectedDate) // today + ceil(0 / pace) months
    }

    @Test
    fun `deadline in the current month gives one month left`() {
        val today = LocalDate.of(2026, 6, 15)
        val g = goal(target = 100_000L, baseline = 0L, deadline = LocalDate.of(2026, 6, 30), createdAt = createdAt(YearMonth.of(2026, 1)))
        val progress = GetGoalProgressUseCase.compute(g, emptyMap(), today)
        assertEquals(100_000L, progress.requiredMonthlyMinor) // remaining / 1 month
    }

    @Test
    fun `deadline in the past is clamped to one month, never divides by zero`() {
        val today = LocalDate.of(2026, 6, 15)
        val g = goal(target = 100_000L, baseline = 0L, deadline = LocalDate.of(2026, 1, 1), createdAt = createdAt(YearMonth.of(2025, 11)))
        val progress = GetGoalProgressUseCase.compute(g, emptyMap(), today)
        assertEquals(100_000L, progress.requiredMonthlyMinor)
    }

    @Test
    fun `A-24 four months left reading is flagged infeasible with suggestions`() {
        // Same figures as the scenario (490,000 target, 11,200 baseline) but only 4 months to the deadline
        // (Sep-Dec 2026) instead of 12. Jun-Aug have already put 60,000 away, so 418,800 is left and the
        // required saving jumps to 104,700/mo, which blows past 0.5x a modest income; the suggestions kick in.
        val today = LocalDate.of(2026, 9, 1)
        val creationMonth = YearMonth.of(2026, 6)
        val g = goal(target = 490_000L, baseline = 11_200L, deadline = YearMonth.of(2026, 12).atEndOfMonth(), createdAt = createdAt(creationMonth))
        val summaries = mapOf(
            YearMonth.of(2026, 6) to summary(20_000L, income = 60_000L),
            YearMonth.of(2026, 7) to summary(20_000L, income = 60_000L),
            YearMonth.of(2026, 8) to summary(20_000L, income = 60_000L),
        )
        val progress = GetGoalProgressUseCase.compute(g, summaries, today)
        assertEquals(104_700L, progress.requiredMonthlyMinor)
        assertTrue(progress.isInfeasible)
        assertNotNull(progress.suggestedDeadline)
        assertNotNull(progress.suggestedTargetMinor)
        assertTrue(progress.suggestedTargetMinor!! < g.targetLkrMinor)
    }

    @Test
    fun `flow refreshes at midnight so months left moves with the calendar`() = runTest {
        val start = LocalDate.of(2026, 1, 31).atTime(23, 0).atZone(ZONE).toInstant()
        // Virtual time drives the clock, so advancing the scheduler moves "today".
        val clock = object : Clock() {
            override fun getZone(): ZoneId = ZONE
            override fun withZone(zone: ZoneId): Clock = this
            override fun instant(): Instant = start.plusMillis(testScheduler.currentTime)
        }
        val goalRepo = FakeGoalRepository()
        val useCase = GetGoalProgressUseCase(goalRepo, com.fundtrail.domain.usecase.summary.GetMonthlySummaryUseCase(FakeTransactionRepository()), clock)
        goalRepo.save(
            Goal(
                id = "g1", name = "MacBook", targetLkrMinor = 100_000L, deadline = LocalDate.of(2026, 12, 31),
                baselineSavedMinor = 0L, createdAt = YearMonth.of(2026, 1).atDay(1).atStartOfDay(ZONE).toInstant().toEpochMilli(),
            )
        )

        val required = mutableListOf<Long>()
        backgroundScope.launch { useCase().collect { required += it!!.requiredMonthlyMinor } }
        runCurrent()
        assertEquals(listOf(8_334L), required) // 31 Jan: Jan-Dec, 12 months left

        advanceTimeBy(2 * 60 * 60 * 1000L) // past midnight into February
        runCurrent()
        assertEquals(listOf(8_334L, 9_091L), required) // 1 Feb: 11 months left, nothing else changed
    }

    @Test
    fun `flow recomputes when a transaction is added`() = runTest {
        val fixedToday = LocalDate.of(2026, 2, 1)
        val clock = Clock.fixed(fixedToday.atStartOfDay(ZONE).toInstant(), ZONE)
        val goalRepo = FakeGoalRepository()
        val txRepo = FakeTransactionRepository()
        val useCase = GetGoalProgressUseCase(goalRepo, com.fundtrail.domain.usecase.summary.GetMonthlySummaryUseCase(txRepo), clock)

        goalRepo.save(
            Goal(
                id = "g1", name = "MacBook", targetLkrMinor = 100_000L, deadline = LocalDate.of(2026, 12, 31),
                baselineSavedMinor = 0L, createdAt = YearMonth.of(2026, 1).atDay(1).atStartOfDay(ZONE).toInstant().toEpochMilli(),
            )
        )

        val before = useCase().first()
        assertNotNull(before)

        txRepo.add(
            Transaction(
                id = "tx1", type = TransactionType.INCOME, amountLkrMinor = 10_000L, accountId = "acc_main_bank",
                incomeSource = com.fundtrail.domain.model.IncomeSource.SALARY,
                occurredAt = YearMonth.of(2026, 1).atDay(15).atStartOfDay(ZONE).toInstant().toEpochMilli(),
            )
        )

        val after = useCase().first()
        assertTrue(after!!.savedToDateMinor > before!!.savedToDateMinor)
    }
}
