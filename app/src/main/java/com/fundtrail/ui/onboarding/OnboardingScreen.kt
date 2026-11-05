package com.fundtrail.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.ui.accounts.displayName

/** FR-08: at most three skippable steps (goal, accounts, income estimate); every path reaches the dashboard. */
@Composable
fun OnboardingRoute(onContinue: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onContinue() }
    OnboardingScreen(
        state = state,
        onGoalNameChange = viewModel::onGoalNameChange,
        onGoalTargetChange = viewModel::onGoalTargetChange,
        onGoalSavedChange = viewModel::onGoalSavedChange,
        onGoalNext = viewModel::onGoalNext,
        onGoalSkip = viewModel::onGoalSkip,
        onAccountNameChange = viewModel::onAccountNameChange,
        onAccountKindChange = viewModel::onAccountKindChange,
        onAccountNext = viewModel::onAccountNext,
        onAccountSkip = viewModel::onAccountSkip,
        onIncomeLowChange = viewModel::onIncomeLowChange,
        onIncomeHighChange = viewModel::onIncomeHighChange,
        onIncomeNext = viewModel::onIncomeNext,
        onIncomeSkip = viewModel::onIncomeSkip,
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onGoalNameChange: (String) -> Unit,
    onGoalTargetChange: (String) -> Unit,
    onGoalSavedChange: (String) -> Unit,
    onGoalNext: () -> Unit,
    onGoalSkip: () -> Unit,
    onAccountNameChange: (String) -> Unit,
    onAccountKindChange: (AccountKind) -> Unit,
    onAccountNext: () -> Unit,
    onAccountSkip: () -> Unit,
    onIncomeLowChange: (String) -> Unit,
    onIncomeHighChange: (String) -> Unit,
    onIncomeNext: () -> Unit,
    onIncomeSkip: () -> Unit,
) {
    val stepNumber = OnboardingStep.entries.indexOf(state.step) + 1
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.onboarding_step_of, stepNumber, OnboardingStep.entries.size))

        when (state.step) {
            OnboardingStep.GOAL -> GoalStep(state, onGoalNameChange, onGoalTargetChange, onGoalSavedChange, onGoalNext, onGoalSkip)
            OnboardingStep.ACCOUNT -> AccountStep(state, onAccountNameChange, onAccountKindChange, onAccountNext, onAccountSkip)
            OnboardingStep.INCOME -> IncomeStep(state, onIncomeLowChange, onIncomeHighChange, onIncomeNext, onIncomeSkip)
        }
    }
}

@Composable
private fun GoalStep(
    state: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onSavedChange: (String) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Text(stringResource(R.string.onboarding_goal_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.onboarding_goal_subtitle))
    OutlinedTextField(
        value = state.goalName, onValueChange = onNameChange,
        label = { Text(stringResource(R.string.goal_setup_name)) }, singleLine = true,
        enabled = !state.isSaving, modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.goalTarget, onValueChange = onTargetChange,
        label = { Text(stringResource(R.string.goal_setup_target)) }, singleLine = true,
        enabled = !state.isSaving, modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.goalSaved, onValueChange = onSavedChange,
        label = { Text(stringResource(R.string.goal_setup_saved)) }, singleLine = true,
        enabled = !state.isSaving, modifier = Modifier.fillMaxWidth(),
    )
    state.goalError?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    StepActions(isSaving = state.isSaving, nextLabel = R.string.onboarding_next, onNext = onNext, onSkip = onSkip)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountStep(
    state: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onKindChange: (AccountKind) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    var kindExpanded by remember { mutableStateOf(false) }
    Text(stringResource(R.string.onboarding_account_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.onboarding_account_subtitle))
    OutlinedTextField(
        value = state.accountName, onValueChange = onNameChange,
        label = { Text(stringResource(R.string.accounts_name_label)) }, singleLine = true,
        enabled = !state.isSaving, modifier = Modifier.fillMaxWidth(),
    )
    ExposedDropdownMenuBox(expanded = kindExpanded, onExpandedChange = { if (!state.isSaving) kindExpanded = it }) {
        OutlinedTextField(
            value = state.accountKind.displayName(), onValueChange = {}, readOnly = true,
            label = { Text(stringResource(R.string.accounts_kind_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindExpanded) },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = kindExpanded, onDismissRequest = { kindExpanded = false }) {
            AccountKind.entries.forEach { option ->
                DropdownMenuItem(text = { Text(option.displayName()) }, onClick = { onKindChange(option); kindExpanded = false })
            }
        }
    }
    state.accountError?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    StepActions(isSaving = state.isSaving, nextLabel = R.string.onboarding_next, onNext = onNext, onSkip = onSkip)
}

@Composable
private fun IncomeStep(
    state: OnboardingUiState,
    onLowChange: (String) -> Unit,
    onHighChange: (String) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Text(stringResource(R.string.onboarding_income_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.onboarding_income_subtitle))
    OutlinedTextField(
        value = state.incomeLow, onValueChange = onLowChange,
        label = { Text(stringResource(R.string.income_estimate_low_label)) }, singleLine = true,
        enabled = !state.isSaving, modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.incomeHigh, onValueChange = onHighChange,
        label = { Text(stringResource(R.string.income_estimate_high_label)) }, singleLine = true,
        enabled = !state.isSaving, modifier = Modifier.fillMaxWidth(),
    )
    state.incomeError?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    StepActions(isSaving = state.isSaving, nextLabel = R.string.onboarding_finish, onNext = onNext, onSkip = onSkip)
}

@Composable
private fun StepActions(isSaving: Boolean, nextLabel: Int, onNext: () -> Unit, onSkip: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onSkip, enabled = !isSaving) { Text(stringResource(R.string.onboarding_skip)) }
        Button(onClick = onNext, enabled = !isSaving) { Text(stringResource(nextLabel)) }
    }
}
