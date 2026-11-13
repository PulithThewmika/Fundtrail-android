package com.fundtrail.ui.recurring

import com.fundtrail.ui.common.HeroAmount
import com.fundtrail.ui.common.HeroSurface
import com.fundtrail.ui.common.onHero
import com.fundtrail.ui.common.FormSheet
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.ui.common.AppCard
import com.fundtrail.ui.common.AppScreen
import com.fundtrail.ui.common.DateFormatter
import com.fundtrail.ui.common.MoneyFormatter
import com.fundtrail.ui.common.EmptyView
import androidx.compose.material.icons.outlined.EventRepeat

@Composable
fun RecurringRoute(onBack: () -> Unit, viewModel: RecurringViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecurringScreen(
        state = state,
        onBack = onBack,
        onAddClick = viewModel::onAddClick,
        onEditClick = viewModel::onEditClick,
        onPauseToggle = viewModel::onPauseToggle,
        onDelete = viewModel::onDelete,
        onNameChange = viewModel::onNameChange,
        onCategoryChange = viewModel::onCategoryChange,
        onAccountChange = viewModel::onAccountChange,
        onAmountChange = viewModel::onAmountChange,
        onVariableChange = viewModel::onVariableChange,
        onCadenceChange = viewModel::onCadenceChange,
        onDayOfMonthChange = viewModel::onDayOfMonthChange,
        onSave = viewModel::onSave,
        onFormDismiss = viewModel::onFormDismiss,
        onActionErrorShown = viewModel::onActionErrorShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    state: RecurringUiState,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onPauseToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAccountChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onVariableChange: (Boolean) -> Unit,
    onCadenceChange: (com.fundtrail.domain.model.RecurringCadence) -> Unit,
    onDayOfMonthChange: (String) -> Unit,
    onSave: () -> Unit,
    onFormDismiss: () -> Unit,
    onActionErrorShown: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val actionError = state.actionError?.let { stringResource(it) }
    LaunchedEffect(actionError) {
        if (actionError != null) {
            snackbarHostState.showSnackbar(actionError)
            onActionErrorShown()
        }
    }
    AppScreen(
        title = stringResource(R.string.recurring_title),
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.recurring_add))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                HeroSurface {
                    Text(
                        stringResource(R.string.recurring_monthly_total),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onHero.copy(alpha = 0.8f),
                    )
                    HeroAmount(state.monthlyTotalMinor, color = MaterialTheme.colorScheme.onHero)
                }
            }
            if (state.rows.isEmpty()) {
                item {
                    EmptyView(
                        icon = Icons.Outlined.EventRepeat,
                        title = stringResource(R.string.recurring_empty_title),
                        illustration = R.drawable.ill_empty_recurring,
                        body = stringResource(R.string.recurring_empty),
                        actionLabel = stringResource(R.string.recurring_add),
                        actionIcon = Icons.Default.Add,
                        onAction = onAddClick,
                    )
                }
            } else {
                items(state.rows, key = { it.id }) { row ->
                    RecurringRow(row, onEditClick, onPauseToggle, onDelete)
                }
            }
        }
    }

    state.form?.let { form ->
        RecurringFormDialog(
            state = state,
            form = form,
            onNameChange = onNameChange,
            onCategoryChange = onCategoryChange,
            onAccountChange = onAccountChange,
            onAmountChange = onAmountChange,
            onVariableChange = onVariableChange,
            onCadenceChange = onCadenceChange,
            onDayOfMonthChange = onDayOfMonthChange,
            onSave = onSave,
            onDismiss = onFormDismiss,
        )
    }
}

@Composable
private fun RecurringRow(
    row: RecurringRowUi,
    onEdit: (String) -> Unit,
    onPauseToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    AppCard(spacing = 4.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(row.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (row.isPaused) {
                Text(
                    stringResource(R.string.recurring_paused_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            stringResource(
                R.string.recurring_row_due,
                MoneyFormatter.formatLkr(row.expectedMinor),
                DateFormatter.format(row.nextDueDate),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onEdit(row.id) }) { Text(stringResource(R.string.recurring_edit)) }
            TextButton(onClick = { onPauseToggle(row.id) }) {
                Text(stringResource(if (row.isPaused) R.string.recurring_resume else R.string.recurring_pause))
            }
            TextButton(onClick = { onDelete(row.id) }) { Text(stringResource(R.string.recurring_delete)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringFormDialog(
    state: RecurringUiState,
    form: RecurringFormState,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAccountChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onVariableChange: (Boolean) -> Unit,
    onCadenceChange: (com.fundtrail.domain.model.RecurringCadence) -> Unit,
    onDayOfMonthChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    FormSheet(
        onDismissRequest = onDismiss,
        title = stringResource(if (form.editingId == null) R.string.recurring_add_title else R.string.recurring_edit_title),
        confirmLabel = stringResource(R.string.recurring_save),
        onConfirm = onSave,
        confirmEnabled = form.canSave,
        dismissLabel = stringResource(R.string.category_cancel),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.recurring_field_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                PickerField(
                    label = stringResource(R.string.recurring_field_category),
                    selected = state.categories.firstOrNull { it.id == form.categoryId }?.name.orEmpty(),
                    options = state.categories.map { it.id to it.name },
                    onSelected = onCategoryChange,
                )
                PickerField(
                    label = stringResource(R.string.recurring_field_account),
                    selected = state.accounts.firstOrNull { it.id == form.accountId }?.name.orEmpty(),
                    options = state.accounts.map { it.id to it.name },
                    onSelected = onAccountChange,
                )
                OutlinedTextField(
                    value = form.amountText,
                    onValueChange = onAmountChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.recurring_field_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.recurring_field_variable), modifier = Modifier.weight(1f))
                    Switch(checked = form.isVariableAmount, onCheckedChange = onVariableChange)
                }
                Text(stringResource(R.string.recurring_field_cadence), style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val cadences = com.fundtrail.domain.model.RecurringCadence.entries
                    cadences.forEachIndexed { index, cadence ->
                        SegmentedButton(
                            selected = form.cadence == cadence,
                            onClick = { onCadenceChange(cadence) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = cadences.size),
                        ) { Text(stringResource(cadence.labelRes())) }
                    }
                }
                val dayRange = if (form.cadence == com.fundtrail.domain.model.RecurringCadence.WEEKLY) 1..7 else 1..28
                OutlinedTextField(
                    value = form.dayOfMonthText,
                    onValueChange = onDayOfMonthChange,
                    singleLine = true,
                    isError = form.dayOfMonth?.let { it !in dayRange } ?: form.dayOfMonthText.isNotEmpty(),
                    label = {
                        Text(
                            stringResource(
                                if (form.cadence == com.fundtrail.domain.model.RecurringCadence.WEEKLY) R.string.recurring_field_day_of_week
                                else R.string.recurring_field_day,
                            ),
                        )
                    },
                    supportingText = {
                        Text(
                            stringResource(
                                if (form.cadence == com.fundtrail.domain.model.RecurringCadence.WEEKLY) R.string.recurring_field_day_of_week_hint
                                else R.string.recurring_field_day_hint,
                            ),
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                form.error?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerField(label: String, selected: String, options: List<Pair<String, String>>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelected(id); expanded = false })
            }
        }
    }
}

private fun com.fundtrail.domain.model.RecurringCadence.labelRes(): Int = when (this) {
    com.fundtrail.domain.model.RecurringCadence.WEEKLY -> R.string.recurring_cadence_weekly
    com.fundtrail.domain.model.RecurringCadence.MONTHLY -> R.string.recurring_cadence_monthly
    com.fundtrail.domain.model.RecurringCadence.YEARLY -> R.string.recurring_cadence_yearly
}
