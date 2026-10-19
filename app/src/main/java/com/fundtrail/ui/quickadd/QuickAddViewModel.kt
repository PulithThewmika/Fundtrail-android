package com.fundtrail.ui.quickadd

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SameAccountTransferException
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.usecase.account.ObserveActiveAccountsUseCase
import com.fundtrail.domain.usecase.category.ObserveSuggestedCategoriesUseCase
import com.fundtrail.domain.usecase.expense.AddExpenseUseCase
import com.fundtrail.domain.usecase.expense.DeleteTransactionUseCase
import com.fundtrail.domain.usecase.expense.ObserveLastUsedAccountUseCase
import com.fundtrail.domain.usecase.income.AddIncomeUseCase
import com.fundtrail.domain.usecase.transfer.AddTransferUseCase
import com.fundtrail.ui.common.mapErrorToMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * What the sheet shows (SCR-20, EPIC-009). Supports Expense, Income, and Transfer types in a single ViewModel.
 */
data class QuickAddUiState(
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    @StringRes val amountError: Int? = null,
    /** Best suggestion first. */
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    /** True while the selected category is the app's suggestion rather than the user's pick. */
    val categorySuggested: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: String? = null,
    val selectedToAccountId: String? = null,
    val incomeSource: IncomeSource? = IncomeSource.SALARY,
    val date: LocalDate = LocalDate.now(APP_ZONE),
    val note: String = "",
    /** The type the expense will be saved with: the override, or else the selected category's type. */
    val spendKind: SpendKind? = null,
    val spendKindOverridden: Boolean = false,
    val moreOptionsExpanded: Boolean = false,
    val isSaving: Boolean = false,
    @StringRes val errorMessage: Int? = null,
    /** Id of the expense/transaction just saved; the UI offers Undo, closes the sheet and calls [QuickAddViewModel.onSavedHandled]. */
    val savedTransactionId: String? = null,
) {
    val canSave: Boolean
        get() {
            if (isSaving || amountText.isEmpty()) return false
            val amount = AmountInput.toMinor(amountText) ?: return false
            if (amount <= 0) return false
            return when (transactionType) {
                TransactionType.EXPENSE -> selectedCategoryId != null && selectedAccountId != null
                TransactionType.INCOME -> incomeSource != null && selectedAccountId != null
                TransactionType.TRANSFER -> selectedAccountId != null && selectedToAccountId != null && selectedAccountId != selectedToAccountId
            }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuickAddViewModel @Inject constructor(
    observeAccounts: ObserveActiveAccountsUseCase,
    observeLastUsedAccount: ObserveLastUsedAccountUseCase,
    private val observeSuggestions: ObserveSuggestedCategoriesUseCase,
    private val addExpense: AddExpenseUseCase,
    private val addIncome: AddIncomeUseCase,
    private val addTransfer: AddTransferUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
) : ViewModel() {

    /** Everything the user has typed or tapped; null means "not chosen, use the default". */
    private data class Draft(
        val transactionType: TransactionType = TransactionType.EXPENSE,
        val amountText: String = "",
        val amountError: Int? = null,
        val chosenCategoryId: String? = null,
        val chosenAccountId: String? = null,
        val chosenToAccountId: String? = null,
        val incomeSource: IncomeSource? = IncomeSource.SALARY,
        val date: LocalDate = LocalDate.now(APP_ZONE),
        val note: String = "",
        val spendKindOverride: SpendKind? = null,
        val moreOptionsExpanded: Boolean = false,
        val isSaving: Boolean = false,
        val errorMessage: Int? = null,
        val savedTransactionId: String? = null,
    )

    private val draft = MutableStateFlow(Draft())

    // The account decides the category ranking (suggestion level 1), so it is resolved first.
    private val accountChoice = combine(
        observeAccounts(),
        observeLastUsedAccount(),
        draft.map { it.chosenAccountId }.distinctUntilChanged(),
    ) { accounts, lastUsedId, chosenId ->
        val selected = listOf(chosenId, lastUsedId).firstOrNull { id -> accounts.any { it.id == id } }
            ?: accounts.firstOrNull()?.id
        accounts to selected
    }

    val uiState: StateFlow<QuickAddUiState> = accountChoice
        .flatMapLatest { (accounts, accountId) ->
            val suggestions = if (accountId == null) flowOf(emptyList()) else observeSuggestions(accountId)
            combine(draft, suggestions) { d, categories ->
                val chosen = categories.firstOrNull { it.id == d.chosenCategoryId }
                val selected = chosen ?: categories.firstOrNull()

                // Never preselected: the user taps where the money goes, so a quick save can't move it into the
                // wrong pot and silently change balances or goal progress (A-19).
                val toAccountId = d.chosenToAccountId?.takeIf { id -> accounts.any { it.id == id } }

                val sameAccountError = if (d.transactionType == TransactionType.TRANSFER && accountId != null && accountId == toAccountId) {
                    R.string.quick_add_same_account_error
                } else null

                QuickAddUiState(
                    transactionType = d.transactionType,
                    amountText = d.amountText,
                    amountError = d.amountError,
                    categories = categories,
                    selectedCategoryId = selected?.id,
                    categorySuggested = chosen == null && selected != null,
                    accounts = accounts,
                    selectedAccountId = accountId,
                    selectedToAccountId = toAccountId,
                    incomeSource = d.incomeSource,
                    date = d.date,
                    note = d.note,
                    spendKind = d.spendKindOverride ?: selected?.defaultSpendKind,
                    spendKindOverridden = d.spendKindOverride != null,
                    moreOptionsExpanded = d.moreOptionsExpanded,
                    isSaving = d.isSaving,
                    errorMessage = d.errorMessage ?: sameAccountError,
                    savedTransactionId = d.savedTransactionId,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuickAddUiState())

    fun onTransactionTypeSelected(type: TransactionType) = draft.update {
        it.copy(transactionType = type, amountError = null, errorMessage = null)
    }

    fun onAmountChange(value: String) = draft.update {
        it.copy(amountText = AmountInput.sanitize(value), amountError = null, errorMessage = null)
    }

    fun onKeypadDigit(digit: String) = draft.update {
        val current = it.amountText
        it.copy(
            amountText = AmountInput.sanitize(current + digit),
            amountError = null,
            errorMessage = null,
        )
    }

    fun onKeypadDot() = draft.update {
        val current = it.amountText
        if (current.contains('.')) it
        else it.copy(
            amountText = AmountInput.sanitize(if (current.isEmpty()) "0." else "$current."),
            amountError = null,
            errorMessage = null,
        )
    }

    fun onKeypadBackspace() = draft.update {
        val current = it.amountText
        if (current.isEmpty()) it
        else it.copy(
            amountText = current.dropLast(1),
            amountError = null,
            errorMessage = null,
        )
    }

    fun onCategorySelected(id: String) = draft.update { it.copy(chosenCategoryId = id, spendKindOverride = null) }

    fun onAccountSelected(id: String) = draft.update { it.copy(chosenAccountId = id) }

    fun onToAccountSelected(id: String) = draft.update { it.copy(chosenToAccountId = id, errorMessage = null) }

    fun onIncomeSourceSelected(source: IncomeSource) = draft.update { it.copy(incomeSource = source) }

    fun onDateChange(date: LocalDate) = draft.update { it.copy(date = date) }

    fun onNoteChange(value: String) = draft.update { it.copy(note = value) }

    /** Picking the type the category already has clears the override, so it goes on following the category. */
    fun onSpendKindSelected(kind: SpendKind) {
        val categoryKind = uiState.value.let { s -> s.categories.firstOrNull { it.id == s.selectedCategoryId }?.defaultSpendKind }
        draft.update { it.copy(spendKindOverride = kind.takeIf { k -> k != categoryKind }) }
    }

    fun onToggleMoreOptions() = draft.update { it.copy(moreOptionsExpanded = !it.moreOptionsExpanded) }

    fun onSave() {
        val state = uiState.value
        if (state.isSaving) return
        val amount = AmountInput.toMinor(state.amountText)
        if (amount == null || amount <= 0) {
            draft.update { it.copy(amountError = R.string.quick_add_amount_required) }
            return
        }

        val accountId = state.selectedAccountId ?: return
        val occurredAt = occurredAt(state.date)
        when (state.transactionType) {
            TransactionType.EXPENSE -> {
                val categoryId = state.selectedCategoryId ?: return
                val spendKind = draft.value.spendKindOverride
                save { addExpense(amount, categoryId, accountId, occurredAt, state.note, spendKind) }
            }
            TransactionType.INCOME -> {
                val source = state.incomeSource ?: return
                save { addIncome(amount, source, accountId, occurredAt, state.note) }
            }
            TransactionType.TRANSFER -> {
                val toAccountId = state.selectedToAccountId ?: return
                save { addTransfer(amount, accountId, toAccountId, occurredAt, state.note) }
            }
        }
    }

    /** One save path for all three types; the use case owns the rules, this only maps the outcome to the form. */
    private fun save(add: suspend () -> Result<Transaction>) {
        draft.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = add()) {
                // The Firestore write is queued offline, so success here looks the same online and offline (FR-39).
                is Result.Success -> draft.value = Draft(savedTransactionId = result.data.id)
                is Result.Error -> draft.update {
                    val message = if (result.exception is SameAccountTransferException) R.string.quick_add_same_account_error
                    else mapErrorToMessage(result.exception)
                    it.copy(isSaving = false, errorMessage = message)
                }
            }
        }
    }

    fun onSavedHandled() = draft.update { it.copy(savedTransactionId = null) }

    /**
     * Undo for the snackbar shown after a save. The delete runs in [viewModelScope] (architecture.md §3.3), so it
     * finishes even if the host screen goes away. If it fails, [onFailed] gets the message to show, because the
     * transaction then still counts in the totals and balances, and the user must not assume it is gone.
     */
    fun undo(transactionId: String, onFailed: (messageRes: Int) -> Unit) {
        viewModelScope.launch {
            if (deleteTransaction(transactionId) is Result.Error) onFailed(R.string.quick_add_undo_failed)
        }
    }

    /** Clears the form when the sheet is closed, so the next open starts fresh. */
    fun reset() = draft.update { Draft(savedTransactionId = it.savedTransactionId) }

    /** Today keeps the real time; a back-dated day keeps the current time of day on that date. */
    private fun occurredAt(date: LocalDate): Long =
        date.atTime(LocalTime.now(APP_ZONE)).atZone(APP_ZONE).toInstant().toEpochMilli()
}
