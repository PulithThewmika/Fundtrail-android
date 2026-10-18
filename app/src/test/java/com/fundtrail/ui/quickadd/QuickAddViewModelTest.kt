package com.fundtrail.ui.quickadd

import com.fundtrail.R
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.DefaultCategories
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.usecase.account.ObserveActiveAccountsUseCase
import com.fundtrail.domain.usecase.category.ObserveSuggestedCategoriesUseCase
import com.fundtrail.domain.usecase.expense.AddExpenseUseCase
import com.fundtrail.domain.usecase.expense.DeleteTransactionUseCase
import com.fundtrail.domain.usecase.expense.ObserveLastUsedAccountUseCase
import com.fundtrail.fakes.FakeAccountRepository
import com.fundtrail.fakes.FakeCategoryRepository
import com.fundtrail.fakes.FakeTransactionRepository
import com.fundtrail.fakes.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class QuickAddViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    // The fake starts empty (accounts come from seeding), so seed the five defaults like a signed-in user has.
    private val accounts = FakeAccountRepository().also { repo -> runBlocking { repo.seedDefaultsIfEmpty() } }
    // The fake starts with a few sample categories; add the real defaults (ids already present are skipped by add()).
    private val categories = FakeCategoryRepository().also { repo ->
        runBlocking { DefaultCategories.all.forEach { repo.add(it) } }
    }
    private val transactions = FakeTransactionRepository()

    private fun TestScope.viewModel() = QuickAddViewModel(
        ObserveActiveAccountsUseCase(accounts),
        ObserveLastUsedAccountUseCase(transactions),
        ObserveSuggestedCategoriesUseCase(categories),
        AddExpenseUseCase(transactions, categories),
        DeleteTransactionUseCase(transactions),
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} } }

    private suspend fun savedExpenses() = transactions.observeMonth(YearMonth.now()).first()

    @Test
    fun preselectsTheSuggestedCategoryAndAnAccount() = runTest {
        val vm = viewModel()

        val state = vm.uiState.value
        assertNotNull(state.selectedCategoryId)
        assertEquals(state.categories.first().id, state.selectedCategoryId)
        assertTrue(state.categorySuggested)
        assertNotNull(state.selectedAccountId)
        assertEquals(LocalDate.now(APP_ZONE), state.date)
    }

    @Test
    fun theThreeTapPathSavesWithNothingButAnAmount() = runTest {
        val vm = viewModel()
        val before = savedExpenses().size

        vm.onAmountChange("1250")
        assertTrue(vm.uiState.value.canSave)
        vm.onSave()

        val after = savedExpenses()
        assertEquals(before + 1, after.size)
        val saved = after.first { it.id == vm.uiState.value.savedTransactionId }
        assertEquals(125_000L, saved.amountLkrMinor)
        assertEquals(vm.uiState.value.selectedCategoryId, saved.categoryId)
    }

    @Test
    fun saveIsDisabledWithoutAnAmount() = runTest {
        val vm = viewModel()

        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun aZeroAmountShowsAnInlineErrorAndSavesNothing() = runTest {
        val vm = viewModel()
        val before = savedExpenses().size

        vm.onAmountChange("0")
        vm.onSave()

        assertEquals(R.string.quick_add_amount_required, vm.uiState.value.amountError)
        assertEquals(before, savedExpenses().size)
        assertNull(vm.uiState.value.savedTransactionId)
    }

    @Test
    fun typingClearsTheAmountError() = runTest {
        val vm = viewModel()
        vm.onAmountChange("0")
        vm.onSave()

        vm.onAmountChange("5")

        assertNull(vm.uiState.value.amountError)
    }

    @Test
    fun theSavedExpenseTakesTheTypeOfTheChosenCategory() = runTest {
        val vm = viewModel()

        vm.onCategorySelected("cat_rent")
        assertEquals(SpendKind.COMMITTED, vm.uiState.value.spendKind)
        vm.onAmountChange("100")
        vm.onSave()
        val rent = savedExpenses().first { it.id == vm.uiState.value.savedTransactionId }
        assertEquals(SpendKind.COMMITTED, rent.spendKind)

        vm.onSavedHandled()
        vm.onCategorySelected("cat_shopping")
        vm.onAmountChange("100")
        vm.onSave()
        val shopping = savedExpenses().first { it.id == vm.uiState.value.savedTransactionId }
        assertEquals(SpendKind.DISCRETIONARY, shopping.spendKind)
    }

    @Test
    fun overridingTheTypeAppliesToThatExpenseAndPickingTheCategoryTypeClearsIt() = runTest {
        val vm = viewModel()
        vm.onCategorySelected("cat_rent")

        vm.onSpendKindSelected(SpendKind.DISCRETIONARY)
        assertTrue(vm.uiState.value.spendKindOverridden)
        assertEquals(SpendKind.DISCRETIONARY, vm.uiState.value.spendKind)

        vm.onSpendKindSelected(SpendKind.COMMITTED)
        assertFalse(vm.uiState.value.spendKindOverridden)

        vm.onSpendKindSelected(SpendKind.DISCRETIONARY)
        vm.onAmountChange("100")
        vm.onSave()
        val saved = savedExpenses().first { it.id == vm.uiState.value.savedTransactionId }
        assertEquals(SpendKind.DISCRETIONARY, saved.spendKind)
    }

    @Test
    fun choosingAnotherCategoryDropsAnEarlierTypeOverride() = runTest {
        val vm = viewModel()
        vm.onCategorySelected("cat_rent")
        vm.onSpendKindSelected(SpendKind.DISCRETIONARY)

        vm.onCategorySelected("cat_gym")

        assertFalse(vm.uiState.value.spendKindOverridden)
        assertEquals(SpendKind.COMMITTED, vm.uiState.value.spendKind)
    }

    @Test
    fun defaultsToTheAccountOfTheLatestExpense() = runTest {
        val vm = viewModel()
        vm.onAccountSelected("default-cash")
        vm.onAmountChange("100")
        vm.onSave()
        vm.onSavedHandled()
        vm.reset()

        // The explicit pick was reset, yet the new expense makes the cash account the default again.
        assertEquals("default-cash", vm.uiState.value.selectedAccountId)
    }

    @Test
    fun backDatedExpensesLandOnThePickedDay() = runTest {
        val vm = viewModel()
        val day = LocalDate.now(APP_ZONE).minusDays(1)

        vm.onDateChange(day)
        vm.onAmountChange("100")
        vm.onSave()

        val month = YearMonth.from(day)
        val saved = transactions.observeMonth(month).first().first { it.id == vm.uiState.value.savedTransactionId }
        val savedDay = Instant.ofEpochMilli(saved.occurredAt).atZone(APP_ZONE).toLocalDate()
        assertEquals(day, savedDay)
    }

    @Test
    fun resetClearsTheDraftSoTheNextOpenIsFresh() = runTest {
        val vm = viewModel()
        vm.onAmountChange("999")
        vm.onNoteChange("lunch")
        vm.onCategorySelected("cat_rent")

        vm.reset()

        val state = vm.uiState.value
        assertEquals("", state.amountText)
        assertEquals("", state.note)
        assertTrue(state.categorySuggested)
    }

    @Test
    fun undoRemovesTheExpenseAgain() = runTest {
        val vm = viewModel()
        vm.onAmountChange("100")
        vm.onSave()
        val id = vm.uiState.value.savedTransactionId!!
        assertTrue(savedExpenses().any { it.id == id })

        assertNull(vm.undo(id))

        assertFalse(savedExpenses().any { it.id == id })
    }

    @Test
    fun aFailedUndoReportsAnErrorInsteadOfFailingSilently() = runTest {
        val vm = viewModel()

        // An id the repository does not hold makes the delete fail, like a rejected write would.
        assertEquals(R.string.quick_add_undo_failed, vm.undo("no-such-expense"))
    }

    @Test
    fun anArchivedCategoryLeavesTheChipsAndTheSelectionFallsBack() = runTest {
        val vm = viewModel()
        vm.onCategorySelected("cat_rent")

        categories.archive("cat_rent")

        val state = vm.uiState.value
        assertTrue(state.categories.none { it.id == "cat_rent" })
        assertTrue(state.selectedCategoryId != "cat_rent")
        assertTrue(state.categorySuggested)
    }
}
