package com.fundtrail.ui.dashboard

import com.fundtrail.ui.common.CountingHeroAmount
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SouthWest
import com.fundtrail.ui.common.HeroPill
import com.fundtrail.ui.common.HeroSurface
import com.fundtrail.ui.common.IconBadge
import com.fundtrail.ui.common.InsetStat
import com.fundtrail.ui.common.ProgressRing
import com.fundtrail.ui.common.StatTile
import com.fundtrail.ui.common.negative
import com.fundtrail.ui.common.positive
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.fundtrail.ui.more.IncomeSettingsDialog
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import androidx.compose.material3.IconButton
import com.fundtrail.domain.model.UserProfile
import com.fundtrail.ui.common.Avatar
import com.fundtrail.ui.common.ProfileViewModel
import com.fundtrail.ui.common.hero
import com.fundtrail.ui.common.onHero
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow
import com.fundtrail.ui.common.AppCard
import com.fundtrail.ui.common.AppScreen
import com.fundtrail.ui.common.EmptyView
import com.fundtrail.ui.common.HeroAmount
import com.fundtrail.ui.common.ListRow
import com.fundtrail.ui.common.MonthSwitcher
import com.fundtrail.ui.common.PrimaryButton
import com.fundtrail.ui.common.ProgressBar
import com.fundtrail.ui.common.SectionTitle
import com.fundtrail.ui.common.StatusChip
import com.fundtrail.ui.common.UiStateContent
import com.fundtrail.ui.theme.Spacing
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.SafeToSpend
import com.fundtrail.domain.model.SafeToSpendParts
import com.fundtrail.domain.model.SafeToSpendState
import com.fundtrail.domain.model.SalaryStatus
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.ui.common.DateFormatter
import com.fundtrail.ui.common.MoneyFormatter
import com.fundtrail.ui.common.Skeleton
import com.fundtrail.ui.common.SkeletonCard
import com.fundtrail.ui.common.SkeletonCircle
import com.fundtrail.ui.common.SkeletonLabelValue
import com.fundtrail.ui.common.SkeletonLine
import com.fundtrail.ui.common.SkeletonPill
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.theme.FundtrailAppTheme
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

@Composable
fun DashboardRoute(
    onAddExpense: () -> Unit,
    onConfirmSalary: (expectedAmountMinor: Long?, date: LocalDate) -> Unit = { _, _ -> },
    onSetGoal: () -> Unit,
    onOpenGoal: () -> Unit,
    onOpenRecurring: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile by profileViewModel.profile.collectAsStateWithLifecycle()
    var showIncomeSettings by rememberSaveable { mutableStateOf(false) }
    if (showIncomeSettings) IncomeSettingsDialog(onDismiss = { showIncomeSettings = false })
    DashboardScreen(
        state = state,
        onOpenIncomeSettings = { showIncomeSettings = true },
        profile = profile,
        onOpenSettings = onOpenSettings,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onRetry = viewModel::onRetry,
        onAddExpense = onAddExpense,
        onConfirmSalary = { amount, salaryPayday ->
            val date = state.month.atDay(salaryPayday.coerceIn(1, state.month.lengthOfMonth()))
            onConfirmSalary(amount, date)
        },
        onSetGoal = onSetGoal,
        onOpenGoal = onOpenGoal,
        onOpenRecurring = onOpenRecurring,
        onOpenInsights = onOpenInsights,
        onConfirmDue = viewModel::onConfirmDue,
        onSkipDue = viewModel::onSkipDue,
        onDueConfirmAmountChange = viewModel::onDueConfirmAmountChange,
        onDueConfirmSave = viewModel::onDueConfirmSave,
        onDueConfirmDismiss = viewModel::onDueConfirmDismiss,
        onDueErrorShown = viewModel::onDueErrorShown,
        onSafeToSpendClick = viewModel::onSafeToSpendClick,
        onSafeToSpendDismiss = viewModel::onSafeToSpendDismiss,
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    profile: UserProfile? = null,
    onOpenSettings: () -> Unit = {},
    onOpenIncomeSettings: () -> Unit = {},
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRetry: () -> Unit,
    onAddExpense: () -> Unit,
    onConfirmSalary: (expectedAmountMinor: Long?, salaryPayday: Int) -> Unit = { _, _ -> },
    onSetGoal: () -> Unit,
    onOpenGoal: () -> Unit,
    onOpenRecurring: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onConfirmDue: (String) -> Unit = {},
    onSkipDue: (String) -> Unit = {},
    onDueConfirmAmountChange: (String) -> Unit = {},
    onDueConfirmSave: () -> Unit = {},
    onDueConfirmDismiss: () -> Unit = {},
    onDueErrorShown: () -> Unit = {},
    onSafeToSpendClick: () -> Unit = {},
    onSafeToSpendDismiss: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val dueError = state.dueError?.let { stringResource(it) }
    LaunchedEffect(dueError) {
        if (dueError != null) {
            snackbarHostState.showSnackbar(dueError)
            onDueErrorShown()
        }
    }
    AppScreen(
        title = stringResource(R.string.dashboard_title),
        actions = {
            if (profile != null) {
                val label = stringResource(R.string.dashboard_open_settings)
                IconButton(onClick = onOpenSettings, modifier = Modifier.semantics { contentDescription = label }) {
                    Avatar(profile, size = 40.dp)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // The empty state has its own call to action, so the FAB would only repeat it.
            if (state.content !is UiState.Empty) {
                ExtendedFloatingActionButton(
                    onClick = onAddExpense,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.dashboard_add_expense)) },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            MonthSwitcher(state.month, state.canGoNext, onPreviousMonth, onNextMonth)
            Column(Modifier.padding(horizontal = Spacing.screen), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                when (val status = state.salaryStatus) {
                    is SalaryStatus.Due -> SalaryDueCard(status.expectedAmountMinor, onOpenIncomeSettings) { onConfirmSalary(status.expectedAmountMinor, status.salaryPayday) }
                    is SalaryStatus.Received -> SalaryReceivedCard(status)
                    SalaryStatus.NotDue -> Unit
                }
                // Due items are as-of-today, so the card shows above the month content whatever month is selected (#200).
                if (state.due.isNotEmpty()) DueCard(state.due, onConfirmDue, onSkipDue, onOpenRecurring)
                UiStateContent(
                    state = state.content,
                    onRetry = onRetry,
                    loading = { LoadingSkeleton() },
                    empty = { EmptyState(state.goal, onAddExpense, onSetGoal, onOpenGoal, state.safeToSpend, onSafeToSpendClick) },
                ) { data ->
                    HeroCard(data, state.safeToSpend, onSafeToSpendClick)
                    SummaryCard(data)
                    GoalCard(state.goal, onSetGoal, onOpenGoal)
                    SplitCard(data)
                    AppCard(contentPadding = 0.dp) {
                        ListRow(
                            title = stringResource(R.string.dashboard_insights),
                            icon = Icons.Outlined.Insights,
                            onClick = onOpenInsights,
                        )
                    }
                }
                Spacer(Modifier.height(88.dp)) // keeps the last card clear of the FAB
            }
        }
    }

    state.dueConfirm?.let { sheet ->
        DueConfirmSheetContent(sheet, onDueConfirmAmountChange, onDueConfirmSave, onDueConfirmDismiss)
    }
    if (state.safeToSpendExplainerOpen) {
        state.safeToSpend?.let { SafeToSpendExplainerSheet(it, onSafeToSpendDismiss) }
    }
}

/**
 * The one figure the screen leads with: what is safe to spend this month, or what was saved when looking at a
 * past month. #215/#217: neutral in every state - never an error colour, even when the month is used up (A-22, A-23).
 */
@Composable
private fun HeroCard(data: DashboardData, safeToSpend: SafeToSpend?, onSafeToSpendClick: () -> Unit) {
    val onHero = MaterialTheme.colorScheme.onHero
    HeroSurface(onClick = onSafeToSpendClick.takeIf { safeToSpend != null }) {
        if (safeToSpend == null) {
            Text(stringResource(R.string.dashboard_saved), style = MaterialTheme.typography.labelLarge, color = onHero.copy(alpha = 0.8f))
            CountingHeroAmount(data.savedMinor, color = onHero)
            return@HeroSurface
        }
        Text(stringResource(R.string.dashboard_safe_title), style = MaterialTheme.typography.labelLarge, color = onHero.copy(alpha = 0.8f))
        if (safeToSpend.state == SafeToSpendState.AVAILABLE) {
            CountingHeroAmount(safeToSpend.amountMinor, color = onHero)
            safeToSpend.perDayMinor?.let {
                Text(
                    pluralStringResource(R.plurals.dashboard_safe_per_day, safeToSpend.daysLeft, MoneyFormatter.formatLkr(it), safeToSpend.daysLeft),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onHero,
                )
            }
        } else {
            Text(safeToSpendHeadline(safeToSpend), style = MaterialTheme.typography.titleLarge, color = onHero)
        }
        HeroPill(
            stringResource(R.string.dashboard_safe_tap_hint),
            icon = Icons.Outlined.Info,
            modifier = Modifier.padding(top = Spacing.md),
        )
    }
}

/** An empty month still gets the first-run hint (or "waiting for salary"), as a plain card. */
@Composable
private fun SafeToSpendCard(data: SafeToSpend, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        SectionTitle(stringResource(R.string.dashboard_safe_title))
        Text(safeToSpendHeadline(data), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.dashboard_safe_tap_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun safeToSpendHeadline(data: SafeToSpend): String = when (data.state) {
    SafeToSpendState.AVAILABLE -> {
        val amount = MoneyFormatter.formatLkr(data.amountMinor)
        data.perDayMinor
            ?.let {
                pluralStringResource(
                    R.plurals.dashboard_safe_available_per_day, data.daysLeft, amount, MoneyFormatter.formatLkr(it), data.daysLeft,
                )
            }
            ?: stringResource(R.string.dashboard_safe_available, amount)
    }
    SafeToSpendState.FULLY_USED ->
        if (data.overByMinor > 0L) {
            stringResource(R.string.dashboard_safe_fully_used, MoneyFormatter.formatLkr(data.overByMinor))
        } else {
            stringResource(R.string.dashboard_safe_fully_used_exact)
        }
    SafeToSpendState.WAITING_FOR_SALARY -> stringResource(R.string.dashboard_safe_waiting)
    SafeToSpendState.NOT_ENOUGH_DATA -> stringResource(R.string.dashboard_safe_first_run)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SafeToSpendExplainerSheet(data: SafeToSpend, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.xl).padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(stringResource(R.string.dashboard_safe_explainer_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(
                    R.string.dashboard_safe_formula,
                    MoneyFormatter.formatLkr(data.parts.receivedMinor),
                    MoneyFormatter.formatLkr(data.parts.committedMinor),
                    MoneyFormatter.formatLkr(data.parts.goalSavingMinor),
                    MoneyFormatter.formatLkr(data.parts.discretionarySpentMinor),
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.dashboard_safe_result, MoneyFormatter.formatLkr(data.amountMinor)),
                style = MaterialTheme.typography.titleMedium,
            )
            listOf(
                R.string.dashboard_safe_note_received,
                R.string.dashboard_safe_note_committed,
                R.string.dashboard_safe_note_goal,
                R.string.dashboard_safe_note_spent,
            ).forEach { note ->
                Text(stringResource(note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PrimaryButton(stringResource(R.string.dashboard_safe_close), onClick = onDismiss)
        }
    }
}

@Composable
private fun DueCard(items: List<DueItemData>, onConfirm: (String) -> Unit, onSkip: (String) -> Unit, onViewAll: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.dashboard_due_title), Modifier.weight(1f))
            TextButton(onClick = onViewAll) { Text(stringResource(R.string.dashboard_due_view_all)) }
        }
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.dashboard_due_date, DateFormatter.format(item.dueDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(MoneyFormatter.formatLkr(item.expectedMinor), style = MaterialTheme.typography.titleMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilledTonalButton(onClick = { onConfirm(item.templateId) }) {
                    Text(stringResource(R.string.dashboard_due_confirm))
                }
                TextButton(onClick = { onSkip(item.templateId) }) {
                    Text(stringResource(R.string.dashboard_due_skip))
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DueConfirmSheetContent(
    sheet: DueConfirmSheet,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.xl).padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(stringResource(R.string.dashboard_due_confirm_title, sheet.name), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = sheet.amountText,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.dashboard_due_confirm_amount)) },
                singleLine = true,
                enabled = !sheet.isSaving,
                isError = sheet.error != null,
                supportingText = sheet.error?.let { { Text(stringResource(it)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(stringResource(R.string.dashboard_due_confirm), onClick = onSave, loading = sheet.isSaving)
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.category_cancel)) }
        }
    }
}

@Composable
private fun LabelValueRow(label: String, value: String, emphasised: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        // The value is weighted, not the label: a long value (e.g. dashboard_goal_no_projection)
        // would otherwise squeeze the label to near-zero width.
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (emphasised) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = if (emphasised) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = Spacing.sm),
        )
    }
}

@Composable
private fun AmountRow(label: String, amountMinor: Long, emphasised: Boolean = false) {
    LabelValueRow(label, MoneyFormatter.formatLkr(amountMinor), emphasised)
}

/** Received and spent side by side, then what is left. Saved is green when positive and red when negative. */
@Composable
private fun SummaryCard(data: DashboardData) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        StatTile(
            stringResource(R.string.dashboard_received),
            MoneyFormatter.formatLkr(data.receivedMinor),
            Icons.Outlined.SouthWest,
            Modifier.weight(1f).fillMaxHeight(),
        )
        StatTile(
            stringResource(R.string.dashboard_spent),
            MoneyFormatter.formatLkr(data.spentMinor),
            Icons.Outlined.NorthEast,
            Modifier.weight(1f).fillMaxHeight(),
        )
    }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.dashboard_saved),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    MoneyFormatter.formatLkr(data.savedMinor),
                    style = MaterialTheme.typography.headlineSmall,
                    color = signColor(data.savedMinor),
                )
            }
            IconBadge(Icons.Outlined.Savings)
        }
        Text(
            stringResource(R.string.dashboard_received_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (data.incomeBySource.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = Spacing.xs), color = MaterialTheme.colorScheme.outlineVariant)
            SectionTitle(stringResource(R.string.dashboard_income_by_source))
            data.incomeBySource.forEach { (source, amount) -> AmountRow(stringResource(source.label), amount) }
        }
    }
}

/** Green above zero, red below, the default text colour at zero. */
@Composable
private fun signColor(amountMinor: Long): Color = when {
    amountMinor > 0L -> MaterialTheme.colorScheme.positive
    amountMinor < 0L -> MaterialTheme.colorScheme.negative
    else -> Color.Unspecified
}

/** S1 callout 2 (A-19, FR-76-79, FR-92): required vs actual saving, status and projected date. */
@Composable
private fun GoalCard(goal: UiState<GoalCardData>, onSetGoal: () -> Unit, onOpenGoal: () -> Unit) {
    when (goal) {
        // The page skeleton already covers this; a second skeleton here would just repeat it.
        UiState.Loading -> Unit
        UiState.Empty -> AppCard {
            EmptyView(
                icon = Icons.Outlined.Flag,
                title = stringResource(R.string.dashboard_goal_title),
                body = stringResource(R.string.dashboard_goal_placeholder),
                actionLabel = stringResource(R.string.dashboard_goal_set),
                onAction = onSetGoal,
                illustration = R.drawable.ill_empty_goal,
            )
        }
        is UiState.Error -> AppCard {
            SectionTitle(stringResource(R.string.dashboard_goal_title))
            Text(stringResource(goal.message), style = MaterialTheme.typography.bodyMedium)
        }
        is UiState.Success -> GoalCardContent(goal.data, onOpenGoal)
    }
}

@Composable
private fun GoalCardContent(data: GoalCardData, onOpenGoal: () -> Unit) {
    AppCard(onClick = onOpenGoal, spacing = Spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            ProgressRing(
                fraction = data.percent / 100f,
                centerText = stringResource(R.string.dashboard_share_percent, data.percent),
                description = stringResource(R.string.dashboard_goal_progress_description, data.percent, data.name),
                size = 72.dp,
                strokeWidth = 8.dp,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    stringResource(R.string.dashboard_goal_title_named, data.name),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(
                        R.string.dashboard_goal_of_target,
                        MoneyFormatter.formatLkr(data.savedToDateMinor),
                        MoneyFormatter.formatLkr(data.targetMinor),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GoalStatusChip(data.status, data.monthlyGapMinor)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            InsetStat(
                stringResource(R.string.dashboard_goal_required),
                MoneyFormatter.formatLkr(data.requiredMonthlyMinor),
                Modifier.weight(1f),
            )
            data.actualPaceMinor?.let { pace ->
                InsetStat(
                    stringResource(R.string.dashboard_goal_pace),
                    MoneyFormatter.formatLkr(pace),
                    Modifier.weight(1f),
                    valueColor = if (pace >= data.requiredMonthlyMinor) MaterialTheme.colorScheme.positive else MaterialTheme.colorScheme.negative,
                )
            }
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            InsetStat(
                stringResource(R.string.dashboard_goal_projected),
                data.projectedDate?.let { DateFormatter.format(it) } ?: stringResource(R.string.dashboard_goal_no_projection),
                Modifier.weight(1f).fillMaxHeight(),
            )
            InsetStat(
                stringResource(R.string.dashboard_goal_deadline),
                DateFormatter.format(data.deadline),
                Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun GoalStatusChip(status: GoalCardStatus, gapMinor: Long?) {
    val label = when (status) {
        GoalCardStatus.AHEAD -> stringResource(R.string.goal_status_ahead, MoneyFormatter.formatLkr(abs(gapMinor ?: 0L)))
        GoalCardStatus.ON_TRACK -> stringResource(R.string.goal_status_on_track)
        GoalCardStatus.BEHIND -> stringResource(R.string.goal_status_behind, MoneyFormatter.formatLkr(abs(gapMinor ?: 0L)))
        GoalCardStatus.NOT_ENOUGH_DATA -> stringResource(R.string.goal_status_not_enough_data)
        GoalCardStatus.NEEDS_ADJUSTING -> stringResource(R.string.goal_status_needs_adjusting)
    }
    StatusChip(label, container = goalStatusContainerColor(status), content = goalStatusContentColor(status))
}

// Green when ahead or on track, red when behind or the goal can't be met as set, neutral without enough data.
@Composable
private fun goalStatusContainerColor(status: GoalCardStatus): Color = when (status) {
    GoalCardStatus.AHEAD -> MaterialTheme.colorScheme.primaryContainer
    GoalCardStatus.ON_TRACK -> MaterialTheme.colorScheme.secondaryContainer
    GoalCardStatus.BEHIND, GoalCardStatus.NEEDS_ADJUSTING -> MaterialTheme.colorScheme.errorContainer
    GoalCardStatus.NOT_ENOUGH_DATA -> MaterialTheme.colorScheme.surfaceContainerHigh
}

@Composable
private fun goalStatusContentColor(status: GoalCardStatus): Color = when (status) {
    GoalCardStatus.AHEAD -> MaterialTheme.colorScheme.onPrimaryContainer
    GoalCardStatus.ON_TRACK -> MaterialTheme.colorScheme.onSecondaryContainer
    GoalCardStatus.BEHIND, GoalCardStatus.NEEDS_ADJUSTING -> MaterialTheme.colorScheme.onErrorContainer
    GoalCardStatus.NOT_ENOUGH_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun SplitCard(data: DashboardData) {
    AppCard {
        SectionTitle(stringResource(R.string.dashboard_split_title))
        val committedColor = MaterialTheme.colorScheme.primary
        val discretionaryColor = MaterialTheme.colorScheme.tertiary
        if (data.committedMinor <= 0L && data.discretionaryMinor <= 0L) {
            Text(
                stringResource(R.string.dashboard_split_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@AppCard
        }
        val splitDescription = "${stringResource(R.string.dashboard_committed)} ${MoneyFormatter.formatLkr(data.committedMinor)}, " +
            "${stringResource(R.string.dashboard_discretionary)} ${MoneyFormatter.formatLkr(data.discretionaryMinor)}"
        // Plain Compose bar; ADR-005 (#123) records the chart approach.
        Row(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .semantics { contentDescription = splitDescription },
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (data.committedMinor > 0L) Box(Modifier.weight(data.committedMinor.toFloat()).fillMaxSize().background(committedColor))
            if (data.discretionaryMinor > 0L) Box(Modifier.weight(data.discretionaryMinor.toFloat()).fillMaxSize().background(discretionaryColor))
        }
        LegendRow(committedColor, stringResource(R.string.dashboard_committed), data.committedMinor)
        LegendRow(discretionaryColor, stringResource(R.string.dashboard_discretionary), data.discretionaryMinor)

        if (data.topCategories.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = Spacing.xs), color = MaterialTheme.colorScheme.outlineVariant)
            SectionTitle(stringResource(R.string.dashboard_top_categories))
            data.topCategories.forEach { category ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            category.name ?: stringResource(R.string.dashboard_unknown_category),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(MoneyFormatter.formatLkr(category.amountMinor), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.dashboard_share_percent, category.sharePercent),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Spacing.sm).widthIn(min = 40.dp),
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((category.sharePercent / 100f).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(if (category.spendKind == SpendKind.COMMITTED) committedColor else discretionaryColor),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, amountMinor: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.size(Spacing.sm))
        AmountRow(label, amountMinor)
    }
}

/** Same cards, same order as the loaded page: month summary, safe to spend, goal, split. */
@Composable
private fun LoadingSkeleton() {
    Skeleton(stringResource(R.string.dashboard_loading), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonCard {
            SkeletonLine(0.32f, 12.dp)
            repeat(3) { SkeletonLabelValue(labelFraction = 0.28f, valueWidth = 104.dp, height = 18.dp) }
            SkeletonLine(0.7f, 10.dp)
        }
        SkeletonCard {
            SkeletonLine(0.38f, 12.dp)
            SkeletonLine(0.5f, 28.dp)
            SkeletonLine(0.8f, 10.dp)
        }
        SkeletonCard {
            SkeletonLine(0.24f, 12.dp)
            SkeletonLine(0.45f, 16.dp)
            SkeletonLine(1f, 8.dp)
            SkeletonLabelValue(labelFraction = 0.4f, valueWidth = 72.dp)
        }
        SkeletonCard {
            SkeletonLine(0.36f, 12.dp)
            SkeletonLine(1f, 16.dp)
            repeat(2) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonCircle(10.dp)
                    SkeletonLine(0.3f, 12.dp)
                    Box(Modifier.weight(1f))
                    SkeletonPill(80.dp)
                }
            }
        }
    }
}

/** One teaching state with one action; the first-run hint and the goal prompt follow it. */
@Composable
private fun EmptyState(
    goal: UiState<GoalCardData>,
    onAddExpense: () -> Unit,
    onSetGoal: () -> Unit,
    onOpenGoal: () -> Unit,
    safeToSpend: SafeToSpend?,
    onSafeToSpendClick: () -> Unit,
) {
    AppCard {
        EmptyView(
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            title = stringResource(R.string.dashboard_empty_title),
            illustration = R.drawable.ill_empty_transactions,
            body = stringResource(R.string.dashboard_empty_body),
            actionLabel = stringResource(R.string.dashboard_add_first_expense),
            actionIcon = Icons.Default.Add,
            onAction = onAddExpense,
        )
    }
    safeToSpend?.let { SafeToSpendCard(it, onSafeToSpendClick) }
    GoalCard(goal, onSetGoal, onOpenGoal)
}

@get:StringRes
private val IncomeSource.label: Int
    get() = when (this) {
        IncomeSource.SALARY -> R.string.income_source_salary
        IncomeSource.FREELANCE -> R.string.income_source_freelance
        IncomeSource.ADSENSE -> R.string.income_source_adsense
        IncomeSource.CRYPTO -> R.string.income_source_crypto
        IncomeSource.OTHER -> R.string.income_source_other
    }

private val previewDashboardData = DashboardData(
    receivedMinor = 13_450_000L,
    spentMinor = 9_630_000L,
    savedMinor = 3_820_000L,
    incomeBySource = listOf(IncomeSource.SALARY to 13_000_000L, IncomeSource.FREELANCE to 450_000L),
    committedMinor = 6_200_000L,
    discretionaryMinor = 3_430_000L,
    topCategories = listOf(
        RankedCategory("Rent", SpendKind.COMMITTED, 3_400_000L, 35),
        RankedCategory("Coffee and dining", SpendKind.DISCRETIONARY, 2_400_000L, 25),
        RankedCategory("Groceries", SpendKind.COMMITTED, 1_500_000L, 16),
    ),
)

// Scenario figures (S1 callout 2, Docs/Scenario analysis.md), in LKR minor units.
private val previewGoalCard = GoalCardData(
    name = "MacBook Pro M4",
    savedToDateMinor = 1_120_000L,
    targetMinor = 49_000_000L,
    percent = 2,
    requiredMonthlyMinor = 3_990_000L,
    actualPaceMinor = 2_000_000L,
    status = GoalCardStatus.BEHIND,
    monthlyGapMinor = 1_990_000L,
    projectedDate = LocalDate.of(2028, 10, 5),
    deadline = LocalDate.of(2027, 10, 7),
)

@Preview(showBackground = true, name = "Populated")
@Composable
private fun DashboardScreenPreview() {
    FundtrailAppTheme {
        DashboardScreen(
            state = DashboardUiState(
                month = YearMonth.of(2026, 10),
                canGoNext = false,
                content = UiState.Success(previewDashboardData),
                goal = UiState.Success(previewGoalCard),
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onRetry = {},
            onAddExpense = {},
            onSetGoal = {},
            onOpenGoal = {},
        )
    }
}

@Preview(showBackground = true, name = "No goal")
@Composable
private fun DashboardScreenNoGoalPreview() {
    FundtrailAppTheme {
        DashboardScreen(
            state = DashboardUiState(
                month = YearMonth.of(2026, 10),
                canGoNext = false,
                content = UiState.Success(previewDashboardData),
                goal = UiState.Empty,
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onRetry = {},
            onAddExpense = {},
            onSetGoal = {},
            onOpenGoal = {},
        )
    }
}

@Preview(showBackground = true, name = "Infeasible goal")
@Composable
private fun DashboardScreenInfeasibleGoalPreview() {
    FundtrailAppTheme {
        DashboardScreen(
            state = DashboardUiState(
                month = YearMonth.of(2026, 10),
                canGoNext = false,
                content = UiState.Success(previewDashboardData),
                goal = UiState.Success(
                    previewGoalCard.copy(status = GoalCardStatus.NEEDS_ADJUSTING, actualPaceMinor = null, monthlyGapMinor = null, projectedDate = null),
                ),
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onRetry = {},
            onAddExpense = {},
            onSetGoal = {},
            onOpenGoal = {},
        )
    }
}

@Preview(showBackground = true, name = "Safe to spend")
@Composable
private fun DashboardScreenSafeToSpendPreview() {
    FundtrailAppTheme {
        DashboardScreen(
            state = DashboardUiState(
                month = YearMonth.of(2026, 10),
                canGoNext = false,
                content = UiState.Success(previewDashboardData),
                goal = UiState.Success(previewGoalCard),
                safeToSpend = SafeToSpend(
                    amountMinor = 1_840_000L,
                    perDayMinor = 230_000L,
                    state = SafeToSpendState.AVAILABLE,
                    daysLeft = 8,
                    parts = SafeToSpendParts(13_450_000L, 6_200_000L, 3_990_000L, 1_220_000L),
                ),
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onRetry = {},
            onAddExpense = {},
            onSetGoal = {},
            onOpenGoal = {},
        )
    }
}

@Composable
private fun SalaryDueCard(expectedSalaryMinor: Long?, onSetSalary: () -> Unit, onConfirm: () -> Unit) {
    AppCard {
        SectionTitle(stringResource(R.string.dashboard_salary_due_title))
        if (expectedSalaryMinor != null) {
            Text(
                stringResource(R.string.dashboard_salary_due_body, MoneyFormatter.formatLkr(expectedSalaryMinor)),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (expectedSalaryMinor == null) {
            Text(
                stringResource(R.string.dashboard_salary_due_no_amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryButton(stringResource(R.string.dashboard_salary_due_confirm), onClick = onConfirm)
        if (expectedSalaryMinor == null) {
            TextButton(onClick = onSetSalary, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.income_salary_set_action)) }
        }
    }
}

@Composable
private fun SalaryReceivedCard(received: SalaryStatus.Received) {
    AppCard {
        SectionTitle(stringResource(R.string.dashboard_salary_received_title))
        AmountRow(stringResource(R.string.income_source_salary), received.amountMinor, emphasised = true)
        received.differenceFromExpected?.takeIf { it != 0L }?.let { diff ->
            val sign = if (diff > 0) "+" else "-"
            val diffText = "LKR $sign" + MoneyFormatter.formatLkr(abs(diff)).removePrefix("LKR ")
            Text(
                stringResource(R.string.dashboard_salary_received_variance, diffText),
                style = MaterialTheme.typography.bodySmall,
                color = signColor(diff),
            )
        }
    }
}
