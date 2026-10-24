package com.fundtrail.ui.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.model.APP_ZONE
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class GoalDetailStatus { AHEAD, ON_TRACK, BEHIND, NOT_ENOUGH_DATA, NEEDS_ADJUSTING }
data class GoalDetailUiState(
    val content: UiState<GoalDetailData> = UiState.Loading,
    val actionError: Int? = null,
)

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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    observeGoal: ObserveActiveGoalUseCase,
    getProgress: GetGoalProgressUseCase,
    private val saveGoal: SaveGoalUseCase,
    private val clock: Clock,
) : ViewModel() {
    private val whatIf = MutableStateFlow<Long?>(null)
    private val retryTrigger = MutableStateFlow(0)
    private val _actionError = MutableStateFlow<Int?>(null)
    private val saveMutex = Mutex()
    private var pendingGoal: Goal? = null

    private val contentState: kotlinx.coroutines.flow.Flow<UiState<GoalDetailData>> = retryTrigger
        .flatMapLatest {
            observeGoal().flatMapLatest { goal ->
                if (goal == null) {
                    flowOf(UiState.Empty)
                } else {
                    combine(getProgress(goal), whatIf) { progress, amount ->
                        UiState.Success(toData(goal, progress, amount))
                    }
                }
            }.catch { emit(UiState.Error(mapErrorToMessage(it))) }
        }

    val uiState: StateFlow<GoalDetailUiState> = combine(contentState, _actionError) { content, actionError ->
        GoalDetailUiState(content, actionError)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalDetailUiState())

    fun onWhatIfChange(value: Long) = whatIf.update { value.coerceAtLeast(0L) }
    fun resetWhatIf() = whatIf.update { null }
    fun onRetry() = retryTrigger.update { it + 1 }
    fun onActionErrorShown() = _actionError.update { null }

    fun moveDeadline() = updateGoal { data -> data.goal.copy(deadline = data.progress.suggestedDeadline ?: data.goal.deadline) }
    fun lowerTarget() = updateGoal { data -> data.goal.copy(targetLkrMinor = data.progress.suggestedTargetMinor ?: data.goal.targetLkrMinor) }

    private fun updateGoal(change: (GoalDetailData) -> Goal) {
        onActionErrorShown()
        viewModelScope.launch {
            saveMutex.withLock {
                val data = (uiState.value.content as? UiState.Success<GoalDetailData>)?.data ?: return@withLock
                // Writes go to Firestore and come back through sync, so Room can still hold the
                // pre-save goal here. Base the change on the last goal we saved, guarded by id so a
                // stale copy can never leak onto a different goal.
                val base = pendingGoal?.takeIf { it.id == data.goal.id } ?: data.goal
                val nextGoal = change(data.copy(goal = base))
                when (val result = saveGoal(nextGoal)) {
                    is Result.Success -> pendingGoal = nextGoal
                    is Result.Error -> {
                        pendingGoal = null
                        _actionError.value = mapErrorToMessage(result.exception)
                    }
                }
            }
        }
    }

    private fun toData(goal: Goal, progress: GoalProgress, amount: Long?): GoalDetailData {
        val whatIfMinor = amount ?: progress.actualPaceMinor ?: progress.requiredMonthlyMinor
        // An explicit what-if of 0 projects nothing; only an unset what-if falls back to the
        // goal's own projection, so the elvis operator cannot do this.
        val date = if (amount != null) project(progress.remainingMinor, amount) else progress.projectedDate
        val status = when {
            progress.isInfeasible && amount == null -> GoalDetailStatus.NEEDS_ADJUSTING
            amount != null && amount >= aheadThreshold(progress.requiredMonthlyMinor) -> GoalDetailStatus.AHEAD
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
            maxOf(100_000L, progress.requiredMonthlyMinor * 3L, whatIfMinor.coerceAtLeast(0L)),
            date,
            status,
            if (status == GoalDetailStatus.AHEAD) {
                (whatIfMinor - progress.requiredMonthlyMinor).coerceAtLeast(0L)
            } else {
                (progress.requiredMonthlyMinor - whatIfMinor).coerceAtLeast(0L)
            },
        )
    }

    private fun project(remaining: Long, monthly: Long): LocalDate? {
        if (monthly <= 0L) return null
        val months = (remaining + monthly - 1L) / monthly
        return LocalDate.now(clock.withZone(APP_ZONE)).plusMonths(months)
    }

    private fun aheadThreshold(requiredMonthlyMinor: Long): Long =
        BigDecimal.valueOf(requiredMonthlyMinor)
            .multiply(BigDecimal("1.1"))
            .setScale(0, RoundingMode.CEILING)
            .toLong()
}
