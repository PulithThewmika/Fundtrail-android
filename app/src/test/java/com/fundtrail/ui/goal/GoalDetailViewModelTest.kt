package com.fundtrail.ui.goal

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.GoalRepository
import com.fundtrail.domain.usecase.goal.GetGoalProgressUseCase
import com.fundtrail.domain.usecase.goal.ObserveActiveGoalUseCase
import com.fundtrail.domain.usecase.goal.SaveGoalUseCase
import com.fundtrail.domain.usecase.summary.GetMonthlySummaryUseCase
import com.fundtrail.fakes.FakeGoalRepository
import com.fundtrail.fakes.FakeTransactionRepository
import com.fundtrail.fakes.MainDispatcherRule
import com.fundtrail.ui.common.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

/** #168: the what-if slider computed in the ViewModel, and the status it does not overwrite. */
class GoalDetailViewModelTest {

    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 7)
    private val clock = Clock.fixed(today.atStartOfDay(APP_ZONE).toInstant(), APP_ZONE)
    private val goals = FakeGoalRepository { today }
    private val transactions = FakeTransactionRepository()

    private fun TestScope.viewModel() = GoalDetailViewModel(
        observeGoal = ObserveActiveGoalUseCase(goals),
        getProgress = GetGoalProgressUseCase(goals, GetMonthlySummaryUseCase(transactions), clock),
        saveGoal = SaveGoalUseCase(goals),
        clock = clock,
    ).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    private fun GoalDetailViewModel.data() = (uiState.value.content as UiState.Success).data

    @Test
    fun `loads the active goal with its progress`() = runTest {
        val vm = viewModel()

        val data = vm.data()
        assertEquals("MacBook Pro M4", data.goal.name)
        assertEquals(49_000_000L, data.goal.targetLkrMinor)
        assertTrue("required monthly should be positive", data.progress.requiredMonthlyMinor > 0L)
        assertTrue(data.percent in 0..100)
    }

    @Test
    fun `an empty state is reported when there is no active goal`() = runTest {
        val noGoal = object : GoalRepository {
            override fun observeActive(): Flow<Goal?> = flowOf(null)
            override suspend fun save(goal: Goal) = Result.Success(Unit)
            override suspend fun archive(id: String) = Result.Success(Unit)
        }
        val vm = GoalDetailViewModel(
            observeGoal = ObserveActiveGoalUseCase(noGoal),
            getProgress = GetGoalProgressUseCase(noGoal, GetMonthlySummaryUseCase(transactions), clock),
            saveGoal = SaveGoalUseCase(noGoal),
            clock = clock,
        ).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        }

        assertEquals(UiState.Empty, vm.uiState.value.content)
    }

    @Test
    fun `the what-if amount drives the projected date, and reset restores the goal's own`() = runTest {
        val vm = viewModel()
        val ownProjection = vm.data().progress.projectedDate

        vm.onWhatIfChange(10_000_000L) // LKR 100,000 a month clears it far sooner
        val fast = vm.data()
        assertEquals(10_000_000L, fast.whatIfMinor)
        assertNotNull(fast.whatIfDate)

        vm.onWhatIfChange(1_000_000L) // LKR 10,000 a month takes much longer
        val slow = vm.data()
        assertTrue(slow.whatIfDate!!.isAfter(fast.whatIfDate))

        vm.resetWhatIf()
        assertEquals(ownProjection, vm.data().whatIfDate)
    }

    @Test
    fun `a zero what-if amount projects nothing, even when the goal itself has a projection`() = runTest {
        val vm = infeasibleGoalViewModel() // Jul-Sep saving gives a positive pace
        assertNotNull("fixture needs its own projection to make this meaningful", vm.data().progress.projectedDate)

        vm.onWhatIfChange(0L)

        assertEquals(0L, vm.data().whatIfMinor)
        // An explicit 0 must not fall back to the goal's own date.
        assertNull(vm.data().whatIfDate)
    }

    @Test
    fun `the slider maximum always covers the amount it is showing`() = runTest {
        val vm = viewModel()

        vm.onWhatIfChange(99_000_000L)

        val data = vm.data()
        assertTrue("max must cover the value", data.whatIfMaxMinor >= data.whatIfMinor)
    }

    @Test
    fun `moving the slider leaves the goal's own status alone`() = runTest {
        val vm = viewModel()
        val realStatus = vm.data().progress.status

        vm.onWhatIfChange(99_000_000L)

        // The chip reads progress.status, so an exploratory slider drag must not rewrite it.
        assertEquals(realStatus, vm.data().progress.status)
    }

    @Test
    fun `an ahead what-if reports the lead, a behind one reports the shortfall`() = runTest {
        val vm = viewModel()
        val required = vm.data().progress.requiredMonthlyMinor

        vm.onWhatIfChange(required * 2)
        assertEquals(GoalDetailStatus.AHEAD, vm.data().whatIfStatus)
        assertEquals(required, vm.data().statusGapMinor) // 2x required leads by exactly required

        vm.onWhatIfChange(required / 2)
        assertEquals(GoalDetailStatus.BEHIND, vm.data().whatIfStatus)
        assertEquals(required - required / 2, vm.data().statusGapMinor)
    }

    @Test
    fun `a second adjustment builds on the first, not on the pre-save goal`() = runTest {
        val vm = infeasibleGoalViewModel()
        val progress = vm.data().progress
        assertTrue("fixture should be infeasible", progress.isInfeasible)
        assertNotNull(progress.suggestedTargetMinor)
        assertNotNull(progress.suggestedDeadline)

        vm.lowerTarget()
        vm.moveDeadline()

        // Both adjustments must survive: the deadline save must not carry the old target back.
        // The deadline itself is recomputed against the lowered target, so only that it moved
        // off the original is asserted, not the first suggestion's value.
        val saved = goals.observeActive().first()!!
        assertEquals(progress.suggestedTargetMinor, saved.targetLkrMinor)
        assertTrue("the deadline adjustment should have applied too", saved.deadline != LocalDate.of(2026, 11, 30))
    }

    @Test
    fun `a failed adjustment reports an error that is not cleared on sight`() = runTest {
        val failing = object : GoalRepository {
            override fun observeActive(): Flow<Goal?> = goals.observeActive()
            override suspend fun save(goal: Goal): Result<Unit> = Result.Error(IllegalStateException("offline"))
            override suspend fun archive(id: String): Result<Unit> = Result.Success(Unit)
        }
        val vm = GoalDetailViewModel(
            observeGoal = ObserveActiveGoalUseCase(failing),
            getProgress = GetGoalProgressUseCase(failing, GetMonthlySummaryUseCase(transactions), clock),
            saveGoal = SaveGoalUseCase(failing),
            clock = clock,
        ).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        }

        vm.moveDeadline()
        assertNotNull("a failed adjustment must say why", vm.uiState.value.actionError)

        // It has to outlive the next state emission, or the user never reads it.
        vm.onWhatIfChange(1_000_000L)
        assertNotNull(vm.uiState.value.actionError)

        // The next attempt clears it.
        vm.lowerTarget()
        assertNotNull(vm.uiState.value.actionError) // the retry fails too, so a reason is still shown
    }

    /** Jul-Sep earn 60,000 a month, so a November deadline needs far more than half of it (ADR-0006). */
    private suspend fun TestScope.infeasibleGoalViewModel(): GoalDetailViewModel {
        listOf(YearMonth.of(2026, 7), YearMonth.of(2026, 8), YearMonth.of(2026, 9)).forEachIndexed { i, month ->
            transactions.add(
                Transaction(
                    id = "inc_$i",
                    type = TransactionType.INCOME,
                    status = TransactionStatus.ACTUAL,
                    amountLkrMinor = 6_000_000L, // LKR 60,000 a month
                    accountId = "acc_main_bank",
                    incomeSource = IncomeSource.SALARY,
                    occurredAt = month.atDay(25).atStartOfDay(APP_ZONE).toInstant().toEpochMilli(),
                )
            )
        }
        goals.save(
            Goal(
                id = "goal_tight",
                name = "MacBook Pro M4",
                targetLkrMinor = 49_000_000L,
                deadline = LocalDate.of(2026, 11, 30),
                baselineSavedMinor = 1_120_000L,
                createdAt = YearMonth.of(2026, 7).atDay(1).atStartOfDay(APP_ZONE).toInstant().toEpochMilli(),
            )
        )
        return viewModel()
    }
}
