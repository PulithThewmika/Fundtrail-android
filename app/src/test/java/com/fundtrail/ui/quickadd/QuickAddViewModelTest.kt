package com.fundtrail.ui.quickadd

import com.fundtrail.R
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.DefaultCategories
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.repository.GoalRepository
import com.fundtrail.domain.usecase.account.ObserveAccountsUseCase
import com.fundtrail.domain.usecase.account.ObserveActiveAccountsUseCase
import com.fundtrail.domain.usecase.category.ObserveCategoriesUseCase
import com.fundtrail.domain.usecase.category.ObserveSuggestedCategoriesUseCase
import com.fundtrail.domain.usecase.expense.AddExpenseUseCase
import com.fundtrail.domain.usecase.expense.DeleteTransactionUseCase
import com.fundtrail.domain.usecase.expense.ObserveLastUsedAccountUseCase
import com.fundtrail.domain.usecase.goal.GetGoalProgressUseCase
import com.fundtrail.domain.usecase.goal.GoalCostUseCase
import com.fundtrail.domain.usecase.goal.ObserveActiveGoalUseCase
import com.fundtrail.domain.usecase.income.AddIncomeUseCase
import com.fundtrail.domain.usecase.summary.GetMonthlySummaryUseCase
import com.fundtrail.domain.usecase.transaction.ObserveTransactionUseCase
import com.fundtrail.domain.usecase.transaction.UpdateTransactionUseCase
import com.fundtrail.domain.usecase.transfer.AddTransferUseCase
import com.fundtrail.fakes.FakeAccountRepository
import com.fundtrail.fakes.FakeCategoryRepository
import com.fundtrail.fakes.FakeTransactionRepository
import com.fundtrail.fakes.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

class QuickAddViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    // The fake starts empty (accounts come from seeding), so seed the five defaults like a signed-in user has.
    private val accounts = FakeAccountRepository().also { repo -> runBlocking { repo.seedDefaultsIfEmpty() } }
    // The fake starts with a few sample categories; add the real defaults (ids already present are skipped by add()).
    private val categories = FakeCategoryRepository().also { repo ->
        runBlocking { DefaultCategories.all.forEach { repo.add(it) } }
    }
    private val transactions = FakeTransactionRepository()

    private class InMemoryGoals : GoalRepository {
        val active = MutableStateFlow<Goal?>(null)
        override fun observeActive(): Flow<Goal?> = active
        override suspend fun save(goal: Goal): Result<Unit> = Result.Success(Unit)
        override suspend fun archive(id: String): Result<Unit> = Result.Success(Unit)
    }

    private val goals = InMemoryGoals()

    // One date for the goal setup and the use case: two separate now() calls could land either side of a month
    // boundary mid-test, pointing the summary at a different month than the deadline. Floored to midnight so it
    // stays inside this month whatever time the suite runs at.
    private val today = LocalDate.now(APP_ZONE)
    private val goalClock = Clock.fixed(today.atStartOfDay(APP_ZONE).toInstant(), APP_ZONE)

    /**
     * A goal due this month with [remainingMinor] still to save. The sample transactions already count toward
     * this month's saving, so the target is offset by it, which makes requiredMonthly exactly [remainingMinor].
     */
    private suspend fun setGoalNeeding(remainingMinor: Long) {
        val saved = GetMonthlySummaryUseCase(transactions)(YearMonth.from(today)).first().netSavedMinor
        goals.active.value = Goal(
            id = "g", name = "MacBook", targetLkrMinor = saved + remainingMinor,
            deadline = today, baselineSavedMinor = 0L,
        )
    }

    private fun TestScope.viewModel() = QuickAddViewModel(
        ObserveActiveAccountsUseCase(accounts),
        ObserveAccountsUseCase(accounts),
        ObserveLastUsedAccountUseCase(transactions),
        ObserveSuggestedCategoriesUseCase(categories),
        ObserveCategoriesUseCase(categories),
        AddExpenseUseCase(transactions, categories),
        AddIncomeUseCase(transactions),
        AddTransferUseCase(transactions),
        DeleteTransactionUseCase(transactions),
        ObserveTransactionUseCase(transactions),
        UpdateTransactionUseCase(transactions, categories),
        ObserveActiveGoalUseCase(goals),
        GetGoalProgressUseCase(goals, GetMonthlySummaryUseCase(transactions), goalClock),
        GoalCostUseCase(),
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} } }

    private suspend fun savedTransactions() = transactions.observeMonth(YearMonth.now()).first()

    @Test
    fun preselectsTheSuggestedCategoryAndAnAccount() = runTest {
        val vm = viewModel()

        val state = vm.uiState.value
        assertEquals(TransactionType.EXPENSE, state.transactionType)
        assertNotNull(state.selectedCategoryId)
        assertEquals(state.categories.first().id, state.selectedCategoryId)
        assertTrue(state.categorySuggested)
        assertNotNull(state.selectedAccountId)
        assertEquals(LocalDate.now(APP_ZONE), state.date)
    }

    @Test
    fun keypadButtonsTypeAndModifyAmount() = runTest {
        val vm = viewModel()

        vm.onKeypadDigit("1")
        vm.onKeypadDigit("0")
        vm.onKeypadDigit("0")
        vm.onKeypadDot()
        vm.onKeypadDigit("5")
        assertEquals("100.5", vm.uiState.value.amountText)

        vm.onKeypadBackspace()
        assertEquals("100.", vm.uiState.value.amountText)
    }

    @Test
    fun keypadButtonsLimitDecimalsToTwoAndConvertToMinorUnits() = runTest {
        val vm = viewModel()

        vm.onKeypadDigit("1")
        vm.onKeypadDigit("2")
        vm.onKeypadDot()
        vm.onKeypadDigit("3")
        vm.onKeypadDigit("4")
        vm.onKeypadDigit("5")

        assertEquals("12.34", vm.uiState.value.amountText)
        assertEquals(1234L, AmountInput.toMinor(vm.uiState.value.amountText))
    }

    @Test
    fun keypadButtonsDropLeadingZerosAndConvertToMinorUnits() = runTest {
        val vm = viewModel()

        vm.onKeypadDigit("00")
        assertEquals("0", vm.uiState.value.amountText)
        vm.onKeypadDigit("5")

        assertEquals("5", vm.uiState.value.amountText)
        assertEquals(500L, AmountInput.toMinor(vm.uiState.value.amountText))
    }

    @Test
    fun theThreeTapPathSavesWithNothingButAnAmount() = runTest {
        val vm = viewModel()
        val before = savedTransactions().size

        vm.onAmountChange("1250")
        assertTrue(vm.uiState.value.canSave)
        vm.onSave()

        val after = savedTransactions()
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
        val before = savedTransactions().size

        vm.onAmountChange("0")
        vm.onSave()

        assertEquals(R.string.quick_add_amount_required, vm.uiState.value.amountError)
        assertEquals(before, savedTransactions().size)
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
    fun supportsIncomeTypeAndSavesTransaction() = runTest {
        val vm = viewModel()
        vm.onTransactionTypeSelected(TransactionType.INCOME)

        assertEquals(TransactionType.INCOME, vm.uiState.value.transactionType)

        vm.onAmountChange("1000")
        vm.onIncomeSourceSelected(IncomeSource.FREELANCE)
        assertTrue(vm.uiState.value.canSave)
        vm.onSave()

        val saved = savedTransactions().first { it.id == vm.uiState.value.savedTransactionId }
        assertEquals(TransactionType.INCOME, saved.type)
        assertEquals(100_000L, saved.amountLkrMinor)
        assertEquals(IncomeSource.FREELANCE, saved.incomeSource)
    }

    @Test
    fun supportsTransferTypeAndSavesTransaction() = runTest {
        val vm = viewModel()
        vm.onTransactionTypeSelected(TransactionType.TRANSFER)

        assertEquals(TransactionType.TRANSFER, vm.uiState.value.transactionType)

        val accs = vm.uiState.value.accounts
        assertTrue(accs.size >= 2)
        val fromId = accs[0].id
        val toId = accs[1].id

        vm.onAccountSelected(fromId)
        vm.onToAccountSelected(toId)
        vm.onAmountChange("500")

        assertTrue(vm.uiState.value.canSave)
        vm.onSave()

        val saved = savedTransactions().first { it.id == vm.uiState.value.savedTransactionId }
        assertEquals(TransactionType.TRANSFER, saved.type)
        assertEquals(50_000L, saved.amountLkrMinor)
        assertEquals(fromId, saved.accountId)
        assertEquals(toId, saved.toAccountId)
    }

    @Test
    fun transferDestinationIsNotPreselectedSoSaveWaitsForTheUsersPick() = runTest {
        val vm = viewModel()
        vm.onTransactionTypeSelected(TransactionType.TRANSFER)
        vm.onAmountChange("500")

        assertNull(vm.uiState.value.selectedToAccountId)
        assertFalse(vm.uiState.value.canSave)

        vm.onToAccountSelected(vm.uiState.value.accounts.first { it.id != vm.uiState.value.selectedAccountId }.id)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun transferSameAccountFailsWithInlineError() = runTest {
        val vm = viewModel()
        vm.onTransactionTypeSelected(TransactionType.TRANSFER)

        val accId = vm.uiState.value.accounts.first().id
        vm.onAccountSelected(accId)
        vm.onToAccountSelected(accId)
        vm.onAmountChange("500")

        assertFalse(vm.uiState.value.canSave)
        vm.onSave()

        assertEquals(R.string.quick_add_same_account_error, vm.uiState.value.errorMessage)
    }

    @Test
    fun theSavedExpenseTakesTheTypeOfTheChosenCategory() = runTest {
        val vm = viewModel()

        vm.onCategorySelected("cat_rent")
        assertEquals(SpendKind.COMMITTED, vm.uiState.value.spendKind)
        vm.onAmountChange("100")
        vm.onSave()
        val rent = savedTransactions().first { it.id == vm.uiState.value.savedTransactionId }
        assertEquals(SpendKind.COMMITTED, rent.spendKind)

        vm.onSavedHandled()
        vm.onCategorySelected("cat_shopping")
        vm.onAmountChange("100")
        vm.onSave()
        val shopping = savedTransactions().first { it.id == vm.uiState.value.savedTransactionId }
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
        val saved = savedTransactions().first { it.id == vm.uiState.value.savedTransactionId }
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
        assertTrue(savedTransactions().any { it.id == id })

        var failure: Int? = null
        vm.undo(id) { failure = it }

        assertFalse(savedTransactions().any { it.id == id })
        assertNull(failure)
    }

    @Test
    fun aFailedUndoReportsAnErrorInsteadOfFailingSilently() = runTest {
        val vm = viewModel()

        // An id the repository does not hold makes the delete fail, like a rejected write would.
        var failure: Int? = null
        vm.undo("no-such-expense") { failure = it }

        assertEquals(R.string.quick_add_undo_failed, failure)
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

    @Test
    fun startEditFillsTheFormAndLocksTheType() = runTest {
        val vm = viewModel()
        val saved = com.fundtrail.domain.model.Transaction(
            id = "txn_edit", type = TransactionType.EXPENSE, amountLkrMinor = 1_250_00,
            accountId = "default-cash", categoryId = "cat_rent", spendKind = SpendKind.COMMITTED,
            occurredAt = System.currentTimeMillis(), note = "rent",
        )
        transactions.add(saved)

        vm.startEdit("txn_edit")

        val state = vm.uiState.value
        assertTrue(state.isEditing)
        assertEquals("1250.00", state.amountText)
        assertEquals("default-cash", state.selectedAccountId)
        assertEquals("cat_rent", state.selectedCategoryId)
        assertEquals("rent", state.note)

        // Locked: picking a different type while editing has no effect.
        vm.onTransactionTypeSelected(TransactionType.INCOME)
        assertEquals(TransactionType.EXPENSE, vm.uiState.value.transactionType)
    }

    @Test
    fun savingAnEditUpdatesTheExistingEntryInPlaceWithNoUndoOffer() = runTest {
        val vm = viewModel()
        val saved = com.fundtrail.domain.model.Transaction(
            id = "txn_edit", type = TransactionType.EXPENSE, amountLkrMinor = 100_00,
            accountId = "default-cash", categoryId = "cat_rent", spendKind = SpendKind.COMMITTED,
            occurredAt = System.currentTimeMillis(), note = "old",
        )
        transactions.add(saved)
        vm.startEdit("txn_edit")

        vm.onAmountChange("250")
        vm.onNoteChange("corrected")
        vm.onSave()

        val state = vm.uiState.value
        assertEquals("txn_edit", state.savedTransactionId)
        assertTrue(state.isEditing)
        val count = savedTransactions().count { it.id == "txn_edit" }
        assertEquals(1, count)
        val edited = savedTransactions().first { it.id == "txn_edit" }
        assertEquals(250_00L, edited.amountLkrMinor)
        assertEquals("corrected", edited.note)
    }

    @Test
    fun editingANegativeCryptoIncomeKeepsTheLossSign() = runTest {
        val vm = viewModel()
        val saved = com.fundtrail.domain.model.Transaction(
            id = "txn_loss", type = TransactionType.INCOME, amountLkrMinor = -50_00,
            accountId = "default-cash", incomeSource = IncomeSource.CRYPTO,
            occurredAt = System.currentTimeMillis(), note = "old",
        )
        transactions.add(saved)
        vm.startEdit("txn_loss")

        // The amount field shows the magnitude only; editing the note must not flip the stored sign.
        vm.onNoteChange("confirmed loss")
        vm.onSave()

        val edited = savedTransactions().first { it.id == "txn_loss" }
        assertEquals(-50_00L, edited.amountLkrMinor)
    }

    @Test
    fun editingWithTheSameDateKeepsTheOriginalTimeOfDay() = runTest {
        val vm = viewModel()
        val morning = LocalDate.now(APP_ZONE).atTime(8, 0).atZone(APP_ZONE).toInstant().toEpochMilli()
        val saved = com.fundtrail.domain.model.Transaction(
            id = "txn_time", type = TransactionType.EXPENSE, amountLkrMinor = 100_00,
            accountId = "default-cash", categoryId = "cat_rent", spendKind = SpendKind.COMMITTED,
            occurredAt = morning, note = "old",
        )
        transactions.add(saved)
        vm.startEdit("txn_time")

        vm.onNoteChange("corrected")
        vm.onSave()

        assertEquals(morning, savedTransactions().first { it.id == "txn_time" }.occurredAt)
    }

    @Test
    fun salaryIncomePreselectsMainBankAndCryptoPreselectsBinance() = runTest {
        val vm = viewModel()
        vm.onTransactionTypeSelected(TransactionType.INCOME)

        assertEquals("default-main-bank", vm.uiState.value.selectedAccountId)

        vm.onIncomeSourceSelected(IncomeSource.CRYPTO)
        assertEquals("default-binance", vm.uiState.value.selectedAccountId)
    }

    @Test
    fun anAccountTheUserTappedSurvivesAnIncomeSourceChange() = runTest {
        val vm = viewModel()
        vm.onTransactionTypeSelected(TransactionType.INCOME)
        vm.onAccountSelected("default-cash")

        vm.onIncomeSourceSelected(IncomeSource.CRYPTO)

        assertEquals("default-cash", vm.uiState.value.selectedAccountId)
    }

    @Test
    fun aCryptoLossSavesANegativeAmount() = runTest {
        val vm = viewModel()
        vm.onTransactionTypeSelected(TransactionType.INCOME)
        vm.onIncomeSourceSelected(IncomeSource.CRYPTO)
        vm.onCryptoLossChange(true)
        vm.onAmountChange("50")

        vm.onSave()

        val saved = savedTransactions().first { it.id == vm.uiState.value.savedTransactionId }
        assertEquals(-50_00L, saved.amountLkrMinor)
    }

    @Test
    fun switchingAwayFromCryptoClearsTheLossToggle() = runTest {
        val vm = viewModel()
        vm.onTransactionTypeSelected(TransactionType.INCOME)
        vm.onIncomeSourceSelected(IncomeSource.CRYPTO)
        vm.onCryptoLossChange(true)

        vm.onIncomeSourceSelected(IncomeSource.SALARY)

        assertFalse(vm.uiState.value.cryptoLoss)
    }

    @Test
    fun editingALossShowsLossSelectedAndSwitchingToGainSavesAPositiveAmount() = runTest {
        val vm = viewModel()
        val fixedMonth = YearMonth.of(2026, 6)
        val fixedAt = fixedMonth.atDay(15).atTime(12, 0).atZone(APP_ZONE).toInstant().toEpochMilli()
        val saved = com.fundtrail.domain.model.Transaction(
            id = "txn_loss_edit", type = TransactionType.INCOME, amountLkrMinor = -50_00,
            accountId = "default-binance", incomeSource = IncomeSource.CRYPTO,
            occurredAt = fixedAt, note = "old",
        )
        transactions.add(saved)
        vm.startEdit("txn_loss_edit")

        assertTrue(vm.uiState.value.cryptoLoss)

        vm.onCryptoLossChange(false)
        vm.onSave()

        val edited = transactions.observeMonth(fixedMonth).first().first { it.id == "txn_loss_edit" }
        assertEquals(50_00L, edited.amountLkrMinor)
    }

    @Test
    fun editingAnEntryOnAnArchivedAccountKeepsThatAccount() = runTest {
        val vm = viewModel()
        val saved = com.fundtrail.domain.model.Transaction(
            id = "txn_archived_acc", type = TransactionType.EXPENSE, amountLkrMinor = 100_00,
            accountId = "default-binance", categoryId = "cat_rent", spendKind = SpendKind.COMMITTED,
            occurredAt = System.currentTimeMillis(), note = "old",
        )
        transactions.add(saved)
        accounts.archive("default-binance")

        vm.startEdit("txn_archived_acc")
        val editing = vm.uiState.value
        assertEquals("default-binance", editing.selectedAccountId)
        assertTrue(editing.accounts.any { it.id == "default-binance" })

        vm.onNoteChange("corrected")
        vm.onSave()

        assertEquals("default-binance", savedTransactions().first { it.id == "txn_archived_acc" }.accountId)
    }

    @Test
    fun editingATransferWithAnArchivedDestinationKeepsItAndCanSave() = runTest {
        val vm = viewModel()
        val fixedMonth = YearMonth.of(2026, 6)
        val fixedAt = fixedMonth.atDay(15).atTime(12, 0).atZone(APP_ZONE).toInstant().toEpochMilli()
        val saved = com.fundtrail.domain.model.Transaction(
            id = "txn_archived_dest", type = TransactionType.TRANSFER,
            status = com.fundtrail.domain.model.TransactionStatus.ACTUAL,
            amountLkrMinor = 100_00, accountId = "default-main-bank", toAccountId = "default-binance",
            occurredAt = fixedAt,
        )
        transactions.add(saved)
        accounts.archive("default-binance")

        vm.startEdit("txn_archived_dest")
        val editing = vm.uiState.value
        assertEquals("default-binance", editing.selectedToAccountId)
        assertTrue(editing.accounts.any { it.id == "default-binance" })
        assertNull(editing.errorMessage)
        assertTrue(editing.canSave)

        vm.onAmountChange("150")
        vm.onSave()

        assertEquals(150_00L, transactions.observeMonth(fixedMonth).first().first { it.id == "txn_archived_dest" }.amountLkrMinor)
    }

    @Test
    fun goalCostHintShowsForADiscretionaryExpenseWithAGoal() = runTest {
        setGoalNeeding(3_000_000L)
        val vm = viewModel()
        vm.onCategorySelected("cat_shopping")
        vm.onAmountChange("1500")

        // LKR 1,500 against LKR 30,000 a month: 1500 / (30000 / 30) = 1.5 days.
        assertEquals(GoalCostHint("1.5", "MacBook"), vm.uiState.value.goalCost)
    }

    @Test
    fun goalCostHintNeverBlocksSave() = runTest {
        setGoalNeeding(3_000_000L)
        val vm = viewModel()
        vm.onCategorySelected("cat_shopping")
        vm.onAmountChange("1500")

        assertNotNull(vm.uiState.value.goalCost)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun goalCostHintHiddenForACommittedCategory() = runTest {
        setGoalNeeding(3_000_000L)
        val vm = viewModel()
        vm.onCategorySelected("cat_rent")
        vm.onAmountChange("1500")

        assertNull(vm.uiState.value.goalCost)
    }

    @Test
    fun goalCostHintFollowsTheSpendKindOverride() = runTest {
        setGoalNeeding(3_000_000L)
        val vm = viewModel()
        vm.onCategorySelected("cat_rent")
        vm.onAmountChange("1500")
        vm.onSpendKindSelected(SpendKind.DISCRETIONARY)

        assertNotNull(vm.uiState.value.goalCost)
    }

    @Test
    fun goalCostHintHiddenForIncomeAndTransfers() = runTest {
        setGoalNeeding(3_000_000L)
        val vm = viewModel()
        vm.onCategorySelected("cat_shopping")
        vm.onAmountChange("1500")

        vm.onTransactionTypeSelected(TransactionType.INCOME)
        assertNull(vm.uiState.value.goalCost)
        vm.onTransactionTypeSelected(TransactionType.TRANSFER)
        assertNull(vm.uiState.value.goalCost)
    }

    @Test
    fun goalCostHintHiddenWithoutAGoalOrAnAmount() = runTest {
        setGoalNeeding(3_000_000L)
        val vm = viewModel()
        vm.onCategorySelected("cat_shopping")
        assertNull(vm.uiState.value.goalCost)

        vm.onAmountChange("1500")
        assertNotNull(vm.uiState.value.goalCost)

        goals.active.value = null
        assertNull(vm.uiState.value.goalCost)
    }

    @Test
    fun goalCostHintHiddenWhenNothingIsLeftToSave() = runTest {
        setGoalNeeding(0L)
        val vm = viewModel()
        vm.onCategorySelected("cat_shopping")
        vm.onAmountChange("1500")

        assertNull(vm.uiState.value.goalCost)
    }
}
