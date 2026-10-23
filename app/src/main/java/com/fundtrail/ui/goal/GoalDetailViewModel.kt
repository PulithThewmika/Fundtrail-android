package com.fundtrail.ui.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.GoalProgress
import com.fundtrail.domain.model.GoalStatus
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.goal.GetGoalProgressUseCase
import com.fundtrail.domain.usecase.goal.ObserveActiveGoalUseCase
import com.fundtrail.domain.usecase.goal.SaveGoalUseCase
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.common.mapErrorToMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class GoalDetailStatus { AHEAD, ON_TRACK, BEHIND, NOT_ENOUGH_DATA, NEEDS_ADJUSTING }
data class GoalDetailData(
    val goal: Goal,
    val progress: GoalProgress,
    val percent: Int,
    val progressFraction: Float,
    val whatIfMinor: Long,
    val whatIfMaxMinor: Long,
    val whatIfDate: LocalDate?,
    val whatIfStatus: GoalDetailStatus,
    val statusGapMinor: Long,
)

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    observeGoal: ObserveActiveGoalUseCase,
    getProgress: GetGoalProgressUseCase,
    private val saveGoal: SaveGoalUseCase,
    private val clock: Clock,
) : ViewModel() {
    private val whatIf = MutableStateFlow<Long?>(null)
    private val _actionError = MutableStateFlow<Int?>(null)
    val actionError: StateFlow<Int?> = _actionError.asStateFlow()

    val uiState: StateFlow<UiState<GoalDetailData>> = combine(observeGoal(), getProgress(), whatIf) { goal, progress, amount ->
        when {
            goal == null -> UiState.Empty
            progress == null -> UiState.Loading
            else -> UiState.Success(toData(goal, progress, amount))
        }
    }.catch { emit(UiState.Error(mapErrorToMessage(it))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun onWhatIfChange(value: Long) = whatIf.update { value.coerceAtLeast(0L) }
    fun resetWhatIf() = whatIf.update { null }

    fun moveDeadline() = updateGoal { data -> data.goal.copy(deadline = data.progress.suggestedDeadline ?: data.goal.deadline) }
    fun lowerTarget() = updateGoal { data -> data.goal.copy(targetLkrMinor = data.progress.suggestedTargetMinor ?: data.goal.targetLkrMinor) }

    private fun updateGoal(change: (GoalDetailData) -> Goal) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            when (val result = saveGoal(change(data))) {
                is Result.Success -> Unit
                is Result.Error -> _actionError.value = mapErrorToMessage(result.exception)
            }
        }
    }

    private fun toData(goal: Goal, progress: GoalProgress, amount: Long?): GoalDetailData {
        val whatIfMinor = amount ?: progress.actualPaceMinor ?: progress.requiredMonthlyMinor
        val date = project(progress.remainingMinor, whatIfMinor)
        val status = when {
            progress.isInfeasible && amount == null -> GoalDetailStatus.NEEDS_ADJUSTING
            amount != null && amount >= progress.requiredMonthlyMinor -> GoalDetailStatus.ON_TRACK
            amount != null -> GoalDetailStatus.BEHIND
            else -> when (progress.status) {
                GoalStatus.AHEAD -> GoalDetailStatus.AHEAD
                GoalStatus.ON_TRACK -> GoalDetailStatus.ON_TRACK
                GoalStatus.BEHIND -> GoalDetailStatus.BEHIND
                GoalStatus.NOT_ENOUGH_DATA -> GoalDetailStatus.NOT_ENOUGH_DATA
            }
        }
        return GoalDetailData(
            goal,
            progress,
            (progress.savedToDateMinor * 100 / goal.targetLkrMinor.coerceAtLeast(1L)).coerceIn(0L, 100L).toInt(),
            (progress.savedToDateMinor.toFloat() / goal.targetLkrMinor.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f),
            whatIfMinor,
            maxOf(100_000L, progress.requiredMonthlyMinor * 3L),
            date,
            status,
            (progress.requiredMonthlyMinor - whatIfMinor).coerceAtLeast(0L),
        )
    }

    private fun project(remaining: Long, monthly: Long): LocalDate? {
        if (monthly <= 0L) return null
        val months = (remaining + monthly - 1L) / monthly
        return LocalDate.now(clock).plusMonths(months)
    }
}
