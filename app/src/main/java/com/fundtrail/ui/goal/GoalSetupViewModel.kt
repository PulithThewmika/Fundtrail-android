package com.fundtrail.ui.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.goal.SaveGoalUseCase
import com.fundtrail.ui.common.mapErrorToMessage
import com.fundtrail.ui.quickadd.AmountInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

data class GoalSetupUiState(
    val name: String = "MacBook Pro M4",
    val target: String = AmountInput.fromMinor(49_000_000L),
    val deadline: LocalDate = LocalDate.now().plusMonths(12),
    val alreadySaved: String = AmountInput.fromMinor(1_120_000L),
    val requiredMonthlyMinor: Long = 3_990_000L,
    val isSaving: Boolean = false,
    val errorMessage: Int? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class GoalSetupViewModel @Inject constructor(
    private val saveGoal: SaveGoalUseCase,
    private val clock: Clock,
) : ViewModel() {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val today get() = LocalDate.now(clock.withZone(zone))
    private val _uiState = MutableStateFlow(GoalSetupUiState(deadline = today.plusMonths(12)))
    val uiState: StateFlow<GoalSetupUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }
    fun onTargetChange(value: String) = _uiState.update { state ->
        state.copy(target = AmountInput.sanitize(value), requiredMonthlyMinor = required(state.copy(target = AmountInput.sanitize(value))))
    }
    fun onAlreadySavedChange(value: String) = _uiState.update { state ->
        state.copy(alreadySaved = AmountInput.sanitize(value), requiredMonthlyMinor = required(state.copy(alreadySaved = AmountInput.sanitize(value))))
    }
    fun onDeadlineChange(value: LocalDate) = _uiState.update { state ->
        state.copy(deadline = value, requiredMonthlyMinor = required(state.copy(deadline = value)), errorMessage = null)
    }

    fun save() {
        val state = uiState.value
        val target = AmountInput.toMinor(state.target)
        val saved = AmountInput.toMinor(state.alreadySaved)
        if (target == null || saved == null) {
            _uiState.update { it.copy(errorMessage = com.fundtrail.R.string.goal_setup_amount_required) }
            return
        }
        val goal = Goal(UUID.randomUUID().toString(), state.name.trim(), target, state.deadline, saved)
        try {
            goal.validate(today = today, zone = zone)
        } catch (error: IllegalArgumentException) {
            _uiState.update { it.copy(errorMessage = mapErrorToMessage(error)) }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = saveGoal(goal)) {
                is Result.Success -> _uiState.update { it.copy(isSaving = false, saved = true) }
                is Result.Error -> _uiState.update { it.copy(isSaving = false, errorMessage = mapErrorToMessage(result.exception)) }
            }
        }
    }

    private fun required(state: GoalSetupUiState): Long {
        val target = AmountInput.toMinor(state.target) ?: return 0L
        val saved = AmountInput.toMinor(state.alreadySaved) ?: return 0L
        val months = ChronoUnit.MONTHS.between(YearMonthCompat.of(today), YearMonthCompat.of(state.deadline)).coerceAtLeast(1L)
        return ((target - saved).coerceAtLeast(0L) + months - 1L) / months
    }
}

private object YearMonthCompat {
    fun of(date: LocalDate) = java.time.YearMonth.from(date)
}
