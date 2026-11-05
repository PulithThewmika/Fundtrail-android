package com.fundtrail.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionDay
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.usecase.account.ObserveAccountsUseCase
import com.fundtrail.domain.usecase.category.ObserveCategoriesUseCase
import com.fundtrail.domain.usecase.expense.DeleteTransactionUseCase
import com.fundtrail.domain.usecase.sync.ObserveSyncStatusUseCase
import com.fundtrail.domain.usecase.transaction.ObserveTransactionHistoryUseCase
import com.fundtrail.domain.usecase.transaction.RestoreTransactionUseCase
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.common.mapErrorToMessage
import com.fundtrail.ui.common.uiStateOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** "Today", "Yesterday", or a dated day; the screen resolves [Today] and [Yesterday] with string resources. */
sealed interface DayLabel {
    data object Today : DayLabel
    data object Yesterday : DayLabel
    data class Dated(val text: String) : DayLabel
}

/** One row in the History list (SCR-11, #108). Only the field for [type] is non-null; the screen picks it. */
data class TransactionRowUi(
    val id: String,
    val type: TransactionType,
    val amountMinor: Long,
    val categoryName: String?,
    val incomeSource: IncomeSource?,
    val accountName: String,
    val toAccountName: String?,
    /** The LKR figure came from a typed rate, not the amount that landed (#177). */
    val isEstimated: Boolean = false,
)

/** [key] is the ISO date, stable regardless of label text, for the `LazyColumn` header's key (#108 AC). */
data class DayUi(val key: String, val label: DayLabel, val netMinor: Long, val rows: List<TransactionRowUi>)

data class HistoryUiState(
    val month: YearMonth,
    val canGoNext: Boolean,
    val filter: TransactionType? = null,
    /** FR-102: matched against the note (merchant text lives there, there's no separate field). */
    val search: String = "",
    /** FR-101. */
    val accountFilter: String? = null,
    val categoryFilter: String? = null,
    val sourceFilter: IncomeSource? = null,
    val accounts: List<Pair<String, String>> = emptyList(),
    val categories: List<Pair<String, String>> = emptyList(),
    val content: UiState<List<DayUi>> = UiState.Loading,
    /** Set right after a delete; the screen shows the Undo snackbar for 5 s then calls [HistoryViewModel.onUndoHandled]. */
    val pendingUndoId: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val observeHistory: ObserveTransactionHistoryUseCase,
    observeAccounts: ObserveAccountsUseCase,
    observeCategories: ObserveCategoriesUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val restoreTransaction: RestoreTransactionUseCase,
) : ViewModel() {

    private val currentMonth = YearMonth.now(APP_ZONE)
    private val selection = MutableStateFlow(Selection(currentMonth, filter = null))
    private val pendingUndoId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HistoryUiState> = selection
        .flatMapLatest { sel ->
            combine(
                observeHistory(sel.month, sel.filter),
                observeAccounts(),
                observeCategories(),
                observeSyncStatus(),
                pendingUndoId,
            ) { days, accounts, categories, sync, undoId ->
                val accountNames = accounts.associate { it.id to it.name }
                val categoryNames = categories.associate { it.id to it.name }
                val filtered = days.mapNotNull { day -> day.filtered(sel) }
                val content = uiStateOf(days.isNotEmpty(), sync) {
                    filtered.map { it.toUi(accountNames, categoryNames) }
                }
                stateFor(sel, content, undoId, accounts.map { it.id to it.name }, categories.map { it.id to it.name })
            }
                .onStart { emit(stateFor(sel, UiState.Loading, null, emptyList(), emptyList())) }
                .catch { emit(stateFor(sel, UiState.Error(mapErrorToMessage(it)), null, emptyList(), emptyList())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), stateFor(Selection(currentMonth, null), UiState.Loading, null, emptyList(), emptyList()))

    fun onPreviousMonth() = selection.update { it.copy(month = it.month.minusMonths(1)) }

    fun onNextMonth() = selection.update { if (it.month < currentMonth) it.copy(month = it.month.plusMonths(1)) else it }

    fun onFilterSelected(type: TransactionType?) = selection.update { it.copy(filter = type) }
    fun onSearchChange(value: String) = selection.update { it.copy(search = value) }
    fun onAccountFilterSelected(accountId: String?) = selection.update { it.copy(accountFilter = accountId) }
    fun onCategoryFilterSelected(categoryId: String?) = selection.update { it.copy(categoryFilter = categoryId) }
    fun onSourceFilterSelected(source: IncomeSource?) = selection.update { it.copy(sourceFilter = source) }

    fun onDelete(id: String) {
        viewModelScope.launch {
            if (deleteTransaction(id) is Result.Success) pendingUndoId.value = id
        }
    }

    /** Runs in [viewModelScope] (architecture.md §3.3) so it finishes even if the snackbar's host screen goes away. */
    fun onUndo(id: String, onFailed: (messageRes: Int) -> Unit) {
        viewModelScope.launch {
            if (restoreTransaction(id) is Result.Error) onFailed(R.string.history_undo_failed)
        }
    }

    fun onUndoHandled() {
        pendingUndoId.value = null
    }

    private fun stateFor(
        sel: Selection,
        content: UiState<List<DayUi>>,
        undoId: String?,
        accounts: List<Pair<String, String>>,
        categories: List<Pair<String, String>>,
    ) = HistoryUiState(
        sel.month, canGoNext = sel.month < currentMonth, filter = sel.filter, search = sel.search,
        accountFilter = sel.accountFilter, categoryFilter = sel.categoryFilter, sourceFilter = sel.sourceFilter,
        accounts = accounts, categories = categories, content = content, pendingUndoId = undoId,
    )

    /** FR-101/FR-102, applied over what the month+type query already narrowed down. Null when nothing matches. */
    private fun TransactionDay.filtered(sel: Selection): TransactionDay? {
        val rows = transactions.filter { tx ->
            (sel.search.isBlank() || tx.note?.contains(sel.search, ignoreCase = true) == true) &&
                (sel.accountFilter == null || tx.accountId == sel.accountFilter || tx.toAccountId == sel.accountFilter) &&
                (sel.categoryFilter == null || tx.categoryId == sel.categoryFilter) &&
                (sel.sourceFilter == null || tx.incomeSource == sel.sourceFilter)
        }
        if (rows.isEmpty()) return null
        // Same rule as ObserveTransactionHistoryUseCase.netMinor(): actual income minus actual expense, transfers never count.
        val net = rows.sumOf {
            when {
                it.status != TransactionStatus.ACTUAL || it.type == TransactionType.TRANSFER -> 0L
                it.type == TransactionType.EXPENSE -> -it.amountLkrMinor
                else -> it.amountLkrMinor
            }
        }
        return TransactionDay(date = date, netMinor = net, transactions = rows)
    }

    private fun TransactionDay.toUi(accountNames: Map<String, String>, categoryNames: Map<String, String>) = DayUi(
        key = date.toString(),
        label = dayLabel(date, LocalDate.now(APP_ZONE)),
        netMinor = netMinor,
        rows = transactions.map { it.toRowUi(accountNames, categoryNames) },
    )

    private fun Transaction.toRowUi(accountNames: Map<String, String>, categoryNames: Map<String, String>) = TransactionRowUi(
        id = id,
        type = type,
        // Display sign only; the stored amount already carries the real sign for crypto income (ADR-0003).
        amountMinor = if (type == TransactionType.EXPENSE) -amountLkrMinor else amountLkrMinor,
        categoryName = categoryId?.let { categoryNames[it] },
        incomeSource = incomeSource,
        accountName = accountNames[accountId] ?: accountId,
        toAccountName = toAccountId?.let { accountNames[it] ?: it },
        isEstimated = status == TransactionStatus.EXPECTED,
    )

    private data class Selection(
        val month: YearMonth,
        val filter: TransactionType?,
        val search: String = "",
        val accountFilter: String? = null,
        val categoryFilter: String? = null,
        val sourceFilter: IncomeSource? = null,
    )
}

/** A pure function so "Today"/"Yesterday" switchover is unit-testable without a fixed Clock (#108 AC). */
fun dayLabel(date: LocalDate, today: LocalDate): DayLabel = when (date) {
    today -> DayLabel.Today
    today.minusDays(1) -> DayLabel.Yesterday
    else -> DayLabel.Dated(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
}
