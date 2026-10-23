package com.fundtrail.ui.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.SalaryStatus
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.ui.common.DateFormatter
import com.fundtrail.ui.common.MoneyFormatter
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.theme.FundtrailAppTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun DashboardRoute(
    onAddExpense: () -> Unit,
    onConfirmSalary: (expectedAmountMinor: Long?, date: LocalDate) -> Unit = { _, _ -> },
    onSetGoal: () -> Unit,
    onOpenGoal: () -> Unit,
    onOpenRecurring: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state,
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
        onConfirmDue = viewModel::onConfirmDue,
        onSkipDue = viewModel::onSkipDue,
        onDueConfirmAmountChange = viewModel::onDueConfirmAmountChange,
        onDueConfirmSave = viewModel::onDueConfirmSave,
        onDueConfirmDismiss = viewModel::onDueConfirmDismiss,
        onDueErrorShown = viewModel::onDueErrorShown,
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRetry: () -> Unit,
    onAddExpense: () -> Unit,
    onConfirmSalary: (expectedAmountMinor: Long?, salaryPayday: Int) -> Unit = { _, _ -> },
    onSetGoal: () -> Unit,
    onOpenGoal: () -> Unit,
    onOpenRecurring: () -> Unit = {},
    onConfirmDue: (String) -> Unit = {},
    onSkipDue: (String) -> Unit = {},
    onDueConfirmAmountChange: (String) -> Unit = {},
    onDueConfirmSave: () -> Unit = {},
    onDueConfirmDismiss: () -> Unit = {},
    onDueErrorShown: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val dueError = state.dueError?.let { stringResource(it) }
    LaunchedEffect(dueError) {
        if (dueError != null) {
            snackbarHostState.showSnackbar(dueError)
            onDueErrorShown()
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // The empty state has its own call to action, so the FAB would only repeat it.
            if (state.content !is UiState.Empty) {
                ExtendedFloatingActionButton(
                    onClick = onAddExpense,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MonthHeader(state.month, state.canGoNext, onPreviousMonth, onNextMonth)
            when (val status = state.salaryStatus) {
                is SalaryStatus.Due -> SalaryDueCard(status.expectedAmountMinor) { onConfirmSalary(status.expectedAmountMinor, status.salaryPayday) }
                is SalaryStatus.Received -> SalaryReceivedCard(status)
                SalaryStatus.NotDue -> Unit
            }
            // Due items are as-of-today, so the card shows above the month content whatever month is selected (#200).
            if (state.due.isNotEmpty()) DueCard(state.due, onConfirmDue, onSkipDue, onOpenRecurring)
            when (val content = state.content) {
                UiState.Loading -> LoadingSkeleton()
                UiState.Empty -> EmptyState(state.goal, onAddExpense, onSetGoal, onOpenGoal)
                is UiState.Error -> ErrorState(content.message, onRetry)
                is UiState.Success -> {
                    SummaryCard(content.data)
                    GoalCard(state.goal, onSetGoal, onOpenGoal)
                    SplitCard(content.data)
                }
            }
            Spacer(Modifier.height(72.dp)) // keeps the last card clear of the FAB
        }
    }

    state.dueConfirm?.let { sheet ->
        DueConfirmSheetContent(sheet, onDueConfirmAmountChange, onDueConfirmSave, onDueConfirmDismiss)
    }
}

@Composable
private fun DueCard(items: List<DueItemData>, onConfirm: (String) -> Unit, onSkip: (String) -> Unit, onViewAll: () -> Unit) {
    SectionCard(stringResource(R.string.dashboard_due_title)) {
        items.forEachIndexed { index, item ->
            if (index > 0) Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.dashboard_due_date, DateFormatter.format(item.dueDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.dashboard_due_expected, MoneyFormatter.formatLkr(item.expectedMinor)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onConfirm(item.templateId) }) {
                        Text(stringResource(R.string.dashboard_due_confirm))
                    }
                    OutlinedButton(onClick = { onSkip(item.templateId) }) {
                        Text(stringResource(R.string.dashboard_due_skip))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onViewAll) { Text(stringResource(R.string.dashboard_due_view_all)) }
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
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.dashboard_due_confirm_title, sheet.name), style = MaterialTheme.typography.titleLarge)
            sheet.error?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedTextField(
                value = sheet.amountText,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.dashboard_due_confirm_amount)) },
                singleLine = true,
                enabled = !sheet.isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = !sheet.isSaving, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dashboard_due_confirm))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.category_cancel)) }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, canGoNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    val label = remember(month) { month.format(DateTimeFormatter.ofPattern("MMMM yyyy")) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.dashboard_previous_month))
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.dashboard_next_month))
        }
    }
}

@Composable
private fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelLarge)
            content()
        }
    }
}

@Composable
private fun LabelValueRow(label: String, value: String, emphasised: Boolean = false) {
    val style = if (emphasised) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
    Row(Modifier.fillMaxWidth()) {
        // The value is weighted, not the label: a long value (e.g. dashboard_goal_no_projection)
        // would otherwise squeeze the label to near-zero width.
        Text(label, style = style)
        Text(value, style = style, textAlign = TextAlign.End, modifier = Modifier.weight(1f).padding(start = 8.dp))
    }
}

@Composable
private fun AmountRow(label: String, amountMinor: Long, emphasised: Boolean = false) {
    LabelValueRow(label, MoneyFormatter.formatLkr(amountMinor), emphasised)
}

@Composable
private fun SummaryCard(data: DashboardData) {
    SectionCard(stringResource(R.string.dashboard_this_month)) {
        AmountRow(stringResource(R.string.dashboard_received), data.receivedMinor, emphasised = true)
        AmountRow(stringResource(R.string.dashboard_spent), data.spentMinor, emphasised = true)
        // A negative saving uses the same neutral colour as any other figure (A-22).
        AmountRow(stringResource(R.string.dashboard_saved), data.savedMinor, emphasised = true)
        Text(
            stringResource(R.string.dashboard_received_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (data.incomeBySource.isNotEmpty()) {
            Text(stringResource(R.string.dashboard_income_by_source), style = MaterialTheme.typography.labelMedium)
            data.incomeBySource.forEach { (source, amount) -> AmountRow(stringResource(source.label), amount) }
        }
    }
}

/** S1 callout 2 (A-19, FR-76-79, FR-92): required vs actual saving, status and projected date. */
@Composable
private fun GoalCard(goal: UiState<GoalCardData>, onSetGoal: () -> Unit, onOpenGoal: () -> Unit) {
    when (goal) {
        // The page skeleton already covers this; a second skeleton here would just repeat it.
        UiState.Loading -> Unit
        UiState.Empty -> GoalCardEmpty(onSetGoal)
        is UiState.Error -> SectionCard(stringResource(R.string.dashboard_goal_title)) {
            Text(stringResource(goal.message), style = MaterialTheme.typography.bodyMedium)
        }
        is UiState.Success -> GoalCardContent(goal.data, onOpenGoal)
    }
}

@Composable
private fun GoalCardEmpty(onSetGoal: () -> Unit) {
    SectionCard(stringResource(R.string.dashboard_goal_title)) {
        Text(stringResource(R.string.dashboard_goal_placeholder), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onSetGoal, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.dashboard_goal_set))
        }
    }
}

@Composable
private fun GoalCardContent(data: GoalCardData, onOpenGoal: () -> Unit) {
    SectionCard(
        title = stringResource(R.string.dashboard_goal_title_named, data.name),
        modifier = Modifier.clickable(onClick = onOpenGoal),
    ) {
        GoalProgressBar(data.percent, data.name)
        Text(
            stringResource(
                R.string.dashboard_goal_of_target,
                MoneyFormatter.formatLkr(data.savedToDateMinor),
                MoneyFormatter.formatLkr(data.targetMinor),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        AmountRow(stringResource(R.string.dashboard_goal_required), data.requiredMonthlyMinor)
        data.actualPaceMinor?.let { AmountRow(stringResource(R.string.dashboard_goal_pace), it) }
        GoalStatusChip(data.status, data.monthlyGapMinor)
        LabelValueRow(
            stringResource(R.string.dashboard_goal_projected),
            data.projectedDate?.let { DateFormatter.format(it) } ?: stringResource(R.string.dashboard_goal_no_projection),
        )
        LabelValueRow(stringResource(R.string.dashboard_goal_deadline), DateFormatter.format(data.deadline))
    }
}

@Composable
private fun GoalProgressBar(percent: Int, goalName: String) {
    val description = stringResource(R.string.dashboard_goal_progress_description, percent, goalName)
    Box(
        Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = description },
    ) {
        Box(
            Modifier
                .fillMaxWidth(percent / 100f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
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
    Surface(shape = CircleShape, color = goalStatusContainerColor(status)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = goalStatusContentColor(status),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            maxLines = 1,
        )
    }
}

// Never errorContainer: a behind or infeasible goal is a neutral chip, never a red alarm (NFR-14, NFR-16).
@Composable
private fun goalStatusContainerColor(status: GoalCardStatus): Color = when (status) {
    GoalCardStatus.AHEAD -> MaterialTheme.colorScheme.primaryContainer
    GoalCardStatus.ON_TRACK -> MaterialTheme.colorScheme.secondaryContainer
    GoalCardStatus.BEHIND -> MaterialTheme.colorScheme.tertiaryContainer
    GoalCardStatus.NOT_ENOUGH_DATA, GoalCardStatus.NEEDS_ADJUSTING -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun goalStatusContentColor(status: GoalCardStatus): Color = when (status) {
    GoalCardStatus.AHEAD -> MaterialTheme.colorScheme.onPrimaryContainer
    GoalCardStatus.ON_TRACK -> MaterialTheme.colorScheme.onSecondaryContainer
    GoalCardStatus.BEHIND -> MaterialTheme.colorScheme.onTertiaryContainer
    GoalCardStatus.NOT_ENOUGH_DATA, GoalCardStatus.NEEDS_ADJUSTING -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun SplitCard(data: DashboardData) {
    SectionCard(stringResource(R.string.dashboard_split_title)) {
        val committedColor = MaterialTheme.colorScheme.primary
        val discretionaryColor = MaterialTheme.colorScheme.tertiary
        if (data.committedMinor <= 0L && data.discretionaryMinor <= 0L) {
            Text(stringResource(R.string.dashboard_split_empty), style = MaterialTheme.typography.bodyMedium)
            return@SectionCard
        }
        val splitDescription = "${stringResource(R.string.dashboard_committed)} ${MoneyFormatter.formatLkr(data.committedMinor)}, " +
            "${stringResource(R.string.dashboard_discretionary)} ${MoneyFormatter.formatLkr(data.discretionaryMinor)}"
        // Plain Compose bar; ADR-005 (#123) records the chart approach.
        Row(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .semantics { contentDescription = splitDescription },
        ) {
            if (data.committedMinor > 0L) Box(Modifier.weight(data.committedMinor.toFloat()).fillMaxSize().background(committedColor))
            if (data.discretionaryMinor > 0L) Box(Modifier.weight(data.discretionaryMinor.toFloat()).fillMaxSize().background(discretionaryColor))
        }
        LegendRow(committedColor, stringResource(R.string.dashboard_committed), data.committedMinor)
        LegendRow(discretionaryColor, stringResource(R.string.dashboard_discretionary), data.discretionaryMinor)

        if (data.topCategories.isNotEmpty()) {
            Text(stringResource(R.string.dashboard_top_categories), style = MaterialTheme.typography.labelMedium)
            data.topCategories.forEachIndexed { index, category ->
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "${index + 1}  ${category.name ?: stringResource(R.string.dashboard_unknown_category)}",
                        modifier = Modifier.weight(1f),
                    )
                    Text(MoneyFormatter.formatLkr(category.amountMinor))
                    Text(
                        stringResource(R.string.dashboard_share_percent, category.sharePercent),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp).widthIn(min = 44.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, amountMinor: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.size(8.dp))
        AmountRow(label, amountMinor)
    }
}

@Composable
private fun LoadingSkeleton() {
    val description = stringResource(R.string.dashboard_loading)
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        listOf(160.dp, 180.dp, 140.dp).forEach { height ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

@Composable
private fun EmptyState(goal: UiState<GoalCardData>, onAddExpense: () -> Unit, onSetGoal: () -> Unit, onOpenGoal: () -> Unit) {
    SectionCard(stringResource(R.string.dashboard_this_month)) {
        Text(stringResource(R.string.dashboard_empty_title), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.dashboard_empty_body), style = MaterialTheme.typography.bodyMedium)
    }
    GoalCard(goal, onSetGoal, onOpenGoal)
    SectionCard(stringResource(R.string.dashboard_split_title)) {
        Text(stringResource(R.string.dashboard_split_empty), style = MaterialTheme.typography.bodyMedium)
    }
    Button(onClick = onAddExpense, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.dashboard_add_first_expense))
    }
}

@Composable
private fun ErrorState(@StringRes message: Int, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(message), textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
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

@Composable
private fun SalaryDueCard(expectedSalaryMinor: Long?, onConfirm: () -> Unit) {
    SectionCard(stringResource(R.string.dashboard_salary_due_title)) {
        if (expectedSalaryMinor != null) {
            Text(
                stringResource(R.string.dashboard_salary_due_body, MoneyFormatter.formatLkr(expectedSalaryMinor)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.dashboard_salary_due_confirm))
        }
    }
}

@Composable
private fun SalaryReceivedCard(received: SalaryStatus.Received) {
    SectionCard(stringResource(R.string.dashboard_salary_received_title)) {
        AmountRow(stringResource(R.string.income_source_salary), received.amountMinor, emphasised = true)
        received.differenceFromExpected?.takeIf { it != 0L }?.let { diff ->
            val sign = if (diff > 0) "+" else "-"
            val diffText = "LKR $sign" + MoneyFormatter.formatLkr(abs(diff)).removePrefix("LKR ")
            Text(
                stringResource(R.string.dashboard_salary_received_variance, diffText),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
