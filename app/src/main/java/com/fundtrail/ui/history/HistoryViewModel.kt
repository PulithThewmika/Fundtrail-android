package com.fundtrail.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionDay
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
)

/** [key] is the ISO date, stable regardless of label text, for the `LazyColumn` header's key (#108 AC). */
data class DayUi(val key: String, val label: DayLabel, val netMinor: Long, val rows: List<TransactionRowUi>)

data class HistoryUiState(
    val month: YearMonth,
    val canGoNext: Boolean,
    val filter: TransactionType? = null,
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
        .flatMapLatest { (month, filter) ->
            combine(
                observeHistory(month, filter),
                observeAccounts(),
                observeCategories(),
                observeSyncStatus(),
                pendingUndoId,
            ) { days, accounts, categories, sync, undoId ->
                val accountNames = accounts.associate { it.id to it.name }
                val categoryNames = categories.associate { it.id to it.name }
                val content = uiStateOf(days.isNotEmpty(), sync) {
                    days.map { it.toUi(accountNames, categoryNames) }
                }
                stateFor(month, filter, content, undoId)
            }
                .onStart { emit(stateFor(month, filter, UiState.Loading, null)) }
                .catch { emit(stateFor(month, filter, UiState.Error(mapErrorToMessage(it)), null)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), stateFor(currentMonth, null, UiState.Loading, null))

    fun onPreviousMonth() = selection.update { it.copy(month = it.month.minusMonths(1)) }

    fun onNextMonth() = selection.update { if (it.month < currentMonth) it.copy(month = it.month.plusMonths(1)) else it }

    fun onFilterSelected(type: TransactionType?) = selection.update { it.copy(filter = type) }

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

    private fun stateFor(month: YearMonth, filter: TransactionType?, content: UiState<List<DayUi>>, undoId: String?) =
        HistoryUiState(month, canGoNext = month < currentMonth, filter = filter, content = content, pendingUndoId = undoId)

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
    )

    private class Selection(val month: YearMonth, val filter: TransactionType?) {
        operator fun component1() = month
        operator fun component2() = filter
        fun copy(month: YearMonth = this.month, filter: TransactionType? = this.filter) = Selection(month, filter)
    }
}

/** A pure function so "Today"/"Yesterday" switchover is unit-testable without a fixed Clock (#108 AC). */
fun dayLabel(date: LocalDate, today: LocalDate): DayLabel = when (date) {
    today -> DayLabel.Today
    today.minusDays(1) -> DayLabel.Yesterday
    else -> DayLabel.Dated(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
}
