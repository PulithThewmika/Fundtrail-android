package com.fundtrail.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.GoalProgress
import com.fundtrail.domain.model.GoalStatus
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.NatureShare
import com.fundtrail.domain.model.SalaryStatus
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.usecase.category.ObserveCategoriesUseCase
import com.fundtrail.domain.usecase.goal.GetGoalProgressUseCase
import com.fundtrail.domain.usecase.goal.ObserveActiveGoalUseCase
import com.fundtrail.domain.usecase.income.GetSalaryStatusUseCase
import com.fundtrail.domain.usecase.summary.GetMonthlySummaryUseCase
import com.fundtrail.domain.usecase.summary.GetSpendingBreakdownUseCase
import com.fundtrail.domain.usecase.sync.ObserveSyncStatusUseCase
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.common.mapErrorToMessage
import com.fundtrail.ui.common.uiStateOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.roundToInt

data class DashboardUiState(
    val month: YearMonth,
    /** False on the current month: there is nothing to show in the future. */
    val canGoNext: Boolean,
    val content: UiState<DashboardData> = UiState.Loading,
    /** As-of-today; does not depend on [month] (Empty = no active goal). */
    val goal: UiState<GoalCardData> = UiState.Loading,
    val salaryStatus: SalaryStatus = SalaryStatus.NotDue,
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

/** Status shown on the dashboard goal card. [GoalStatus.BEHIND] splits into BEHIND or NEEDS_ADJUSTING by [GoalProgress.isInfeasible] (ADR-0006). */
enum class GoalCardStatus { NOT_ENOUGH_DATA, AHEAD, ON_TRACK, BEHIND, NEEDS_ADJUSTING }

data class GoalCardData(
    val name: String,
    val savedToDateMinor: Long,
    val targetMinor: Long,
    /** 0..100. */
    val percent: Int,
    val requiredMonthlyMinor: Long,
    val actualPaceMinor: Long?,
    val status: GoalCardStatus,
    val monthlyGapMinor: Long?,
    val projectedDate: LocalDate?,
    val deadline: LocalDate,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    getMonthlySummary: GetMonthlySummaryUseCase,
    getSpendingBreakdown: GetSpendingBreakdownUseCase,
    observeCategories: ObserveCategoriesUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    observeActiveGoal: ObserveActiveGoalUseCase,
    getGoalProgress: GetGoalProgressUseCase,
    getSalaryStatus: GetSalaryStatusUseCase,
) : ViewModel() {

    private val currentMonth = YearMonth.now(APP_ZONE)
    private val selection = MutableStateFlow(Selection(currentMonth))

    private val monthState: Flow<DashboardUiState> = selection
        .flatMapLatest { (month) ->
            combine(
                getMonthlySummary(month),
                getSpendingBreakdown(month),
                observeCategories(),
                observeSyncStatus(),
                getSalaryStatus(month),
            ) { summary, breakdown, categories, sync, salaryStatus ->
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
                stateFor(month, content, salaryStatus)
            }
                .onStart { emit(stateFor(month, UiState.Loading)) }
                .catch { emit(stateFor(month, UiState.Error(mapErrorToMessage(it)))) }
        }

    // Sibling of monthState, not folded into it: goal progress is "as of today" and must not
    // restart every time the user pages to a different month (FR-96 live updates).
    private val goalState: Flow<UiState<GoalCardData>> =
        combine(observeActiveGoal(), getGoalProgress(), observeSyncStatus()) { goal, progress, sync ->
            val data = if (goal != null && progress != null) goalCardData(goal, progress) else null
            uiStateOf(data != null, sync) { data!! }
        }
            .onStart { emit(UiState.Loading) }
            .catch { emit(UiState.Error(mapErrorToMessage(it))) }

    val uiState: StateFlow<DashboardUiState> = combine(monthState, goalState) { base, goal -> base.copy(goal = goal) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), stateFor(currentMonth, UiState.Loading))

    fun onPreviousMonth() = selection.update { Selection(it.month.minusMonths(1)) }

    fun onNextMonth() = selection.update { if (it.month < currentMonth) Selection(it.month.plusMonths(1)) else it }

    /** A new [Selection] instance restarts the flows even though the month is the same. */
    fun onRetry() = selection.update { Selection(it.month) }

    private fun stateFor(
        month: YearMonth,
        content: UiState<DashboardData>,
        salaryStatus: SalaryStatus = SalaryStatus.NotDue,
    ) = DashboardUiState(
        month = month,
        canGoNext = month < currentMonth,
        content = content,
        salaryStatus = salaryStatus,
    )

    private fun List<NatureShare>.amountOf(kind: SpendKind) =
        firstOrNull { it.spendKind == kind }?.amountMinor ?: 0L

    private fun goalCardData(goal: Goal, progress: GoalProgress): GoalCardData {
        val percent = if (goal.targetLkrMinor <= 0L) {
            0
        } else {
            (progress.savedToDateMinor * 100 / goal.targetLkrMinor).coerceIn(0L, 100L).toInt()
        }
        val status = when {
            progress.isInfeasible -> GoalCardStatus.NEEDS_ADJUSTING
            else -> when (progress.status) {
                GoalStatus.NOT_ENOUGH_DATA -> GoalCardStatus.NOT_ENOUGH_DATA
                GoalStatus.AHEAD -> GoalCardStatus.AHEAD
                GoalStatus.ON_TRACK -> GoalCardStatus.ON_TRACK
                GoalStatus.BEHIND -> GoalCardStatus.BEHIND
            }
        }
        return GoalCardData(
            name = goal.name,
            savedToDateMinor = progress.savedToDateMinor,
            targetMinor = goal.targetLkrMinor,
            percent = percent,
            requiredMonthlyMinor = progress.requiredMonthlyMinor,
            actualPaceMinor = progress.actualPaceMinor,
            status = status,
            monthlyGapMinor = progress.monthlyGapMinor,
            projectedDate = progress.projectedDate,
            deadline = goal.deadline,
        )
    }

    // Not a data class: two selections of the same month must stay unequal so retry re-subscribes.
    private class Selection(val month: YearMonth) {
        operator fun component1() = month
    }

    private companion object {
        const val TOP_CATEGORIES = 5
    }
}
