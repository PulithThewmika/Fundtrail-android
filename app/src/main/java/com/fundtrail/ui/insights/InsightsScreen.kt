package com.fundtrail.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.IncomeSummary
import com.fundtrail.domain.model.IncomeVsEstimate
import com.fundtrail.domain.model.IncomeVsEstimateComparison
import com.fundtrail.ui.common.MoneyFormatter
import com.fundtrail.ui.theme.FundtrailAppTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.time.format.DateTimeFormatter

@Composable
fun InsightsRoute(onBack: () -> Unit, viewModel: InsightsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsScreen(state = state, sourceOrder = viewModel.sourceOrder, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(state: InsightsUiState, sourceOrder: List<IncomeSource>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.insights_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.insights_back))
                    }
                },
            )
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
            if (!state.hasEnoughData) {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.insights_not_enough_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                TrendCard(state)
                IncomeBySourceCard(state, sourceOrder)
            }
            RecurringTotalCard(state.recurringMonthlyTotalMinor)
            state.incomeSummary?.let { IncomeSummaryCard(it) }
            state.incomeVsEstimate?.let { EstimateCard(it) }
        }
    }
}

/** FR-57: this month's received income, the trailing 3-month average, and the 6-month low-high range. */
@Composable
private fun IncomeSummaryCard(summary: IncomeSummary) {
    SectionCard(stringResource(R.string.insights_summary_title)) {
        Text(stringResource(R.string.insights_summary_this_month), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(MoneyFormatter.formatLkr(summary.thisMonthReceivedMinor), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.insights_summary_average), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(MoneyFormatter.formatLkr(summary.averageMinor), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.insights_summary_range), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            stringResource(R.string.insights_summary_range_value, MoneyFormatter.formatLkr(summary.lowestMinor), MoneyFormatter.formatLkr(summary.highestMinor)),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/**
 * The user's own monthly estimate beside what actually arrived (FR-58, A-16). Received money only, averaged over up
 * to the last 3 complete months. Neutral wording and colours throughout: below the estimate is information, not a
 * warning (A-03, NFR-14).
 */
@Composable
private fun EstimateCard(vsEstimate: IncomeVsEstimate) {
    SectionCard(stringResource(R.string.insights_estimate_title)) {
        if (vsEstimate.monthsCount > 0) {
            Text(
                pluralStringResource(R.plurals.insights_estimate_average, vsEstimate.monthsCount, vsEstimate.monthsCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(MoneyFormatter.formatLkr(vsEstimate.actualAverageMinor), style = MaterialTheme.typography.headlineSmall)
        } else {
            Text(
                stringResource(R.string.insights_estimate_no_history),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            stringResource(R.string.insights_estimate_yours),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(estimateRangeText(vsEstimate), style = MaterialTheme.typography.titleMedium)

        vsEstimate.comparison?.let { comparison ->
            Text(
                stringResource(
                    when (comparison) {
                        IncomeVsEstimateComparison.BELOW -> R.string.insights_estimate_below
                        IncomeVsEstimateComparison.WITHIN -> R.string.insights_estimate_within
                        IncomeVsEstimateComparison.ABOVE -> R.string.insights_estimate_above
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun estimateRangeText(vsEstimate: IncomeVsEstimate): String {
    val min = vsEstimate.estimateMinMinor
    val max = vsEstimate.estimateMaxMinor
    return when {
        min != null && max != null ->
            stringResource(R.string.insights_estimate_range, MoneyFormatter.formatLkr(min), MoneyFormatter.formatLkr(max))
        min != null -> stringResource(R.string.insights_estimate_from, MoneyFormatter.formatLkr(min))
        max != null -> stringResource(R.string.insights_estimate_up_to, MoneyFormatter.formatLkr(max))
        else -> stringResource(R.string.insights_estimate_not_set)
    }
}

@Preview(showBackground = true)
@Composable
private fun EstimateCardWithinPreview() {
    FundtrailAppTheme {
        EstimateCard(
            IncomeVsEstimate(
                comparison = IncomeVsEstimateComparison.WITHIN,
                actualAverageMinor = 18_500_000L,
                estimateMinMinor = 16_000_000L,
                estimateMaxMinor = 21_000_000L,
                monthsCount = 3,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EstimateCardNotSetPreview() {
    FundtrailAppTheme {
        EstimateCard(IncomeVsEstimate(comparison = null, actualAverageMinor = 0L, monthsCount = 0))
    }
}

@Composable
private fun TrendCard(state: InsightsUiState) {
    val received = state.trend.map { it.receivedMinor.toRupees() }
    val spent = state.trend.map { it.spentMinor.toRupees() }
    val saved = state.trend.map { it.netSavedMinor.toRupees() }
    val labels = state.trend.map { it.month.format(MONTH_LABEL) }
    val colors = MaterialTheme.colorScheme
    val lineColors = listOf(colors.primary, colors.error, colors.tertiary)

    SectionCard(stringResource(R.string.insights_trend_title)) {
        LineChart(series = listOf(received, spent, saved), lineColors = lineColors)
        Legend(
            listOf(
                colors.primary to stringResource(R.string.insights_series_received),
                colors.error to stringResource(R.string.insights_series_spent),
                colors.tertiary to stringResource(R.string.insights_series_saved),
            ),
        )
        // The chart is never the only way to read a figure (ADR-0006 accessibility): the latest month is spelled out.
        state.trend.lastOrNull()?.let { latest ->
            Text(
                stringResource(
                    R.string.insights_trend_latest,
                    labels.last(),
                    MoneyFormatter.formatLkr(latest.receivedMinor),
                    MoneyFormatter.formatLkr(latest.spentMinor),
                    MoneyFormatter.formatLkr(latest.netSavedMinor),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IncomeBySourceCard(state: InsightsUiState, sourceOrder: List<IncomeSource>) {
    val palette = sourcePalette()
    val series = sourceOrder.map { source -> state.incomeBySource.map { (it.bySource[source] ?: 0L).toRupees() } }
    SectionCard(stringResource(R.string.insights_by_source_title)) {
        LineChart(series = series, lineColors = sourceOrder.map { palette.getValue(it) })
        Legend(sourceOrder.map { palette.getValue(it) to stringResource(it.labelRes()) })
    }
}

@Composable
private fun RecurringTotalCard(totalMinor: Long) {
    SectionCard(stringResource(R.string.insights_recurring_title)) {
        Text(MoneyFormatter.formatLkr(totalMinor), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.insights_recurring_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LineChart(series: List<List<Float>>, lineColors: List<Color>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(series) {
        if (series.any { it.isNotEmpty() }) {
            modelProducer.runTransaction {
                lineSeries { series.forEach { series(it) } }
            }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(),
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxWidth().height(200.dp),
    )
}

@Composable
private fun Legend(entries: List<Pair<Color, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { (color, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun sourcePalette(): Map<IncomeSource, Color> {
    val c = MaterialTheme.colorScheme
    return mapOf(
        IncomeSource.SALARY to c.primary,
        IncomeSource.FREELANCE to c.secondary,
        IncomeSource.ADSENSE to c.tertiary,
        IncomeSource.CRYPTO to c.error,
        IncomeSource.OTHER to c.outline,
    )
}

private fun IncomeSource.labelRes(): Int = when (this) {
    IncomeSource.SALARY -> R.string.income_source_salary
    IncomeSource.FREELANCE -> R.string.income_source_freelance
    IncomeSource.ADSENSE -> R.string.income_source_adsense
    IncomeSource.CRYPTO -> R.string.income_source_crypto
    IncomeSource.OTHER -> R.string.income_source_other
}

/** Vico plots `Float`; charts are approximate by nature, so rupees (not minor units) keep the axis readable. */
private fun Long.toRupees(): Float = this / 100f

private val MONTH_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")
