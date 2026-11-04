package com.fundtrail.ui.goal

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.usecase.goal.GetGoalProgressUseCase
import com.fundtrail.domain.usecase.goal.SaveGoalUseCase
import com.fundtrail.fakes.FakeGoalRepository
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
import java.time.YearMonth

/** #167: the live required-monthly readout, and validation in [APP_ZONE]. */
class GoalSetupViewModelTest {

    @get:Rule val mainDispatcher = MainDispatcherRule()

    // 07/10/2026, the date the S3 wireframe uses.
    private val today = LocalDate.of(2026, 10, 7)
    private val clock = Clock.fixed(today.atStartOfDay(APP_ZONE).toInstant(), APP_ZONE)
    private val goals = FakeGoalRepository { today }

    private fun viewModel() = GoalSetupViewModel(SaveGoalUseCase(goals), clock)

    @Test
    fun `starts blank, twelve months out, with no required figure`() {
        val state = viewModel().uiState.value

        assertEquals("", state.name)
        assertEquals("", state.target)
        assertEquals("", state.alreadySaved)
        assertEquals(0L, state.requiredMonthlyMinor)
        assertEquals(today.plusMonths(12), state.deadline)
    }

    @Test
    fun `required monthly updates as the user types and gives the scenario figure`() {
        val vm = viewModel()

        vm.onTargetChange("490000")
        vm.onAlreadySavedChange("11200")
        vm.onDeadlineChange(YearMonth.of(2027, 9).atDay(30))

        // Oct 2026 through Sep 2027 inclusive is 12 months: 478,800 / 12 = 39,900.
        assertEquals(3_990_000L, vm.uiState.value.requiredMonthlyMinor)
    }

    @Test
    fun `the preview counts the deadline month the same way the saved goal does`() = runTest {
        val vm = viewModel()
        vm.onNameChange("MacBook Pro M4")
        vm.onTargetChange("490000")
        vm.onAlreadySavedChange("11200")
        vm.onDeadlineChange(YearMonth.of(2027, 9).atDay(30))
        val preview = vm.uiState.value.requiredMonthlyMinor

        vm.save()
        assertTrue(vm.uiState.value.saved)

        // The figure must not change just because the user pressed Save, so the form has to use
        // the same inclusive month count as the domain (BR-06).
        val saved = goals.observeActive().first()
        assertNotNull(saved)
        val progress = GetGoalProgressUseCase.compute(saved!!, emptyMap(), today)
        assertEquals(preview, progress.requiredMonthlyMinor)
    }

    @Test
    fun `a deadline of today is rejected, in APP_ZONE`() {
        val vm = viewModel()
        vm.onNameChange("MacBook Pro M4")
        vm.onTargetChange("490000")
        vm.onAlreadySavedChange("11200")

        vm.onDeadlineChange(today)
        vm.save()

        assertNotNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.saved)
    }

    @Test
    fun `a blank amount is reported, not saved`() {
        val vm = viewModel()
        vm.onNameChange("MacBook Pro M4")

        vm.save()

        assertNotNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.saved)
    }

    @Test
    fun `typing an amount clears the error the blank one produced`() {
        val vm = viewModel()
        vm.onNameChange("MacBook Pro M4")
        vm.save()
        assertNotNull(vm.uiState.value.errorMessage)

        vm.onTargetChange("490000")

        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `a valid goal saves, trimmed, and reports saved`() = runTest {
        val vm = viewModel()
        vm.onNameChange("  MacBook Pro M4  ")
        vm.onTargetChange("490000")
        vm.onAlreadySavedChange("11200")

        vm.save()

        assertTrue(vm.uiState.value.saved)
        assertNull(vm.uiState.value.errorMessage)
        val saved = goals.observeActive().first()
        assertEquals("MacBook Pro M4", saved?.name)
        assertEquals(49_000_000L, saved?.targetLkrMinor)
        assertEquals(1_120_000L, saved?.baselineSavedMinor)
    }
}
