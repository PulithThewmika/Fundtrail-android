package com.fundtrail.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.auth.SignInUseCase
import com.fundtrail.ui.common.mapErrorToMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInState(
    val email: String = "",
    val password: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    val isLoading: Boolean = false,
    @StringRes val error: Int? = null,
    /** One-off: the UI navigates, then calls [SignInViewModel.onNavigated]. */
    val signedIn: Boolean = false,
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signIn: SignInUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInState())
    val uiState: StateFlow<SignInState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, emailError = null, error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, passwordError = null, error = null) }

    fun onSubmit() {
        val s = _uiState.value
        if (s.isLoading) return
        val emailError = validateEmail(s.email)
        val passwordError = if (s.password.isEmpty()) R.string.auth_error_password_required else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = signIn(s.email, s.password)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, signedIn = true) }
                // The email stays filled in so only the password is retyped (FR-02).
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = mapErrorToMessage(result.exception)) }
            }
        }
    }

    fun onNavigated() = _uiState.update { it.copy(signedIn = false) }
}
