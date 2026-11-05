package com.fundtrail.ui.onboarding

import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.usecase.account.AddAccountUseCase
import com.fundtrail.domain.usecase.goal.SaveGoalUseCase
import com.fundtrail.domain.usecase.settings.UpdateIncomeEstimateUseCase
import com.fundtrail.fakes.FakeAccountRepository
import com.fundtrail.fakes.FakeGoalRepository
import com.fundtrail.fakes.FakeSettingsRepository
import com.fundtrail.fakes.MainDispatcherRule
import kotlinx.coroutines.flow.first
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
import java.time.ZoneOffset

/** FR-08: three skippable steps, every path reaching the dashboard (#323). */
class OnboardingViewModelTest {

    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val clock = Clock.fixed(LocalDate.of(2026, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val goals = FakeGoalRepository { LocalDate.now(clock) }
    private val accounts = FakeAccountRepository()
    private val settings = FakeSettingsRepository()

    private fun viewModel() = OnboardingViewModel(
        SaveGoalUseCase(goals),
        AddAccountUseCase(accounts),
        UpdateIncomeEstimateUseCase(settings),
        clock,
    )

    @Test
    fun `starts on the goal step`() {
        assertEquals(OnboardingStep.GOAL, viewModel().uiState.value.step)
    }

    @Test
    fun `skipping every step still reaches the dashboard`() {
        val vm = viewModel()
        vm.onGoalSkip()
        assertEquals(OnboardingStep.ACCOUNT, vm.uiState.value.step)
        vm.onAccountSkip()
        assertEquals(OnboardingStep.INCOME, vm.uiState.value.step)
        vm.onIncomeSkip()
        assertTrue(vm.uiState.value.finished)
    }

    @Test
    fun `a blank goal name is reported, not saved, and does not advance`() = runTest {
        val vm = viewModel()
        vm.onGoalTargetChange("490000")
        vm.onGoalSavedChange("11200")

        vm.onGoalNext()

        assertNotNull(vm.uiState.value.goalError)
        assertEquals(OnboardingStep.GOAL, vm.uiState.value.step)
    }

    @Test
    fun `a valid goal saves and advances to the account step`() = runTest {
        val vm = viewModel()
        vm.onGoalNameChange("MacBook Pro M4")
        vm.onGoalTargetChange("490000")
        vm.onGoalSavedChange("11200")

        vm.onGoalNext()

        assertEquals(OnboardingStep.ACCOUNT, vm.uiState.value.step)
        assertNull(vm.uiState.value.goalError)
        val saved = goals.observeActive().first()
        assertEquals("MacBook Pro M4", saved?.name)
        assertEquals(49_000_000L, saved?.targetLkrMinor)
    }

    @Test
    fun `a blank account name is reported, not saved, and does not advance`() {
        val vm = viewModel()
        vm.onGoalSkip()

        vm.onAccountNext()

        assertNotNull(vm.uiState.value.accountError)
        assertEquals(OnboardingStep.ACCOUNT, vm.uiState.value.step)
    }

    @Test
    fun `a named account saves and advances to the income step`() = runTest {
        val vm = viewModel()
        vm.onGoalSkip()
        vm.onAccountNameChange("Cash Wallet")
        vm.onAccountKindChange(AccountKind.CASH)

        vm.onAccountNext()

        assertEquals(OnboardingStep.INCOME, vm.uiState.value.step)
        val saved = accounts.observeAll().first()
        assertEquals(1, saved.size)
        assertEquals("Cash Wallet", saved.first().name)
        assertEquals(AccountKind.CASH, saved.first().kind)
    }

    @Test
    fun `an income range with min greater than max is reported and does not finish`() {
        val vm = viewModel()
        vm.onGoalSkip()
        vm.onAccountSkip()
        vm.onIncomeLowChange("200000")
        vm.onIncomeHighChange("100000")

        vm.onIncomeNext()

        assertNotNull(vm.uiState.value.incomeError)
        assertFalse(vm.uiState.value.finished)
    }

    @Test
    fun `leaving the income estimate blank finishes without writing anything`() = runTest {
        val before = settings.observe().first()
        val vm = viewModel()
        vm.onGoalSkip()
        vm.onAccountSkip()

        vm.onIncomeNext()

        assertTrue(vm.uiState.value.finished)
        assertEquals(before, settings.observe().first())
    }

    @Test
    fun `a valid income estimate saves and finishes`() = runTest {
        val vm = viewModel()
        vm.onGoalSkip()
        vm.onAccountSkip()
        vm.onIncomeLowChange("160000")
        vm.onIncomeHighChange("210000")

        vm.onIncomeNext()

        assertTrue(vm.uiState.value.finished)
        val finalSettings = settings.observe().first()
        assertEquals(16_000_000L, finalSettings.incomeEstimateMinMinor)
        assertEquals(21_000_000L, finalSettings.incomeEstimateMaxMinor)
    }
}
