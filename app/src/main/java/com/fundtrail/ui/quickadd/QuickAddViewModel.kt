package com.fundtrail.ui.quickadd

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.FxConversion
import com.fundtrail.domain.model.IncomeDefaults
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.LKR
import com.fundtrail.domain.model.OriginalAmount
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SameAccountTransferException
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
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
import com.fundtrail.domain.usecase.income.ObserveLastUsedRatesUseCase
import com.fundtrail.domain.usecase.income.SaveLastUsedRateUseCase
import com.fundtrail.domain.usecase.transaction.ObserveTransactionUseCase
import com.fundtrail.domain.usecase.transaction.UpdateTransactionUseCase
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** The cost of the amount being typed in days of goal saving, e.g. 1.8 days of "MacBook" (A-23, #171). */
data class GoalCostHint(val days: String, val goalName: String)

/**
 * Which of the two FX numbers the user types for a non-LKR income entry (#176). The other one is derived.
 * [LkrReceived] is the amount that actually landed, so it saves as ACTUAL; [Rate] is a guess, so it saves
 * as EXPECTED until it is confirmed (#177).
 */
enum class FxMode { LkrReceived, Rate }

/** The resolved pair for a non-LKR entry: whichever number was typed plus the one derived from it (#174). */
data class FxValues(val lkrMinor: Long, val rateMicros: Long)

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
    /** The currency the typed amount is in; always [LKR] outside the Income tab (#174). */
    val currency: String = LKR,
    val fxMode: FxMode = FxMode.LkrReceived,
    val rateText: String = "",
    val lkrText: String = "",
    /** Both FX numbers once one of them is typeable; null for an LKR entry or while the pair is incomplete. */
    val fx: FxValues? = null,
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
    /** True while editing an existing entry from History (#109): the type tabs lock and Save updates it in place. */
    val isEditing: Boolean = false,
    /** True when [rateText] is the remembered rate rather than something the user typed (#179). */
    val ratePrefilled: Boolean = false,
    /** True when the Crypto income being entered is a realised loss (#104); meaningless for other sources. */
    val cryptoLoss: Boolean = false,
    /** True while editing an entry still stored as EXPECTED: the sheet shows the chip and offers to confirm it (#177). */
    val isEstimated: Boolean = false,
    /** Neutral goal-cost hint under the amount; null hides it. Never affects [canSave]. */
    val goalCost: GoalCostHint? = null,
) {
    /** A typed rate is a guess, so the entry saves as EXPECTED; the LKR that actually landed saves as ACTUAL (#176). */
    val fxStatus: TransactionStatus
        get() = if (currency != LKR && fxMode == FxMode.Rate) TransactionStatus.EXPECTED else TransactionStatus.ACTUAL

    val canSave: Boolean
        get() {
            if (isSaving || amountText.isEmpty()) return false
            val amount = AmountInput.toMinor(amountText) ?: return false
            if (amount <= 0) return false
            return when (transactionType) {
                TransactionType.EXPENSE -> selectedCategoryId != null && selectedAccountId != null
                // A foreign entry also needs one of the two FX numbers, or there is no LKR figure to store (#174).
                TransactionType.INCOME -> incomeSource != null && selectedAccountId != null && (currency == LKR || fx != null)
                TransactionType.TRANSFER -> selectedAccountId != null && selectedToAccountId != null && selectedAccountId != selectedToAccountId
            }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuickAddViewModel @Inject constructor(
    observeAccounts: ObserveActiveAccountsUseCase,
    observeAllAccounts: ObserveAccountsUseCase,
    observeLastUsedAccount: ObserveLastUsedAccountUseCase,
    private val observeSuggestions: ObserveSuggestedCategoriesUseCase,
    private val observeAllCategories: ObserveCategoriesUseCase,
    private val addExpense: AddExpenseUseCase,
    private val addIncome: AddIncomeUseCase,
    observeLastUsedRates: ObserveLastUsedRatesUseCase,
    private val saveLastUsedRate: SaveLastUsedRateUseCase,
    private val addTransfer: AddTransferUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val observeTransaction: ObserveTransactionUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    observeActiveGoal: ObserveActiveGoalUseCase,
    getGoalProgress: GetGoalProgressUseCase,
    private val goalCost: GoalCostUseCase,
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
        /** null means "follow the income source's default" ([IncomeDefaults.currencyFor]). */
        val chosenCurrency: String? = null,
        val fxMode: FxMode = FxMode.LkrReceived,
        val rateText: String = "",
        val lkrText: String = "",
        val date: LocalDate = LocalDate.now(APP_ZONE),
        val note: String = "",
        val spendKindOverride: SpendKind? = null,
        val moreOptionsExpanded: Boolean = false,
        val isSaving: Boolean = false,
        val errorMessage: Int? = null,
        val savedTransactionId: String? = null,
        /** The entry being edited, kept for the fields the form doesn't show (e.g. `createdAt`). Null = adding new. */
        val editingOriginal: Transaction? = null,
        val cryptoLoss: Boolean = false,
    )

    /** The fields that decide the account default (#103 AC 2), kept together so [distinctUntilChanged] sees one key. */
    private data class AccountChoiceKey(
        val chosenAccountId: String?,
        val editingAccountId: String?,
        val editingToAccountId: String?,
        val transactionType: TransactionType,
        val incomeSource: IncomeSource?,
    )

    private val draft = MutableStateFlow(Draft())

    /** Last-used rate per currency (#179); empty until the first non-LKR income is saved. */
    private val lastUsedRates = observeLastUsedRates()

    private data class GoalInfo(val name: String, val requiredMonthlyMinor: Long)

    private val goalInfo = combine(observeActiveGoal(), getGoalProgress()) { goal, progress ->
        if (goal != null && progress != null) GoalInfo(goal.name, progress.requiredMonthlyMinor) else null
    }

    // The account decides the category ranking (suggestion level 1), so it is resolved first.
    private val accountChoice = combine(
        observeAccounts(),
        observeAllAccounts(),
        observeLastUsedAccount(),
        draft.map {
            AccountChoiceKey(it.chosenAccountId, it.editingOriginal?.accountId, it.editingOriginal?.toAccountId, it.transactionType, it.incomeSource)
        }.distinctUntilChanged(),
    ) { activeAccounts, allAccounts, lastUsedId, key ->
        val (chosenId, editingAccountId, editingToAccountId, transactionType, incomeSource) = key
        // Editing an entry on a now-archived account must keep showing and keeping it, not silently move the
        // entry to whichever account a plain add would default to (it is not offered to a new expense, same
        // as #109's archived-category rule below). A transfer's destination gets the same treatment, so an
        // edit isn't stuck unable to save just because the other side of the transfer was archived.
        val incomeDefault = if (transactionType == TransactionType.INCOME) {
            IncomeDefaults.accountFor(incomeSource ?: IncomeSource.SALARY, activeAccounts, lastUsedId)
        } else {
            null
        }
        val selected = listOf(chosenId, editingAccountId, incomeDefault, lastUsedId).firstOrNull { id ->
            id != null && (id == editingAccountId || activeAccounts.any { it.id == id })
        } ?: activeAccounts.firstOrNull()?.id
        val archivedEditingAccounts = listOfNotNull(editingAccountId, editingToAccountId)
            .filter { id -> activeAccounts.none { it.id == id } }
            .distinct()
            .mapNotNull { id -> allAccounts.firstOrNull { it.id == id } }
        val accounts = if (archivedEditingAccounts.isEmpty()) activeAccounts else archivedEditingAccounts + activeAccounts
        accounts to selected
    }

    val uiState: StateFlow<QuickAddUiState> = accountChoice
        .flatMapLatest { (accounts, accountId) ->
            val suggestions = if (accountId == null) flowOf(emptyList()) else observeSuggestions(accountId)
            combine(draft, suggestions, observeAllCategories(), goalInfo, lastUsedRates) { d, suggested, all, goal, rates ->
                // Editing an entry whose category is now archived still shows it, rather than silently moving to
                // the top suggestion (#109): it is not offered to a new expense, only kept for this one.
                val editedCategoryId = d.editingOriginal?.categoryId
                val categories = if (editedCategoryId != null && suggested.none { it.id == editedCategoryId }) {
                    all.firstOrNull { it.id == editedCategoryId }?.let { listOf(it) + suggested } ?: suggested
                } else {
                    suggested
                }
                val chosen = categories.firstOrNull { it.id == d.chosenCategoryId }
                val selected = chosen ?: categories.firstOrNull()

                // Never preselected: the user taps where the money goes, so a quick save can't move it into the
                // wrong pot and silently change balances or goal progress (A-19).
                val toAccountId = d.chosenToAccountId?.takeIf { id -> accounts.any { it.id == id } }

                val sameAccountError = if (d.transactionType == TransactionType.TRANSFER && accountId != null && accountId == toAccountId) {
                    R.string.quick_add_same_account_error
                } else null

                val amountMinor = AmountInput.toMinor(d.amountText)
                // Income only: the Expense and Transfer tabs are LKR by definition (#174 scope).
                val currency = if (d.transactionType == TransactionType.INCOME) {
                    d.chosenCurrency ?: IncomeDefaults.currencyFor(d.incomeSource ?: IncomeSource.SALARY)
                } else {
                    LKR
                }
                // Rate mode prefills the last-used rate for this currency when the user has not typed one (#179);
                // typing anything replaces it, and clearing the field brings the prefill back.
                val prefilledRate = if (currency != LKR && d.rateText.isEmpty()) rates[currency] else null
                val effectiveRateText = d.rateText.ifEmpty { prefilledRate?.let { AmountInput.fromMicros(it) }.orEmpty() }
                val fx = fxValues(currency, amountMinor, d.fxMode, effectiveRateText, d.lkrText)

                val spendKind = d.spendKindOverride ?: selected?.defaultSpendKind
                // Only a discretionary expense gets the hint; income, transfers and committed spends never do.
                val hint = if (d.transactionType == TransactionType.EXPENSE && spendKind == SpendKind.DISCRETIONARY && goal != null) {
                    amountMinor?.let { goalCost(it, goal.requiredMonthlyMinor) }
                        ?.let { GoalCostHint(it.toPlainString(), goal.name) }
                } else {
                    null
                }

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
                    currency = currency,
                    fxMode = d.fxMode,
                    rateText = effectiveRateText,
                    lkrText = d.lkrText,
                    ratePrefilled = prefilledRate != null,
                    fx = fx,
                    date = d.date,
                    note = d.note,
                    spendKind = spendKind,
                    spendKindOverridden = d.spendKindOverride != null,
                    moreOptionsExpanded = d.moreOptionsExpanded,
                    isSaving = d.isSaving,
                    errorMessage = d.errorMessage ?: sameAccountError,
                    savedTransactionId = d.savedTransactionId,
                    isEditing = d.editingOriginal != null,
                    cryptoLoss = d.cryptoLoss,
                    isEstimated = d.editingOriginal?.status == TransactionStatus.EXPECTED,
                    goalCost = hint,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuickAddUiState())

    /** The type tabs are locked while editing (#109), so changing type here would desync it from [editingOriginal]. */
    fun onTransactionTypeSelected(type: TransactionType) = draft.update {
        if (it.editingOriginal != null) it else it.copy(transactionType = type, amountError = null, errorMessage = null)
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

    /**
     * Switching away from Crypto clears the loss toggle: it has no meaning for any other source. The currency
     * goes back to following the new source default, and the FX fields clear with it: a USD rate would be
     * nonsense against USDT (#174).
     */
    fun onIncomeSourceSelected(source: IncomeSource) = draft.update {
        it.copy(
            incomeSource = source,
            cryptoLoss = if (source == IncomeSource.CRYPTO) it.cryptoLoss else false,
            chosenCurrency = null,
            rateText = "",
            lkrText = "",
        )
    }

    /** The original amount survives a currency change (#176 AC 3); the rate and LKR figures do not. */
    fun onCurrencySelected(currency: String) = draft.update {
        it.copy(chosenCurrency = currency, rateText = "", lkrText = "", errorMessage = null)
    }

    /** Switching mode keeps the original amount and both typed figures, so a mis-tap costs nothing (#176 AC 3). */
    fun onFxModeSelected(mode: FxMode) = draft.update { it.copy(fxMode = mode, errorMessage = null) }

    fun onRateChange(value: String) = draft.update {
        it.copy(rateText = AmountInput.sanitizeRate(value), errorMessage = null)
    }

    fun onLkrReceivedChange(value: String) = draft.update {
        it.copy(lkrText = AmountInput.sanitize(value), errorMessage = null)
    }

    fun onCryptoLossChange(isLoss: Boolean) = draft.update { it.copy(cryptoLoss = isLoss) }

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
        val original = draft.value.editingOriginal
        if (original != null) {
            // An untouched date keeps its original time of day too, so editing only the note or amount
            // doesn't reorder the entry within its day (#109: back-dating is what should move it).
            val originalDate = Instant.ofEpochMilli(original.occurredAt).atZone(APP_ZONE).toLocalDate()
            val occurredAt = if (originalDate == state.date) original.occurredAt else occurredAt(state.date)
            saveEdit(original) { updateTransaction(editedTransaction(original, state, amount, accountId, occurredAt), draft.value.spendKindOverride) }
            return
        }
        val occurredAt = occurredAt(state.date)
        when (state.transactionType) {
            TransactionType.EXPENSE -> {
                val categoryId = state.selectedCategoryId ?: return
                val spendKind = draft.value.spendKindOverride
                save { addExpense(amount, categoryId, accountId, occurredAt, state.note, spendKind) }
            }
            TransactionType.INCOME -> {
                val source = state.incomeSource ?: return
                // Remember the rate only once the save succeeds, so a rate typed and abandoned never sticks (#179).
                val rememberRate = state.fx?.let { state.currency to it.rateMicros }
                save(
                    afterSuccess = { rememberRate?.let { (currency, rate) -> saveLastUsedRate(currency, rate) } },
                ) {
                    addIncome(
                        amountLkrMinor = state.fx?.lkrMinor ?: amount,
                        source = source,
                        accountId = accountId,
                        occurredAt = occurredAt,
                        note = state.note,
                        isLoss = state.cryptoLoss,
                        original = state.fx?.let { OriginalAmount(state.currency, amount, it.rateMicros) },
                        status = state.fxStatus,
                    )
                }
            }
            TransactionType.TRANSFER -> {
                val toAccountId = state.selectedToAccountId ?: return
                save { addTransfer(amount, accountId, toAccountId, occurredAt, state.note) }
            }
        }
    }

    /** Keeps everything the form doesn't show (id, createdAt, …) and writes back only the fields it does. */
    private fun editedTransaction(original: Transaction, state: QuickAddUiState, amount: Long, accountId: String, occurredAt: Long) =
        when (original.type) {
            TransactionType.EXPENSE -> original.copy(
                amountLkrMinor = amount, accountId = accountId, categoryId = state.selectedCategoryId,
                occurredAt = occurredAt, note = state.note.trim().ifEmpty { null },
            )
            TransactionType.INCOME -> {
                // The field shows the magnitude only (AmountInput.fromMinor); the Gain/Loss toggle carries the sign.
                val lkr = state.fx?.lkrMinor ?: amount
                original.copy(
                    status = confirmedStatus(original, state),
                    amountLkrMinor = if (state.cryptoLoss) -lkr else lkr,
                    accountId = accountId, incomeSource = state.incomeSource,
                    original = state.fx?.let {
                        OriginalAmount(state.currency, if (state.cryptoLoss) -amount else amount, it.rateMicros)
                    },
                    occurredAt = occurredAt, note = state.note.trim().ifEmpty { null },
                )
            }
            TransactionType.TRANSFER -> original.copy(
                amountLkrMinor = amount, accountId = accountId, toAccountId = state.selectedToAccountId,
                occurredAt = occurredAt, note = state.note.trim().ifEmpty { null },
            )
        }

    /** One save path for all three types; the use case owns the rules, this only maps the outcome to the form. */
    private fun save(afterSuccess: suspend () -> Unit = {}, add: suspend () -> Result<Transaction>) {
        draft.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = add()) {
                // The Firestore write is queued offline, so success here looks the same online and offline (FR-39).
                is Result.Success -> {
                    afterSuccess()
                    draft.value = Draft(savedTransactionId = result.data.id)
                }
                is Result.Error -> draft.update {
                    val message = if (result.exception is SameAccountTransferException) R.string.quick_add_same_account_error
                    else mapErrorToMessage(result.exception)
                    it.copy(isSaving = false, errorMessage = message)
                }
            }
        }
    }

    /**
     * Saves an edit. Keeps [Draft.editingOriginal] on success (unlike [save]) so [QuickAddUiState.isEditing] is
     * still true when the Route's effect reads [QuickAddUiState.savedTransactionId], and it skips the Undo offer.
     */
    private fun saveEdit(original: Transaction, update: suspend () -> Result<Transaction>) {
        draft.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = update()) {
                is Result.Success -> draft.value = Draft(savedTransactionId = result.data.id, editingOriginal = original)
                is Result.Error -> draft.update { it.copy(isSaving = false, errorMessage = mapErrorToMessage(result.exception)) }
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

    /** Loads an existing entry into the form for the History row tap (#109). The type tabs lock on its type. */
    fun startEdit(id: String) {
        viewModelScope.launch {
            val txn = observeTransaction(id).first() ?: return@launch
            draft.value = Draft(
                transactionType = txn.type,
                amountText = AmountInput.fromMinor(txn.original?.amountMinor ?: txn.amountLkrMinor),
                chosenCategoryId = txn.categoryId,
                chosenAccountId = txn.accountId,
                chosenToAccountId = txn.toAccountId,
                incomeSource = txn.incomeSource ?: IncomeSource.SALARY,
                // A foreign entry types its original amount, so the field shows that and the locked LKR figure
                // goes into the "LKR received" box: editing it re-derives the rate for this entry only (#177).
                chosenCurrency = txn.original?.currency,
                lkrText = txn.original?.let { AmountInput.fromMinor(txn.amountLkrMinor) }.orEmpty(),
                date = Instant.ofEpochMilli(txn.occurredAt).atZone(APP_ZONE).toLocalDate(),
                note = txn.note.orEmpty(),
                spendKindOverride = txn.spendKind,
                editingOriginal = txn,
                cryptoLoss = txn.type == TransactionType.INCOME && txn.amountLkrMinor < 0,
            )
        }
    }

    /** Clears the form when the sheet is closed, so the next open starts fresh. */
    fun reset() = draft.update { Draft(savedTransactionId = it.savedTransactionId) }

    /**
     * Confirming an estimate (#177): an entry stored as EXPECTED becomes ACTUAL the moment the user gives the
     * LKR that actually landed, and only that entry changes. Switching back to Rate mode leaves it EXPECTED.
     * An entry that is already ACTUAL is never downgraded, and an EXPECTED entry with no `original` block is
     * left alone — those are the expected-salary rows, which EPIC-014 confirms its own way.
     */
    private fun confirmedStatus(stored: Transaction, state: QuickAddUiState): TransactionStatus = when {
        stored.status == TransactionStatus.ACTUAL -> TransactionStatus.ACTUAL
        stored.original == null -> stored.status
        state.fxMode == FxMode.Rate -> TransactionStatus.EXPECTED
        else -> TransactionStatus.ACTUAL
    }

    /**
     * Resolves the two FX numbers from whichever one the user typed (#176). Both are magnitudes: the Gain/Loss
     * toggle applies the sign on save, so a loss cannot flip the rate negative.
     */
    private fun fxValues(currency: String, amountMinor: Long?, mode: FxMode, rateText: String, lkrText: String): FxValues? {
        if (currency == LKR) return null
        val amount = amountMinor?.takeIf { it > 0 } ?: return null
        return when (mode) {
            FxMode.Rate -> AmountInput.toMicros(rateText)?.takeIf { it > 0 }
                ?.let { FxValues(FxConversion.lkrFromRate(amount, it), it) }
            FxMode.LkrReceived -> AmountInput.toMinor(lkrText)?.takeIf { it > 0 }
                ?.let { FxValues(it, FxConversion.rateFromLkr(amount, it)) }
        }
    }

    /** Today keeps the real time; a back-dated day keeps the current time of day on that date. */
    private fun occurredAt(date: LocalDate): Long =
        date.atTime(LocalTime.now(APP_ZONE)).atZone(APP_ZONE).toInstant().toEpochMilli()
}
