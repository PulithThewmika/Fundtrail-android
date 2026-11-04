package com.fundtrail.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        onSave = viewModel::onSave,
        onDismiss = handleDismiss,
    )
}

@Composable
fun IncomeSettingsDialogContent(
    state: IncomeSettingsUiState,
    onLowEstimateChanged: (String) -> Unit,
    onHighEstimateChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.income_estimate_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text(stringResource(R.string.income_estimate_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.income_estimate_cancel))
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
