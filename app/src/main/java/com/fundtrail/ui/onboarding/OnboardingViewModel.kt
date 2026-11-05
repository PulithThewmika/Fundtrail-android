package com.fundtrail.ui.onboarding

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.account.AddAccountUseCase
import com.fundtrail.domain.usecase.goal.SaveGoalUseCase
import com.fundtrail.domain.usecase.settings.UpdateIncomeEstimateUseCase
import com.fundtrail.ui.common.mapErrorToMessage
import com.fundtrail.ui.more.IncomeSettingsViewModel
import com.fundtrail.ui.quickadd.AmountInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

enum class OnboardingStep { GOAL, ACCOUNT, INCOME }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.GOAL,
    val isSaving: Boolean = false,
    val finished: Boolean = false,
    // Goal step (FR-75)
    val goalName: String = "",
    val goalTarget: String = "",
    val goalSaved: String = "",
    @StringRes val goalError: Int? = null,
    // Account step (FR-11)
    val accountName: String = "",
    val accountKind: AccountKind = AccountKind.BANK,
    @StringRes val accountError: Int? = null,
    // Income estimate step (FR-08, optional)
    val incomeLow: String = "",
    val incomeHigh: String = "",
    @StringRes val incomeError: Int? = null,
)

/**
 * The three skippable onboarding steps FR-08 asks for: goal, accounts, optional income estimate.
 * Each step saves through the same use case the full screen would, just with compact inline fields
 * instead of embedding Goal setup / Accounts / Income settings wholesale - those carry management
 * features (archive, rename, multiple accounts) that don't belong in a first-run wizard (#323).
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val saveGoal: SaveGoalUseCase,
    private val addAccount: AddAccountUseCase,
    private val updateIncomeEstimate: UpdateIncomeEstimateUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val today get() = LocalDate.now(clock.withZone(APP_ZONE))

    fun onGoalNameChange(value: String) = _uiState.update { it.copy(goalName = value, goalError = null) }
    fun onGoalTargetChange(value: String) = _uiState.update { it.copy(goalTarget = AmountInput.sanitize(value), goalError = null) }
    fun onGoalSavedChange(value: String) = _uiState.update { it.copy(goalSaved = AmountInput.sanitize(value), goalError = null) }

    fun onGoalNext() {
        val state = _uiState.value
        val target = AmountInput.toMinor(state.goalTarget)
        val saved = AmountInput.toMinor(state.goalSaved)
        if (state.goalName.isBlank() || target == null || saved == null) {
            _uiState.update { it.copy(goalError = R.string.goal_setup_amount_required) }
            return
        }
        val goal = Goal(UUID.randomUUID().toString(), state.goalName.trim(), target, today.plusMonths(12), saved)
        try {
            goal.validate(today = today, zone = APP_ZONE)
        } catch (error: IllegalArgumentException) {
            _uiState.update { it.copy(goalError = mapErrorToMessage(error)) }
            return
        }
        _uiState.update { it.copy(isSaving = true, goalError = null) }
        viewModelScope.launch {
            when (val result = saveGoal(goal)) {
                is Result.Success -> advanceTo(OnboardingStep.ACCOUNT)
                is Result.Error -> _uiState.update { it.copy(isSaving = false, goalError = mapErrorToMessage(result.exception)) }
            }
        }
    }

    fun onGoalSkip() = advanceTo(OnboardingStep.ACCOUNT)

    fun onAccountNameChange(value: String) = _uiState.update { it.copy(accountName = value, accountError = null) }
    fun onAccountKindChange(value: AccountKind) = _uiState.update { it.copy(accountKind = value) }

    fun onAccountNext() {
        val state = _uiState.value
        if (state.accountName.isBlank()) {
            _uiState.update { it.copy(accountError = R.string.accounts_error_name_blank) }
            return
        }
        _uiState.update { it.copy(isSaving = true, accountError = null) }
        viewModelScope.launch {
            when (val result = addAccount(state.accountName, state.accountKind, "LKR")) {
                is Result.Success -> advanceTo(OnboardingStep.INCOME)
                is Result.Error -> _uiState.update { it.copy(isSaving = false, accountError = mapErrorToMessage(result.exception)) }
            }
        }
    }

    fun onAccountSkip() = advanceTo(OnboardingStep.INCOME)

    fun onIncomeLowChange(value: String) = _uiState.update { it.copy(incomeLow = value, incomeError = null) }
    fun onIncomeHighChange(value: String) = _uiState.update { it.copy(incomeHigh = value, incomeError = null) }

    fun onIncomeNext() {
        val state = _uiState.value
        val lowText = state.incomeLow.trim()
        val highText = state.incomeHigh.trim()
        val low = IncomeSettingsViewModel.parseLkrToMinor(lowText)
        if (lowText.isNotBlank() && low == null) {
            _uiState.update { it.copy(incomeError = R.string.income_estimate_error_invalid_number) }
            return
        }
        val high = IncomeSettingsViewModel.parseLkrToMinor(highText)
        if (highText.isNotBlank() && high == null) {
            _uiState.update { it.copy(incomeError = R.string.income_estimate_error_invalid_number) }
            return
        }
        if (low != null && high != null && low > high) {
            _uiState.update { it.copy(incomeError = R.string.income_estimate_error_min_greater_than_max) }
            return
        }
        if (low == null && high == null) {
            _uiState.update { it.copy(finished = true) }
            return
        }
        _uiState.update { it.copy(isSaving = true, incomeError = null) }
        viewModelScope.launch {
            when (val result = updateIncomeEstimate(low, high)) {
                is Result.Success -> _uiState.update { it.copy(isSaving = false, finished = true) }
                is Result.Error -> _uiState.update { it.copy(isSaving = false, incomeError = mapErrorToMessage(result.exception)) }
            }
        }
    }

    fun onIncomeSkip() = _uiState.update { it.copy(finished = true) }

    private fun advanceTo(step: OnboardingStep) = _uiState.update { it.copy(step = step, isSaving = false) }
}
