package com.fundtrail.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.NatureShare
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.usecase.category.ObserveCategoriesUseCase
import com.fundtrail.domain.usecase.summary.GetMonthlySummaryUseCase
import com.fundtrail.domain.usecase.summary.GetSpendingBreakdownUseCase
import com.fundtrail.domain.usecase.sync.ObserveSyncStatusUseCase
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.common.mapErrorToMessage
import com.fundtrail.ui.common.uiStateOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.roundToInt

data class DashboardUiState(
    val month: YearMonth,
    /** False on the current month: there is nothing to show in the future. */
    val canGoNext: Boolean,
    val content: UiState<DashboardData> = UiState.Loading,
)

data class DashboardData(
    val receivedMinor: Long,
    val spentMinor: Long,
    val savedMinor: Long,
    /** Largest first. */
    val incomeBySource: List<Pair<IncomeSource, Long>>,
    val committedMinor: Long,
    val discretionaryMinor: Long,
    val topCategories: List<RankedCategory>,
)

/** [name] is null when the category is not in Room (yet). */
data class RankedCategory(val name: String?, val spendKind: SpendKind, val amountMinor: Long, val sharePercent: Int)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    getMonthlySummary: GetMonthlySummaryUseCase,
    getSpendingBreakdown: GetSpendingBreakdownUseCase,
    observeCategories: ObserveCategoriesUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
) : ViewModel() {

    private val currentMonth = YearMonth.now(APP_ZONE)
    private val selection = MutableStateFlow(Selection(currentMonth))

    val uiState: StateFlow<DashboardUiState> = selection
        .flatMapLatest { (month) ->
            combine(
                getMonthlySummary(month),
                getSpendingBreakdown(month),
                observeCategories(),
                observeSyncStatus(),
            ) { summary, breakdown, categories, sync ->
                val names = categories.associate { it.id to it.name }
                val content = uiStateOf(summary.incomeBySource.isNotEmpty() || breakdown.categories.isNotEmpty(), sync) {
                    DashboardData(
                        receivedMinor = summary.receivedIncomeMinor,
                        spentMinor = summary.spentMinor,
                        savedMinor = summary.netSavedMinor,
                        incomeBySource = summary.incomeBySource.toList().sortedByDescending { it.second },
                        committedMinor = breakdown.byNature.amountOf(SpendKind.COMMITTED),
                        discretionaryMinor = breakdown.byNature.amountOf(SpendKind.DISCRETIONARY),
                        topCategories = breakdown.categories.take(TOP_CATEGORIES).map {
                            RankedCategory(names[it.categoryId], it.spendKind, it.amountMinor, it.percent.roundToInt())
                        },
                    )
                }
                stateFor(month, content)
            }
                .onStart { emit(stateFor(month, UiState.Loading)) }
                .catch { emit(stateFor(month, UiState.Error(mapErrorToMessage(it)))) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), stateFor(currentMonth, UiState.Loading))

    fun onPreviousMonth() = selection.update { Selection(it.month.minusMonths(1)) }

    fun onNextMonth() = selection.update { if (it.month < currentMonth) Selection(it.month.plusMonths(1)) else it }

    /** A new [Selection] instance restarts the flows even though the month is the same. */
    fun onRetry() = selection.update { Selection(it.month) }

    private fun stateFor(month: YearMonth, content: UiState<DashboardData>) =
        DashboardUiState(month, canGoNext = month < currentMonth, content = content)

    private fun List<NatureShare>.amountOf(kind: SpendKind) =
        firstOrNull { it.spendKind == kind }?.amountMinor ?: 0L

    // Not a data class: two selections of the same month must stay unequal so retry re-subscribes.
    private class Selection(val month: YearMonth) {
        operator fun component1() = month
    }

    private companion object {
        const val TOP_CATEGORIES = 5
    }
}
