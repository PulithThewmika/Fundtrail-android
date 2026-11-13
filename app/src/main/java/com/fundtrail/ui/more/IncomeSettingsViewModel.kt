package com.fundtrail.ui.more

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.settings.ObserveSettingsUseCase
import com.fundtrail.domain.usecase.settings.UpdateIncomeSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class IncomeSettingsUiState(
    val lowEstimateInput: String = "",
    val highEstimateInput: String = "",
    val salaryInput: String = "",
    val paydayInput: String = "",
    val isLoading: Boolean = false,
    @StringRes val errorResId: Int? = null,
    val isSaved: Boolean = false,
    /** False until the stored settings have been read, so an empty form isn't mistaken for "nothing set". */
    val isLoaded: Boolean = false,
) {
    val nothingSet: Boolean
        get() = isLoaded && salaryInput.isBlank() && lowEstimateInput.isBlank() && highEstimateInput.isBlank()
}

@HiltViewModel
class IncomeSettingsViewModel @Inject constructor(
    private val observeSettings: ObserveSettingsUseCase,
    private val updateIncomeSettings: UpdateIncomeSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncomeSettingsUiState())
    val uiState: StateFlow<IncomeSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = observeSettings().first()
            _uiState.update {
                it.copy(
                    lowEstimateInput = formatMinorToMajorInput(settings.incomeEstimateMinMinor),
                    highEstimateInput = formatMinorToMajorInput(settings.incomeEstimateMaxMinor),
                    salaryInput = formatMinorToMajorInput(settings.expectedSalaryMinor),
                    paydayInput = settings.salaryPayday.toString(),
                    isLoaded = true,
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

    fun onSalaryChanged(input: String) {
        _uiState.update { it.copy(salaryInput = input, errorResId = null) }
    }

    fun onPaydayChanged(input: String) {
        _uiState.update { it.copy(paydayInput = input.filter(Char::isDigit).take(2), errorResId = null) }
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

        val salaryText = _uiState.value.salaryInput.trim()
        val salaryMinor = parseLkrToMinor(salaryText)
        if (salaryText.isNotBlank() && salaryMinor == null) {
            _uiState.update { it.copy(errorResId = R.string.income_estimate_error_invalid_number) }
            return
        }

        val payday = _uiState.value.paydayInput.toIntOrNull()
        if (payday == null || payday !in 1..28) {
            _uiState.update { it.copy(errorResId = R.string.income_settings_error_payday) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorResId = null) }
        viewModelScope.launch {
            when (updateIncomeSettings(lowMinor, highMinor, salaryMinor, payday)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, isSaved = true) }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorResId = R.string.error_generic,
                    )
                }
            }
        }
    }

    fun onHandledSaved() {
        _uiState.update { it.copy(isSaved = false) }
    }

    fun onDismissed() {
        viewModelScope.launch {
            val settings = observeSettings().first()
            _uiState.update {
                it.copy(
                    lowEstimateInput = formatMinorToMajorInput(settings.incomeEstimateMinMinor),
                    highEstimateInput = formatMinorToMajorInput(settings.incomeEstimateMaxMinor),
                    salaryInput = formatMinorToMajorInput(settings.expectedSalaryMinor),
                    paydayInput = settings.salaryPayday.toString(),
                    errorResId = null,
                    isLoading = false,
                )
            }
        }
    }

    companion object {
        internal fun parseLkrToMinor(input: String): Long? {
            val clean = input.replace(",", "").trim()
            if (clean.isBlank()) return null
            val bd = clean.toBigDecimalOrNull() ?: return null
            if (bd < BigDecimal.ZERO) return null
            return try {
                bd.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
            } catch (_: ArithmeticException) {
                null
            }
        }

        internal fun formatMinorToMajorInput(minor: Long?): String {
            if (minor == null) return ""
            val major = BigDecimal.valueOf(minor, 2).stripTrailingZeros()
            return major.toPlainString()
        }
    }
}
