package com.fundtrail.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.auth.ConfirmPasswordResetUseCase
import com.fundtrail.ui.common.mapErrorToMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfirmResetPasswordState(
    val password: String = "",
    val confirmPassword: String = "",
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmError: Int? = null,
    val isLoading: Boolean = false,
    @StringRes val error: Int? = null,
    val done: Boolean = false,
)

/** [onSubmit] takes the email link's `oobCode` (ui/navigation/FundtrailNavHost.kt), not stored here. */
@HiltViewModel
class ConfirmResetPasswordViewModel @Inject constructor(
    private val confirmPasswordReset: ConfirmPasswordResetUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfirmResetPasswordState())
    val uiState: StateFlow<ConfirmResetPasswordState> = _uiState.asStateFlow()

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, passwordError = null, error = null) }

    fun onConfirmChange(value: String) = _uiState.update { it.copy(confirmPassword = value, confirmError = null, error = null) }

    fun onSubmit(oobCode: String) {
        val s = _uiState.value
        if (s.isLoading) return
        val passwordError = validateNewPassword(s.password)
        val confirmError = if (s.confirmPassword != s.password) R.string.auth_error_password_mismatch else null
        if (passwordError != null || confirmError != null) {
            _uiState.update { it.copy(passwordError = passwordError, confirmError = confirmError) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = confirmPasswordReset(oobCode, s.password)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, done = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = mapErrorToMessage(result.exception)) }
            }
        }
    }
}
