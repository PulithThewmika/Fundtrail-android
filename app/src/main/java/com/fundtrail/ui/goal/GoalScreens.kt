package com.fundtrail.ui.goal

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.fundtrail.ui.common.CountingHeroAmount
import com.fundtrail.ui.common.Motion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.fundtrail.ui.common.HeroPill
import com.fundtrail.ui.common.HeroSurface
import com.fundtrail.ui.common.IconBadge
import com.fundtrail.ui.common.ProgressRing
import com.fundtrail.ui.common.StatTile
import com.fundtrail.ui.common.negative
import com.fundtrail.ui.common.onHero
import com.fundtrail.ui.common.positive
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow
import com.fundtrail.ui.common.AppCard
import com.fundtrail.ui.common.AppScreen
import com.fundtrail.ui.common.HeroAmount
import com.fundtrail.ui.common.PrimaryButton
import com.fundtrail.ui.common.SectionTitle
import com.fundtrail.ui.common.StatusChip
import com.fundtrail.ui.common.UiStateContent
import com.fundtrail.ui.theme.Spacing
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.GoalStatus
import com.fundtrail.ui.common.DateFormatter
import com.fundtrail.ui.common.Skeleton
import com.fundtrail.ui.common.SkeletonCard
import com.fundtrail.ui.common.SkeletonCircle
import com.fundtrail.ui.common.SkeletonLine
import com.fundtrail.ui.common.MoneyFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.math.BigDecimal
import java.math.RoundingMode
import com.fundtrail.ui.common.EmptyView
import androidx.compose.material.icons.outlined.Flag

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
    AppScreen(title = stringResource(R.string.goal_setup_title), onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
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
            AppCard {
                SectionTitle(stringResource(R.string.goal_setup_preview))
                Text(
                    stringResource(R.string.goal_setup_required, MoneyFormatter.formatLkr(state.requiredMonthlyMinor)),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            state.errorMessage?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            PrimaryButton(stringResource(R.string.goal_setup_save), onClick = onSave, loading = state.isSaving)
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.goal_setup_cancel)) }
        }
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
    AppScreen(title = stringResource(R.string.goal_detail_title), onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            UiStateContent(
                state = state.content,
                onRetry = onRetry,
                loading = { GoalDetailSkeleton() },
                empty = {
                    EmptyView(
                        icon = Icons.Outlined.Flag,
                        title = stringResource(R.string.goal_detail_empty_title),
                        illustration = R.drawable.ill_empty_goal,
                        body = stringResource(R.string.goal_detail_empty),
                        actionLabel = stringResource(R.string.dashboard_goal_set),
                        onAction = onSetGoal,
                    )
                },
            ) { data ->
                DetailContent(data, state.actionError, onWhatIfChange, onResetWhatIf, onMoveDeadline, onLowerTarget)
            }
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
    // A goal already covered has nothing left to pace: say so plainly instead of "not enough data".
    val reached = data.progress.remainingMinor <= 0L
    val onHero = MaterialTheme.colorScheme.onHero
    HeroSurface(spacing = Spacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            ProgressRing(
                fraction = data.progressFraction,
                centerText = "${data.percent}%",
                description = stringResource(R.string.goal_detail_progress, data.percent),
                size = 96.dp,
                strokeWidth = 10.dp,
                color = onHero,
                trackColor = onHero.copy(alpha = 0.2f),
                textColor = onHero,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(data.goal.name, style = MaterialTheme.typography.titleLarge, color = onHero, maxLines = 2, overflow = TextOverflow.Ellipsis)
                GoalStatusChip(data, reached)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CountingHeroAmount(data.progress.savedToDateMinor, color = onHero)
            Text(
                stringResource(
                    R.string.goal_detail_saved,
                    MoneyFormatter.formatLkr(data.progress.savedToDateMinor),
                    MoneyFormatter.formatLkr(data.goal.targetLkrMinor),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = onHero.copy(alpha = 0.8f),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            HeroPill(stringResource(R.string.goal_detail_remaining, MoneyFormatter.formatLkr(data.progress.remainingMinor)), icon = Icons.Outlined.Flag)
            HeroPill(stringResource(R.string.goal_detail_deadline, DateFormatter.format(data.goal.deadline)), icon = Icons.Default.DateRange)
        }
    }
    if (reached) return
    val pace = data.progress.actualPaceMinor
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        StatTile(
            stringResource(R.string.goal_detail_required),
            MoneyFormatter.formatLkr(data.progress.requiredMonthlyMinor),
            Icons.Outlined.Flag,
            Modifier.weight(1f).fillMaxHeight(),
        )
        StatTile(
            stringResource(R.string.goal_detail_actual),
            pace?.let(MoneyFormatter::formatLkr) ?: stringResource(R.string.goal_detail_no_data),
            Icons.Outlined.Speed,
            Modifier.weight(1f).fillMaxHeight(),
            // Green when the pace covers what the goal needs each month, red when it falls short.
            valueColor = when {
                pace == null -> Color.Unspecified
                pace >= data.progress.requiredMonthlyMinor -> MaterialTheme.colorScheme.positive
                else -> MaterialTheme.colorScheme.negative
            },
        )
    }
    if (data.progress.isInfeasible) {
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                IconBadge(
                    Icons.Outlined.ErrorOutline,
                    container = MaterialTheme.colorScheme.errorContainer,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(stringResource(R.string.goal_detail_infeasible), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
            data.progress.suggestedDeadline?.let {
                OutlinedButton(onClick = onMoveDeadline, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.goal_detail_move_deadline, DateFormatter.format(it)))
                }
            }
            data.progress.suggestedTargetMinor?.let {
                OutlinedButton(onClick = onLowerTarget, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.goal_detail_lower_target, MoneyFormatter.formatLkr(it)))
                }
            }
        }
    }
    AppCard {
        SectionTitle(stringResource(R.string.goal_detail_what_if))
        Text(MoneyFormatter.formatLkr(data.whatIfMinor), style = MaterialTheme.typography.headlineSmall)
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
        )
        val projected = stringResource(R.string.goal_detail_projected, data.whatIfDate?.let(DateFormatter::format) ?: stringResource(R.string.dashboard_goal_no_projection))
        Crossfade(
            targetState = projected,
            animationSpec = tween(Motion.FAST),
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            label = "projected",
        ) { text ->
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(Spacing.md))
        }
        TextButton(onClick = onResetWhatIf, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.goal_detail_reset)) }
    }
    actionError?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
}

/** Green when reached, ahead or on track; red when behind or the goal can't be met as set; neutral otherwise. */
@Composable
private fun GoalStatusChip(data: GoalDetailData, reached: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val good = reached || data.progress.status == GoalStatus.AHEAD || data.progress.status == GoalStatus.ON_TRACK
    val bad = !reached && (data.progress.status == GoalStatus.BEHIND || data.progress.isInfeasible)
    StatusChip(
        if (reached) stringResource(R.string.goal_detail_reached) else goalStatusText(data),
        container = when {
            bad -> scheme.errorContainer
            good -> scheme.primaryContainer
            else -> scheme.surfaceContainerHigh
        },
        content = when {
            bad -> scheme.onErrorContainer
            good -> scheme.onPrimaryContainer
            else -> scheme.onSurfaceVariant
        },
    )
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
private fun GoalDetailSkeleton() {
    Skeleton(stringResource(R.string.goal_detail_loading), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SkeletonLine(0.55f, 24.dp)
        SkeletonCircle(48.dp)
        SkeletonLine(0.7f, 14.dp)
        SkeletonLine(0.5f, 14.dp)
        SkeletonLine(0.6f, 14.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) {
                SkeletonCard(Modifier.weight(1f)) {
                    SkeletonLine(0.6f, 10.dp)
                    SkeletonLine(0.8f, 18.dp)
                }
            }
        }
        SkeletonLine(0.4f, 14.dp)
    }
}
