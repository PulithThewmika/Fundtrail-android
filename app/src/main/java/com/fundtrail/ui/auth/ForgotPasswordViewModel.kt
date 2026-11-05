package com.fundtrail.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.auth.SendPasswordResetEmailUseCase
import com.fundtrail.ui.common.mapErrorToMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordState(
    val email: String = "",
    @StringRes val emailError: Int? = null,
    val isLoading: Boolean = false,
    @StringRes val error: Int? = null,
    val emailSent: Boolean = false,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val sendPasswordResetEmail: SendPasswordResetEmailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordState())
    val uiState: StateFlow<ForgotPasswordState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, emailError = null, error = null) }

    fun onSubmit() {
        val s = _uiState.value
        if (s.isLoading) return
        val emailError = validateEmail(s.email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = sendPasswordResetEmail(s.email)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, emailSent = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = mapErrorToMessage(result.exception)) }
            }
        }
    }
}
