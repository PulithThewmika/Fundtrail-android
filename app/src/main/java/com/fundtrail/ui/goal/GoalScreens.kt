package com.fundtrail.ui.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.GoalStatus
import com.fundtrail.ui.common.DateFormatter
import com.fundtrail.ui.common.MoneyFormatter
import com.fundtrail.ui.common.UiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun GoalSetupRoute(onBack: () -> Unit, viewModel: GoalSetupViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    GoalSetupScreen(state, onBack, viewModel::onNameChange, viewModel::onTargetChange, viewModel::onAlreadySavedChange, viewModel::onDeadlineChange, viewModel::save)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSetupScreen(
    state: GoalSetupUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onAlreadySavedChange: (String) -> Unit,
    onDeadlineChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Header(R.string.goal_setup_title, onBack)
        OutlinedTextField(
            state.name,
            onNameChange,
            Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.goal_setup_name)) },
            placeholder = { Text(stringResource(R.string.goal_setup_name_hint)) },
            singleLine = true,
        )
        OutlinedTextField(
            state.target,
            onTargetChange,
            Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.goal_setup_target)) },
            placeholder = { Text(stringResource(R.string.goal_setup_target_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        OutlinedTextField(
            DateFormatter.format(state.deadline),
            {},
            Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.goal_setup_deadline)) },
            readOnly = true,
            trailingIcon = {
                IconButton({ showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, stringResource(R.string.goal_setup_deadline))
                }
            },
        )
        OutlinedTextField(
            state.alreadySaved,
            onAlreadySavedChange,
            Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.goal_setup_saved)) },
            placeholder = { Text(stringResource(R.string.goal_setup_saved_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.goal_setup_preview), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.goal_setup_required, MoneyFormatter.formatLkr(state.requiredMonthlyMinor)))
            }
        }
        state.errorMessage?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            if (state.isSaving) CircularProgressIndicator(strokeWidth = 2.dp) else Text(stringResource(R.string.goal_setup_save))
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.goal_setup_cancel)) }
    }
    if (showDatePicker) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = state.deadline.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(LocalDate.now(APP_ZONE))
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onDeadlineChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.quick_add_date_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.goal_setup_cancel)) } },
        ) { DatePicker(state = picker) }
    }
}

@Composable
fun GoalDetailRoute(onBack: (() -> Unit)?, onSetGoal: () -> Unit, viewModel: GoalDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GoalDetailScreen(state, onBack, onSetGoal, viewModel::onRetry, viewModel::onWhatIfChange, viewModel::resetWhatIf, viewModel::moveDeadline, viewModel::lowerTarget)
}

@Composable
fun GoalDetailScreen(
    state: GoalDetailUiState,
    onBack: (() -> Unit)?,
    onSetGoal: () -> Unit,
    onRetry: () -> Unit,
    onWhatIfChange: (Long) -> Unit,
    onResetWhatIf: () -> Unit,
    onMoveDeadline: () -> Unit,
    onLowerTarget: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Header(R.string.goal_detail_title, onBack)
        when (val content = state.content) {
            UiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            UiState.Empty -> {
                Text(stringResource(R.string.goal_detail_empty))
                Button(onClick = onSetGoal) { Text(stringResource(R.string.dashboard_goal_set)) }
            }
            is UiState.Error -> {
                Text(stringResource(content.message), color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) { Text(stringResource(R.string.goal_detail_retry)) }
            }
            is UiState.Success -> DetailContent(content.data, state.actionError, onWhatIfChange, onResetWhatIf, onMoveDeadline, onLowerTarget)
        }
    }
}

@Composable
private fun DetailContent(
    data: GoalDetailData,
    actionError: Int?,
    onWhatIfChange: (Long) -> Unit,
    onResetWhatIf: () -> Unit,
    onMoveDeadline: () -> Unit,
    onLowerTarget: () -> Unit,
) {
    val progressDescription = stringResource(R.string.goal_detail_progress, data.percent)
    Text(data.goal.name, style = MaterialTheme.typography.headlineSmall)
    CircularProgressIndicator(
        progress = { data.progressFraction },
        Modifier.semantics { contentDescription = progressDescription },
        strokeWidth = 8.dp,
    )
    Text(stringResource(R.string.goal_detail_saved, MoneyFormatter.formatLkr(data.progress.savedToDateMinor), MoneyFormatter.formatLkr(data.goal.targetLkrMinor)))
    Text(stringResource(R.string.goal_detail_remaining, MoneyFormatter.formatLkr(data.progress.remainingMinor)))
    Text(stringResource(R.string.goal_detail_deadline, DateFormatter.format(data.goal.deadline)))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard(R.string.goal_detail_required, data.progress.requiredMonthlyMinor)
        MetricCard(R.string.goal_detail_actual, data.progress.actualPaceMinor)
    }
    Text(goalStatusText(data), style = MaterialTheme.typography.labelLarge)
    Text(stringResource(R.string.goal_detail_projected, data.whatIfDate?.let(DateFormatter::format) ?: stringResource(R.string.dashboard_goal_no_projection)))
    if (data.progress.isInfeasible) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.goal_detail_infeasible))
                data.progress.suggestedDeadline?.let { OutlinedButton(onClick = onMoveDeadline) { Text(stringResource(R.string.goal_detail_move_deadline, DateFormatter.format(it))) } }
                data.progress.suggestedTargetMinor?.let { OutlinedButton(onClick = onLowerTarget) { Text(stringResource(R.string.goal_detail_lower_target, MoneyFormatter.formatLkr(it))) } }
            }
        }
    }
    Text(stringResource(R.string.goal_detail_what_if), style = MaterialTheme.typography.titleMedium)
    Text(MoneyFormatter.formatLkr(data.whatIfMinor))
    Slider(
        value = if (data.whatIfMaxMinor > 0L) {
            (data.whatIfMinor.toDouble() / data.whatIfMaxMinor.toDouble()).toFloat()
        } else {
            0f
        },
        onValueChange = {
            onWhatIfChange(
                BigDecimal(it.toString())
                    .multiply(BigDecimal.valueOf(data.whatIfMaxMinor))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact()
            )
        },
        valueRange = 0f..1f,
        steps = 100,
    )
    TextButton(onClick = onResetWhatIf) { Text(stringResource(R.string.goal_detail_reset)) }
    actionError?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun RowScope.MetricCard(label: Int, amount: Long?) {
    Card(Modifier.weight(1f)) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
            Text(amount?.let(MoneyFormatter::formatLkr) ?: stringResource(R.string.goal_detail_no_data))
        }
    }
}

@Composable
private fun goalStatusText(data: GoalDetailData): String = when (data.progress.status) {
    GoalStatus.AHEAD -> stringResource(
        R.string.goal_status_ahead,
        MoneyFormatter.formatLkr((data.progress.actualPaceMinor ?: 0L) - data.progress.requiredMonthlyMinor),
    )
    GoalStatus.ON_TRACK -> stringResource(R.string.goal_status_on_track)
    GoalStatus.BEHIND -> stringResource(
        R.string.goal_status_behind,
        MoneyFormatter.formatLkr((data.progress.requiredMonthlyMinor - (data.progress.actualPaceMinor ?: 0L)).coerceAtLeast(0L)),
    )
    GoalStatus.NOT_ENOUGH_DATA -> stringResource(R.string.goal_status_not_enough_data)
}

@Composable
private fun Header(title: Int, onBack: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        onBack?.let { back ->
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.goal_back)) }
        }
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
    }
}
