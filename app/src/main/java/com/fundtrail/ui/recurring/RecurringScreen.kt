package com.fundtrail.ui.recurring

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.fundtrail.ui.common.MoneyFormatter

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
        onDayOfMonthChange = viewModel::onDayOfMonthChange,
        onSave = viewModel::onSave,
        onFormDismiss = viewModel::onFormDismiss,
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
    onDayOfMonthChange: (String) -> Unit,
    onSave: () -> Unit,
    onFormDismiss: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recurring_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.recurring_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.recurring_add))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.recurring_monthly_total),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(MoneyFormatter.formatLkr(state.monthlyTotalMinor), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
            if (state.rows.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.recurring_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
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
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                stringResource(R.string.recurring_row_due, MoneyFormatter.formatLkr(row.expectedMinor), row.dayOfMonth),
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
    onDayOfMonthChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onSave, enabled = form.canSave) { Text(stringResource(R.string.recurring_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.category_cancel)) } },
        title = { Text(stringResource(if (form.editingId == null) R.string.recurring_add_title else R.string.recurring_edit_title)) },
        text = {
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
                OutlinedTextField(
                    value = form.dayOfMonthText,
                    onValueChange = onDayOfMonthChange,
                    singleLine = true,
                    isError = form.dayOfMonth?.let { it !in 1..28 } ?: form.dayOfMonthText.isNotEmpty(),
                    label = { Text(stringResource(R.string.recurring_field_day)) },
                    supportingText = { Text(stringResource(R.string.recurring_field_day_hint)) },
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
