package com.fundtrail.ui.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.ui.categories.CategoryIcons
import com.fundtrail.ui.common.DateFormatter
import com.fundtrail.ui.common.KindPicker
import com.fundtrail.ui.common.SectionLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * SCR-20 quick-add sheet, shown over the Insights dashboard. [onSaved] gets an `undo` action for the
 * snackbar that the host shows after the sheet has closed. The action takes a callback that receives the
 * string resource of the error the host must show if the undo fails.
 */
@Composable
fun QuickAddRoute(
    onDismiss: () -> Unit,
    onSaved: (undo: (onFailed: (messageRes: Int) -> Unit) -> Unit) -> Unit,
    viewModel: QuickAddViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedTransactionId) {
        val id = state.savedTransactionId ?: return@LaunchedEffect
        onSaved { onFailed -> viewModel.undo(id, onFailed) }
        viewModel.onSavedHandled()
        onDismiss()
    }
    // The ViewModel outlives the sheet, so clear the form on close or the next open would show the old draft.
    DisposableEffect(Unit) { onDispose { viewModel.reset() } }

    QuickAddSheet(
        state = state,
        onDismiss = onDismiss,
        onAmountChange = viewModel::onAmountChange,
        onCategorySelected = viewModel::onCategorySelected,
        onAccountSelected = viewModel::onAccountSelected,
        onDateChange = viewModel::onDateChange,
        onNoteChange = viewModel::onNoteChange,
        onSpendKindSelected = viewModel::onSpendKindSelected,
        onToggleMoreOptions = viewModel::onToggleMoreOptions,
        onSave = viewModel::onSave,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    state: QuickAddUiState,
    onDismiss: () -> Unit,
    onAmountChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onAccountSelected: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onSpendKindSelected: (SpendKind) -> Unit,
    onToggleMoreOptions: () -> Unit,
    onSave: () -> Unit,
) {
    val amountFocus = remember { FocusRequester() }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.quick_add_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.quick_add_close))
                }
            }

            // Amount first and focused, so typing can start straight away (NFR-01).
            OutlinedTextField(
                value = state.amountText,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.quick_add_amount_label)) },
                prefix = { Text(stringResource(R.string.quick_add_currency)) },
                isError = state.amountError != null,
                supportingText = state.amountError?.let { { Text(stringResource(it)) } },
                singleLine = true,
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().focusRequester(amountFocus),
            )
            LaunchedEffect(Unit) { amountFocus.requestFocus() }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(R.string.quick_add_category)
                if (state.categories.isEmpty()) {
                    Text(
                        stringResource(R.string.quick_add_no_categories),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                        items(state.categories, key = { it.id }) { category ->
                            FilterChip(
                                selected = category.id == state.selectedCategoryId,
                                onClick = { onCategorySelected(category.id) },
                                enabled = !state.isSaving,
                                label = { Text(category.name) },
                                leadingIcon = {
                                    Icon(CategoryIcons.vectorFor(category.icon), contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                            )
                        }
                    }
                    if (state.categorySuggested) {
                        Text(
                            stringResource(R.string.quick_add_category_suggested),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(R.string.quick_add_account)
                if (state.accounts.isEmpty()) {
                    Text(
                        stringResource(R.string.quick_add_no_accounts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                        items(state.accounts, key = { it.id }) { account ->
                            FilterChip(
                                selected = account.id == state.selectedAccountId,
                                onClick = { onAccountSelected(account.id) },
                                enabled = !state.isSaving,
                                label = { Text(account.name) },
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(R.string.quick_add_date)
                AssistChip(
                    onClick = { showDatePicker = true },
                    enabled = !state.isSaving,
                    label = {
                        Text(
                            if (state.date == LocalDate.now(APP_ZONE)) stringResource(R.string.quick_add_today)
                            else DateFormatter.format(state.date),
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }

            TextButton(onClick = onToggleMoreOptions) {
                Text(stringResource(R.string.quick_add_more_options))
                Icon(
                    if (state.moreOptionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                )
            }
            if (state.moreOptionsExpanded) {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    label = { Text(stringResource(R.string.quick_add_note_label)) },
                    singleLine = true,
                    enabled = !state.isSaving,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(R.string.category_type_label)
                    KindPicker(
                        selected = state.spendKind ?: SpendKind.DISCRETIONARY,
                        enabled = !state.isSaving && state.spendKind != null,
                        onSelected = onSpendKindSelected,
                    )
                    Text(
                        stringResource(
                            if (state.spendKindOverridden) R.string.quick_add_type_overridden else R.string.quick_add_type_follows,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            state.errorMessage?.let {
                Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.quick_add_save))
            }
        }
    }

    if (showDatePicker) {
        QuickAddDatePicker(
            selected = state.date,
            onSelected = { onDateChange(it); showDatePicker = false },
            onDismiss = { showDatePicker = false },
        )
    }
}

/** Past days and today only: an expense cannot be in the future. The picker works in UTC days. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddDatePicker(selected: LocalDate, onSelected: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now(APP_ZONE)
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selected.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis == null) onDismiss()
                    else onSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                },
            ) { Text(stringResource(R.string.quick_add_date_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.category_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}
