package com.fundtrail.ui.more

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.UserSettings
import com.fundtrail.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class IncomeSettingsUiState(
    val lowEstimateInput: String = "",
    val highEstimateInput: String = "",
    val isLoading: Boolean = false,
    @StringRes val errorResId: Int? = null,
    val isSaved: Boolean = false,
)

@HiltViewModel
class IncomeSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncomeSettingsUiState())
    val uiState: StateFlow<IncomeSettingsUiState> = _uiState.asStateFlow()

    private var currentSettings: UserSettings = UserSettings()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.observe().first()
            currentSettings = settings
            _uiState.update {
                it.copy(
                    lowEstimateInput = formatMinorToMajorInput(settings.incomeEstimateMinMinor),
                    highEstimateInput = formatMinorToMajorInput(settings.incomeEstimateMaxMinor),
                )
            }
        }
    }

    fun onLowEstimateChanged(input: String) {
        _uiState.update { it.copy(lowEstimateInput = input, errorResId = null) }
    }

    fun onHighEstimateChanged(input: String) {
        _uiState.update { it.copy(highEstimateInput = input, errorResId = null) }
    }

    fun onSave() {
        val lowText = _uiState.value.lowEstimateInput.trim()
        val highText = _uiState.value.highEstimateInput.trim()

        val lowMinor = parseLkrToMinor(lowText)
        if (lowText.isNotBlank() && (lowMinor == null)) {
            _uiState.update { it.copy(errorResId = R.string.income_estimate_error_invalid_number) }
            return
        }

        val highMinor = parseLkrToMinor(highText)
        if (highText.isNotBlank() && (highMinor == null)) {
            _uiState.update { it.copy(errorResId = R.string.income_estimate_error_invalid_number) }
            return
        }

        if ((lowMinor != null) && (highMinor != null) && (lowMinor > highMinor)) {
            _uiState.update { it.copy(errorResId = R.string.income_estimate_error_min_greater_than_max) }
            return
        }

        val updated = currentSettings.copy(
            incomeEstimateMinMinor = lowMinor,
            incomeEstimateMaxMinor = highMinor,
        )

        _uiState.update { it.copy(isLoading = true, errorResId = null) }
        viewModelScope.launch {
            when (settingsRepository.update(updated)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, isSaved = true) }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorResId = R.string.income_estimate_error_min_greater_than_max,
                    )
                }
            }
        }
    }

    fun onHandledSaved() {
        _uiState.update { it.copy(isSaved = false) }
    }

    companion object {
        internal fun parseLkrToMinor(input: String): Long? {
            val clean = input.replace(",", "").trim()
            if (clean.isBlank()) return null
            val bd = clean.toBigDecimalOrNull() ?: return null
            if (bd < BigDecimal.ZERO) return null
            return bd.movePointRight(2).toLong()
        }

        internal fun formatMinorToMajorInput(minor: Long?): String {
            if (minor == null) return ""
            val major = BigDecimal.valueOf(minor, 2).stripTrailingZeros()
            return major.toPlainString()
        }
    }
}
