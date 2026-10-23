package com.fundtrail.ui.history

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.ui.common.EstimatedChip
import com.fundtrail.ui.common.MoneyFormatter
import com.fundtrail.ui.common.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** SCR-11 History (#107): grouped transaction list with edit, delete and undo. */
@Composable
fun HistoryRoute(
    onEditTransaction: (id: String) -> Unit,
    onAddEntry: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val undoLabel = stringResource(R.string.history_undo)
    val deletedMessage = stringResource(R.string.history_deleted)

    // Undo window is exactly 5 s (#109 AC), which Material's Short (4 s) and Long (10 s) durations don't give.
    LaunchedEffect(state.pendingUndoId) {
        val id = state.pendingUndoId ?: return@LaunchedEffect
        val dismissJob = launch { delay(5_000); snackbarHost.currentSnackbarData?.dismiss() }
        val result = snackbarHost.showSnackbar(message = deletedMessage, actionLabel = undoLabel)
        dismissJob.cancel()
        if (result == SnackbarResult.ActionPerformed) {
            // The restore runs in the ViewModel; this scope only shows the follow-up snackbar (§3.3).
            viewModel.onUndo(id) { messageRes -> scope.launch { snackbarHost.showSnackbar(resources.getString(messageRes)) } }
        }
        viewModel.onUndoHandled()
    }

    HistoryScreen(
        state = state,
        snackbarHost = snackbarHost,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onFilterSelected = viewModel::onFilterSelected,
        onEditTransaction = onEditTransaction,
        onDelete = viewModel::onDelete,
        onAddEntry = onAddEntry,
    )
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    snackbarHost: SnackbarHostState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onFilterSelected: (TransactionType?) -> Unit,
    onEditTransaction: (id: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onAddEntry: () -> Unit,
) {
    Scaffold(
        // The app-level Scaffold already applies the system bar insets.
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MonthSwitcher(state.month, state.canGoNext, onPreviousMonth, onNextMonth)
            FilterChips(state.filter, onFilterSelected)
            when (val content = state.content) {
                UiState.Loading -> Unit
                UiState.Empty -> EmptyState(state.month, onAddEntry)
                is UiState.Error -> ErrorState(content.message)
                is UiState.Success -> DayList(content.data, onEditTransaction, onDelete)
            }
        }
    }
}

@Composable
private fun MonthSwitcher(month: YearMonth, canGoNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    val label = remember(month) { month.format(DateTimeFormatter.ofPattern("MMMM yyyy")) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.dashboard_previous_month))
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.dashboard_next_month))
        }
    }
}

@Composable
private fun FilterChips(selected: TransactionType?, onFilterSelected: (TransactionType?) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        FilterChip(selected == null, { onFilterSelected(null) }, { Text(stringResource(R.string.history_filter_all)) })
        FilterChip(selected == TransactionType.EXPENSE, { onFilterSelected(TransactionType.EXPENSE) }, { Text(stringResource(R.string.quick_add_type_expense)) })
        FilterChip(selected == TransactionType.INCOME, { onFilterSelected(TransactionType.INCOME) }, { Text(stringResource(R.string.quick_add_type_income)) })
        FilterChip(selected == TransactionType.TRANSFER, { onFilterSelected(TransactionType.TRANSFER) }, { Text(stringResource(R.string.quick_add_type_transfer)) })
    }
}

@Composable
private fun DayList(days: List<DayUi>, onEditTransaction: (id: String) -> Unit, onDelete: (id: String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        days.forEach { day ->
            item(key = "day-${day.key}") { DayHeader(day) }
            items(day.rows, key = { it.id }) { row -> TransactionRow(row, onEditTransaction, onDelete) }
        }
    }
}

@Composable
private fun DayHeader(day: DayUi) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(day.label.resolve(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(MoneyFormatter.formatLkr(day.netMinor), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun DayLabel.resolve(): String = when (this) {
    DayLabel.Today -> stringResource(R.string.history_today)
    DayLabel.Yesterday -> stringResource(R.string.history_yesterday)
    is DayLabel.Dated -> text
}

@Composable
private fun TransactionRow(row: TransactionRowUi, onEdit: (id: String) -> Unit, onDelete: (id: String) -> Unit) {
    val title = when (row.type) {
        TransactionType.EXPENSE -> row.categoryName ?: stringResource(R.string.dashboard_unknown_category)
        TransactionType.INCOME -> stringResource(row.incomeSource.labelRes())
        // A distinct style for transfers, per FR-100/FR-61: neither income nor expense.
        TransactionType.TRANSFER -> stringResource(R.string.quick_add_type_transfer)
    }
    val subtitle = if (row.type == TransactionType.TRANSFER) "${row.accountName} → ${row.toAccountName}" else row.accountName
    val amountColor = if (row.type == TransactionType.TRANSFER) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onEdit(row.id) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.type == TransactionType.TRANSFER) {
            // Decorative only: the title already reads "Transfer" (#106 AC 2).
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Weighted so a long category name shortens instead of squeezing the chip out of the row.
                Text(
                    title,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                )
                // Only when the amount is still an estimate; tapping the row opens the sheet to confirm it (#177).
                if (row.isEstimated) EstimatedChip()
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(MoneyFormatter.formatLkr(row.amountMinor), color = amountColor, style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = { onDelete(row.id) }) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.history_delete))
        }
    }
}

@Composable
private fun EmptyState(month: YearMonth, onAddEntry: () -> Unit) {
    val monthLabel = remember(month) { month.format(DateTimeFormatter.ofPattern("MMMM")) }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Neutral wording only: no streaks, warnings or "incomplete" language (A-03, A-22, FR-106).
        Text(stringResource(R.string.history_empty_title, monthLabel), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onAddEntry, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.history_add_entry))
        }
    }
}

@Composable
private fun ErrorState(@StringRes message: Int) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(message))
    }
}

@StringRes
private fun IncomeSource?.labelRes(): Int = when (this) {
    IncomeSource.SALARY -> R.string.income_source_salary
    IncomeSource.FREELANCE -> R.string.income_source_freelance
    IncomeSource.ADSENSE -> R.string.income_source_adsense
    IncomeSource.CRYPTO -> R.string.income_source_crypto
    IncomeSource.OTHER, null -> R.string.income_source_other
}
