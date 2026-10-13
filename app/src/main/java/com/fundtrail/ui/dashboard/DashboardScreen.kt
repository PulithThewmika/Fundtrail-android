package com.fundtrail.ui.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.ui.common.MoneyFormatter
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.theme.FundtrailAppTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun DashboardRoute(
    onAddExpense: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onRetry = viewModel::onRetry,
        onAddExpense = onAddExpense,
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRetry: () -> Unit,
    onAddExpense: () -> Unit,
) {
    Scaffold(
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
            when (val content = state.content) {
                UiState.Loading -> LoadingSkeleton()
                UiState.Empty -> EmptyState(onAddExpense)
                is UiState.Error -> ErrorState(content.message, onRetry)
                is UiState.Success -> {
                    SummaryCard(content.data)
                    GoalCardSlot()
                    SplitCard(content.data)
                }
            }
            Spacer(Modifier.height(72.dp)) // keeps the last card clear of the FAB
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
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelLarge)
            content()
        }
    }
}

@Composable
private fun AmountRow(label: String, amountMinor: Long, emphasised: Boolean = false) {
    val style = if (emphasised) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = style, modifier = Modifier.weight(1f))
        Text(MoneyFormatter.formatLkr(amountMinor), style = style)
    }
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

/** Reserved for the M3 goal card; shows only neutral copy so no unfinished figures reach the user. */
// TODO(M3): replace with the goal progress card (EPIC-010 out of scope, wireframe S1 callout 2).
@Composable
private fun GoalCardSlot() {
    SectionCard(stringResource(R.string.dashboard_goal_title)) {
        Text(stringResource(R.string.dashboard_goal_placeholder), style = MaterialTheme.typography.bodyMedium)
    }
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
        listOf(160.dp, 72.dp, 140.dp).forEach { height ->
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
private fun EmptyState(onAddExpense: () -> Unit) {
    SectionCard(stringResource(R.string.dashboard_this_month)) {
        Text(stringResource(R.string.dashboard_empty_title), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.dashboard_empty_body), style = MaterialTheme.typography.bodyMedium)
    }
    GoalCardSlot()
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

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    FundtrailAppTheme {
        DashboardScreen(
            state = DashboardUiState(
                month = YearMonth.of(2026, 10),
                canGoNext = false,
                content = UiState.Success(
                    DashboardData(
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
                    ),
                ),
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onRetry = {},
            onAddExpense = {},
        )
    }
}
