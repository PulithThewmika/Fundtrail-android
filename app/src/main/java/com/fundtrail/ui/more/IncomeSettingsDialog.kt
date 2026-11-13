package com.fundtrail.ui.more

import com.fundtrail.ui.common.SectionTitle
import com.fundtrail.ui.common.FormSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R

@Composable
fun IncomeSettingsDialog(
    onDismiss: () -> Unit,
    viewModel: IncomeSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            viewModel.onHandledSaved()
            onDismiss()
        }
    }

    val handleDismiss = {
        viewModel.onDismissed()
        onDismiss()
    }

    IncomeSettingsDialogContent(
        state = state,
        onLowEstimateChanged = viewModel::onLowEstimateChanged,
        onHighEstimateChanged = viewModel::onHighEstimateChanged,
        onSalaryChanged = viewModel::onSalaryChanged,
        onPaydayChanged = viewModel::onPaydayChanged,
        onSave = viewModel::onSave,
        onDismiss = handleDismiss,
    )
}

@Composable
fun IncomeSettingsDialogContent(
    state: IncomeSettingsUiState,
    onLowEstimateChanged: (String) -> Unit,
    onHighEstimateChanged: (String) -> Unit,
    onSalaryChanged: (String) -> Unit = {},
    onPaydayChanged: (String) -> Unit = {},
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    FormSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.income_estimate_dialog_title),
        confirmLabel = stringResource(R.string.income_estimate_save),
        onConfirm = onSave,
        loading = state.isLoading,
        dismissLabel = stringResource(R.string.income_estimate_cancel),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.income_settings_salary_section))
                OutlinedTextField(
                    value = state.salaryInput,
                    onValueChange = onSalaryChanged,
                    label = { Text(stringResource(R.string.income_settings_salary_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.paydayInput,
                    onValueChange = onPaydayChanged,
                    label = { Text(stringResource(R.string.income_settings_payday_label)) },
                    supportingText = { Text(stringResource(R.string.income_settings_payday_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SectionTitle(stringResource(R.string.income_settings_estimate_section))
                Text(
                    text = stringResource(R.string.income_estimate_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.lowEstimateInput,
                    onValueChange = onLowEstimateChanged,
                    label = { Text(stringResource(R.string.income_estimate_low_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.highEstimateInput,
                    onValueChange = onHighEstimateChanged,
                    label = { Text(stringResource(R.string.income_estimate_high_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.errorResId?.let { errorResId ->
                    Text(
                        text = stringResource(errorResId),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
    )
}

@Preview
@Composable
private fun IncomeSettingsDialogContentPreview() {
    IncomeSettingsDialogContent(
        state = IncomeSettingsUiState(
            lowEstimateInput = "160000",
            highEstimateInput = "210000",
        ),
        onLowEstimateChanged = {},
        onHighEstimateChanged = {},
        onSave = {},
        onDismiss = {},
    )
}
