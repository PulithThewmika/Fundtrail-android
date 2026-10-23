package com.fundtrail.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.IncomeSourceTrendPoint
import com.fundtrail.domain.model.MonthlyTrendPoint
import com.fundtrail.domain.usecase.recurring.ObserveRecurringUseCase
import com.fundtrail.domain.usecase.summary.GetIncomeBySourceTrendUseCase
import com.fundtrail.domain.usecase.summary.GetMonthlyTrendUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * One state for the Insights screen (#210): the received/spent/saved trend, income by source, and the monthly
 * recurring total. The actual-vs-estimate card is #194 (EPIC-014/T5) and is left out behind a TODO(M4) until it
 * merges, so [estimateCardAvailable] stays false and nothing claims that acceptance criterion yet.
 */
data class InsightsUiState(
    val trend: List<MonthlyTrendPoint> = emptyList(),
    val incomeBySource: List<IncomeSourceTrendPoint> = emptyList(),
    val recurringMonthlyTotalMinor: Long = 0L,
    /** The chart needs two months of real data before a trend reads as anything (A-03). */
    val hasEnoughData: Boolean = false,
    /** TODO(M4): flips true once #194 lands and the estimate card has data. */
    val estimateCardAvailable: Boolean = false,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    getMonthlyTrend: GetMonthlyTrendUseCase,
    getIncomeBySourceTrend: GetIncomeBySourceTrendUseCase,
    observeRecurring: ObserveRecurringUseCase,
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> =
        combine(getMonthlyTrend(), getIncomeBySourceTrend(), observeRecurring()) { trend, bySource, templates ->
            val monthsWithData = trend.count { it.receivedMinor != 0L || it.spentMinor != 0L }
            InsightsUiState(
                trend = trend,
                incomeBySource = bySource,
                recurringMonthlyTotalMinor = templates.filter { !it.isPaused }.sumOf { it.expectedAmountMinor },
                hasEnoughData = monthsWithData >= 2,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    /** Sources in a stable order for the by-source chart legend. */
    val sourceOrder: List<IncomeSource> = IncomeSource.entries
}
