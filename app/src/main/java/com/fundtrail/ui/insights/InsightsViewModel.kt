package com.fundtrail.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.IncomeSourceTrendPoint
import com.fundtrail.domain.model.IncomeSummary
import com.fundtrail.domain.model.IncomeVsEstimate
import com.fundtrail.domain.model.MonthlyTrendPoint
import com.fundtrail.domain.usecase.income.GetIncomeSummaryUseCase
import com.fundtrail.domain.usecase.income.GetIncomeVsEstimateUseCase
import com.fundtrail.domain.usecase.recurring.ObserveRecurringUseCase
import com.fundtrail.domain.usecase.summary.GetIncomeBySourceTrendUseCase
import com.fundtrail.domain.usecase.summary.GetMonthlyTrendUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * One state for the Insights screen (#210): the received/spent/saved trend, income by source, the monthly
 * recurring total, and how the average received income compares with the user's own estimate (FR-58, #194).
 */
data class InsightsUiState(
    val trend: List<MonthlyTrendPoint> = emptyList(),
    val incomeBySource: List<IncomeSourceTrendPoint> = emptyList(),
    val recurringMonthlyTotalMinor: Long = 0L,
    /** The chart needs two months of real data before a trend reads as anything (A-03). */
    val hasEnoughData: Boolean = false,
    /** Null while loading or if it could not be read: only the estimate card is hidden, never the whole screen. */
    val incomeVsEstimate: IncomeVsEstimate? = null,
    /** FR-57: this month, trailing 3-month average, lowest-highest range. Null only while loading/on error. */
    val incomeSummary: IncomeSummary? = null,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    getMonthlyTrend: GetMonthlyTrendUseCase,
    getIncomeBySourceTrend: GetIncomeBySourceTrendUseCase,
    observeRecurring: ObserveRecurringUseCase,
    getIncomeVsEstimate: GetIncomeVsEstimateUseCase,
    getIncomeSummary: GetIncomeSummaryUseCase,
) : ViewModel() {

    // A failure here hides the estimate card only (null); it must not blank the trend and the other cards.
    private val estimate: Flow<IncomeVsEstimate?> =
        getIncomeVsEstimate()
            .map<IncomeVsEstimate, IncomeVsEstimate?> { it }
            .onStart { emit(null) }
            .catch { emit(null) }

    private val summary: Flow<IncomeSummary?> =
        getIncomeSummary()
            .map<IncomeSummary, IncomeSummary?> { it }
            .onStart { emit(null) }
            .catch { emit(null) }

    val uiState: StateFlow<InsightsUiState> =
        combine(getMonthlyTrend(), getIncomeBySourceTrend(), observeRecurring(), estimate, summary) { trend, bySource, templates, vsEstimate, incomeSummary ->
            val monthsWithData = trend.count { it.receivedMinor != 0L || it.spentMinor != 0L }
            InsightsUiState(
                trend = trend,
                incomeBySource = bySource,
                recurringMonthlyTotalMinor = templates.filter { !it.isPaused }.sumOf { it.expectedAmountMinor },
                hasEnoughData = monthsWithData >= 2,
                incomeVsEstimate = vsEstimate,
                incomeSummary = incomeSummary,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    /** Sources in a stable order for the by-source chart legend. */
    val sourceOrder: List<IncomeSource> = IncomeSource.entries
}
