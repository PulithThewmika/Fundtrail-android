package com.fundtrail.ui.recurring

import com.fundtrail.R
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.usecase.account.ObserveActiveAccountsUseCase
import com.fundtrail.domain.usecase.category.ObserveActiveCategoriesUseCase
import com.fundtrail.domain.usecase.recurring.AddRecurringUseCase
import com.fundtrail.domain.usecase.recurring.DeleteRecurringUseCase
import com.fundtrail.domain.usecase.recurring.ObserveRecurringUseCase
import com.fundtrail.domain.usecase.recurring.PauseRecurringUseCase
import com.fundtrail.domain.usecase.recurring.UpdateRecurringUseCase
import com.fundtrail.fakes.FakeAccountRepository
import com.fundtrail.fakes.FakeCategoryRepository
import com.fundtrail.fakes.FakeRecurringRepository
import com.fundtrail.fakes.MainDispatcherRule
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecurringViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Colombo")
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 15).atStartOfDay(zone).toInstant(), zone)
    private val accounts = FakeAccountRepository().also { runBlocking { it.seedDefaultsIfEmpty() } }
    private val categories = FakeCategoryRepository()
    private val recurring = FakeRecurringRepository().apply { setTemplates(emptyList()) }

    private fun template(id: String, amount: Long, paused: Boolean = false, day: Int = 1) =
        RecurringTemplate(
            id = id, name = id, categoryId = "cat_rent", accountId = "default-main-bank",
            expectedAmountMinor = amount, dayOfMonth = day, nextDueDate = LocalDate.of(2026, 10, day), isPaused = paused,
        )

    private suspend fun stored() = recurring.observeAll().first()

    private fun TestScope.viewModel() = RecurringViewModel(
        ObserveRecurringUseCase(recurring),
        ObserveActiveCategoriesUseCase(categories),
        ObserveActiveAccountsUseCase(accounts),
        AddRecurringUseCase(recurring, clock),
        UpdateRecurringUseCase(recurring),
        PauseRecurringUseCase(recurring),
        DeleteRecurringUseCase(recurring),
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} } }

    @Test
    fun monthlyTotalExcludesPausedTemplates() = runTest {
        recurring.setTemplates(
            listOf(
                template("rent", 3_400_000L),
                template("gym", 950_000L),
                template("cloud", 120_000L, paused = true),
            ),
        )
        val vm = viewModel()
        // 34,000 + 9,500, the paused 1,200 excluded.
        assertEquals(4_350_000L, vm.uiState.value.monthlyTotalMinor)
        // The paused row still shows, badged.
        assertTrue(vm.uiState.value.rows.first { it.id == "cloud" }.isPaused)
    }

    @Test
    fun dayOfMonthValidationRejects29To31() = runTest {
        val vm = viewModel()
        vm.onAddClick()
        vm.onNameChange("Rent")
        vm.onAmountChange("34000")
        vm.onDayOfMonthChange("31")

        assertFalse(vm.uiState.value.form!!.canSave)

        vm.onSave()
        // Save was refused: nothing was added and the form is still open with an error.
        assertTrue(stored().isEmpty())
        assertTrue(vm.uiState.value.form != null)

        vm.onDayOfMonthChange("28")
        assertTrue(vm.uiState.value.form!!.canSave)
        vm.onSave()
        assertEquals(1, stored().size)
        assertEquals(28, stored().single().dayOfMonth)
        assertNull(vm.uiState.value.form)
    }

    @Test
    fun pauseAndResumeFlipTheFlag() = runTest {
        recurring.setTemplates(listOf(template("gym", 950_000L)))
        val vm = viewModel()

        vm.onPauseToggle("gym")
        assertTrue(stored().first { it.id == "gym" }.isPaused)

        vm.onPauseToggle("gym")
        assertFalse(stored().first { it.id == "gym" }.isPaused)
    }

    @Test
    fun theRowShowsTheNextDueDateNotJustTheDay() = runTest {
        // Caught up to November: the row must say November, not "day 1".
        recurring.setTemplates(
            listOf(template("rent", 3_400_000L).copy(nextDueDate = LocalDate.of(2026, 11, 1))),
        )
        val vm = viewModel()

        assertEquals(LocalDate.of(2026, 11, 1), vm.uiState.value.rows.single().nextDueDate)
    }

    @Test
    fun onlyLkrAccountsCanCarryARecurringCost() = runTest {
        val vm = viewModel()

        // A template holds one LKR figure and no rate, so a foreign account must not be offered (ADR-0003).
        assertTrue(vm.uiState.value.accounts.isNotEmpty())
        assertTrue(vm.uiState.value.accounts.all { it.currency == "LKR" })
    }

    @Test
    fun aRepeatedSaveDoesNotCreateADuplicate() = runTest {
        val vm = viewModel()
        vm.onAddClick()
        vm.onNameChange("Rent")
        vm.onAmountChange("34000")
        vm.onDayOfMonthChange("1")

        vm.onSave()
        vm.onSave()

        // One template, not two under different ids.
        assertEquals(1, stored().size)
    }

    @Test
    fun editingATemplateThatDisappearedFailsWithAMessage() = runTest {
        recurring.setTemplates(listOf(template("gym", 950_000L)))
        val vm = viewModel()
        vm.onEditClick("gym")
        vm.onAmountChange("12000")

        // It is deleted on another device while the form is open.
        recurring.setTemplates(emptyList())
        vm.onSave()

        val form = vm.uiState.value.form
        assertTrue(form != null)
        assertFalse(form!!.isSaving)
        assertEquals(R.string.recurring_gone, form.error)
    }

    @Test
    fun aFailedDeleteIsReported() = runTest {
        recurring.setTemplates(listOf(template("gym", 950_000L)))
        val vm = viewModel()
        recurring.writeError = IllegalStateException("offline")

        vm.onDelete("gym")

        assertEquals(1, stored().size)
        assertTrue(vm.uiState.value.actionError != null)

        vm.onActionErrorShown()
        assertNull(vm.uiState.value.actionError)
    }
}
