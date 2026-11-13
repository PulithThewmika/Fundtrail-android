package com.fundtrail.ui.history

import com.fundtrail.ui.common.rememberHaptics
import com.fundtrail.ui.common.positive
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.ui.graphics.Color
import com.fundtrail.ui.common.AppScreen
import com.fundtrail.ui.common.IconBadge
import com.fundtrail.ui.common.MonthSwitcher
import com.fundtrail.ui.common.card
import com.fundtrail.ui.theme.Spacing
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.ui.common.EstimatedChip
import com.fundtrail.ui.common.Skeleton
import com.fundtrail.ui.common.SkeletonLine
import com.fundtrail.ui.common.SkeletonPill
import com.fundtrail.ui.common.MoneyFormatter
import com.fundtrail.ui.common.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import com.fundtrail.ui.common.EmptyView
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.Refresh

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
        onSearchChange = viewModel::onSearchChange,
        onAccountFilterSelected = viewModel::onAccountFilterSelected,
        onCategoryFilterSelected = viewModel::onCategoryFilterSelected,
        onSourceFilterSelected = viewModel::onSourceFilterSelected,
        onEditTransaction = onEditTransaction,
        onDelete = viewModel::onDelete,
        onAddEntry = onAddEntry,
        onRetry = viewModel::onRetry,
    )
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    snackbarHost: SnackbarHostState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onFilterSelected: (TransactionType?) -> Unit,
    onSearchChange: (String) -> Unit = {},
    onAccountFilterSelected: (String?) -> Unit = {},
    onCategoryFilterSelected: (String?) -> Unit = {},
    onSourceFilterSelected: (IncomeSource?) -> Unit = {},
    onEditTransaction: (id: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onAddEntry: () -> Unit,
    onRetry: () -> Unit,
) {
    AppScreen(
        title = stringResource(R.string.history_title),
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        val controls: @Composable () -> Unit = {
            MonthSwitcher(state.month, state.canGoNext, onPreviousMonth, onNextMonth)
            SearchField(state.search, onSearchChange)
            FilterChips(state.filter, onFilterSelected)
            FacetFilters(state, onAccountFilterSelected, onCategoryFilterSelected, onSourceFilterSelected)
        }
        val content = state.content
        if (content is UiState.Success) {
            // The controls scroll away with the list so the days get the whole screen.
            DayList(content.data, padding, controls, onEditTransaction, onDelete)
            return@AppScreen
        }
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            controls()
            when (content) {
                UiState.Loading -> HistorySkeleton()
                UiState.Empty -> state.filter?.let { FilteredEmptyState(it, state.month) { onFilterSelected(null) } }
                    ?: EmptyState(state.month, onAddEntry)
                is UiState.Error -> EmptyView(
                    icon = Icons.Outlined.CloudOff,
                    title = stringResource(content.message),
                    actionLabel = stringResource(R.string.retry),
                    actionIcon = Icons.Outlined.Refresh,
                    onAction = onRetry,
                )
                is UiState.Success -> Unit
            }
        }
    }
}

@Composable
private fun SearchField(search: String, onSearchChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = search,
        onValueChange = onSearchChange,
        placeholder = { Text(stringResource(R.string.history_search_placeholder)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        singleLine = true,
        shape = CircleShape,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.card,
            focusedContainerColor = MaterialTheme.colorScheme.card,
            unfocusedBorderColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.sm),
    )
}
@Composable
private fun FacetFilters(
    state: HistoryUiState,
    onAccountFilterSelected: (String?) -> Unit,
    onCategoryFilterSelected: (String?) -> Unit,
    onSourceFilterSelected: (IncomeSource?) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        state.accounts.forEach { (id, name) ->
            FilterChip(state.accountFilter == id, { onAccountFilterSelected(if (state.accountFilter == id) null else id) }, { Text(name) })
        }
        state.categories.forEach { (id, name) ->
            FilterChip(state.categoryFilter == id, { onCategoryFilterSelected(if (state.categoryFilter == id) null else id) }, { Text(name) })
        }
        IncomeSource.entries.forEach { source ->
            FilterChip(
                state.sourceFilter == source,
                { onSourceFilterSelected(if (state.sourceFilter == source) null else source) },
                { Text(stringResource(source.labelRes())) },
            )
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
private fun DayList(
    days: List<DayUi>,
    padding: PaddingValues,
    controls: @Composable () -> Unit,
    onEditTransaction: (id: String) -> Unit,
    onDelete: (id: String) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp),
    ) {
        item(key = "controls") { controls() }
        days.forEach { day ->
            item(key = "day-${day.key}") { Box(Modifier.animateItem()) { DayHeader(day) } }
            items(day.rows, key = { it.id }) { row -> Box(Modifier.animateItem()) { TransactionRow(row, onEditTransaction, onDelete) } }
        }
    }
}

/** Two days of rows, laid out like [DayHeader] and [TransactionRow]. */
@Composable
private fun HistorySkeleton() {
    Skeleton(stringResource(R.string.history_loading), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        repeat(2) { day ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SkeletonLine(0.25f, 12.dp)
                Box(Modifier.weight(1f))
                SkeletonPill(72.dp)
            }
            repeat(if (day == 0) 3 else 2) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonLine(0.5f, 14.dp)
                        SkeletonLine(0.3f, 10.dp)
                    }
                    SkeletonPill(84.dp, 14.dp)
                }
            }
        }
    }
}

@Composable
private fun DayHeader(day: DayUi) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(day.label.resolve(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(MoneyFormatter.formatLkr(day.netMinor), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val haptics = rememberHaptics()
    val title = when (row.type) {
        TransactionType.EXPENSE -> row.categoryName ?: stringResource(R.string.dashboard_unknown_category)
        TransactionType.INCOME -> stringResource(row.incomeSource.labelRes())
        // A distinct style for transfers, per FR-100/FR-61: neither income nor expense.
        TransactionType.TRANSFER -> stringResource(R.string.quick_add_type_transfer)
    }
    val subtitle = if (row.type == TransactionType.TRANSFER) "${row.accountName} → ${row.toAccountName}" else row.accountName
    val amountColor = when (row.type) {
        TransactionType.INCOME -> MaterialTheme.colorScheme.positive
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurfaceVariant
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onEdit(row.id) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Decorative only: the title already names the type (#106 AC 2).
        when (row.type) {
            TransactionType.EXPENSE -> IconBadge(
                Icons.Outlined.NorthEast,
                container = MaterialTheme.colorScheme.surfaceContainerHigh,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TransactionType.INCOME -> IconBadge(
                Icons.Outlined.SouthWest,
                container = MaterialTheme.colorScheme.primaryContainer,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            TransactionType.TRANSFER -> IconBadge(
                Icons.Default.SwapHoriz,
                container = MaterialTheme.colorScheme.tertiaryContainer,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Weighted so a long category name shortens instead of squeezing the chip out of the row.
                Text(
                    title,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                // Only when the amount is still an estimate; tapping the row opens the sheet to confirm it (#177).
                if (row.isEstimated) EstimatedChip()
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(MoneyFormatter.formatLkr(row.amountMinor), color = amountColor, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { haptics.tick(); onDelete(row.id) }) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(R.string.history_delete),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun EmptyState(month: YearMonth, onAddEntry: () -> Unit) {
    val monthLabel = remember(month) { month.format(DateTimeFormatter.ofPattern("MMMM")) }
    // Neutral wording only: no streaks, warnings or "incomplete" language (A-03, A-22, FR-106).
    EmptyView(
        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
        title = stringResource(R.string.history_empty_title, monthLabel),
        illustration = R.drawable.ill_empty_transactions,
        body = stringResource(R.string.history_empty_body),
        actionLabel = stringResource(R.string.history_add_entry),
        actionIcon = Icons.Default.Add,
        onAction = onAddEntry,
    )
}

/** The month has entries, just none of the filtered type: say so, and offer the way back rather than "add". */
@Composable
private fun FilteredEmptyState(filter: TransactionType, month: YearMonth, onShowAll: () -> Unit) {
    val monthLabel = remember(month) { month.format(DateTimeFormatter.ofPattern("MMMM")) }
    val title = when (filter) {
        TransactionType.EXPENSE -> R.string.history_filtered_empty_expense
        TransactionType.INCOME -> R.string.history_filtered_empty_income
        TransactionType.TRANSFER -> R.string.history_filtered_empty_transfer
    }
    EmptyView(
        icon = Icons.Outlined.FilterAltOff,
        title = stringResource(title, monthLabel),
        body = stringResource(R.string.history_filtered_empty_body),
        actionLabel = stringResource(R.string.history_filter_show_all),
        onAction = onShowAll,
    )
}

@StringRes
private fun IncomeSource?.labelRes(): Int = when (this) {
    IncomeSource.SALARY -> R.string.income_source_salary
    IncomeSource.FREELANCE -> R.string.income_source_freelance
    IncomeSource.ADSENSE -> R.string.income_source_adsense
    IncomeSource.CRYPTO -> R.string.income_source_crypto
    IncomeSource.OTHER, null -> R.string.income_source_other
}
