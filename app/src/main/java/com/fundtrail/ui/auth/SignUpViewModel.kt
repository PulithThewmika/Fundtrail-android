package com.fundtrail.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.auth.RegisterUseCase
import com.fundtrail.domain.repository.AccountRepository
import com.fundtrail.ui.common.mapErrorToMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmError: Int? = null,
    val isLoading: Boolean = false,
    @StringRes val error: Int? = null,
    val seedRetryRequired: Boolean = false,
    /** One-off: the UI navigates, then calls [SignUpViewModel.onNavigated]. */
    val registered: Boolean = false,
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val register: RegisterUseCase,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpState())
    val uiState: StateFlow<SignUpState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, emailError = null, error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, passwordError = null, error = null) }

    fun onConfirmChange(value: String) = _uiState.update { it.copy(confirmPassword = value, confirmError = null, error = null) }

    fun onSubmit() {
        val s = _uiState.value
        if (s.isLoading) return
        if (s.seedRetryRequired) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                when (val seedResult = accountRepository.seedDefaultsIfEmpty()) {
                    is Result.Success -> _uiState.update {
                        it.copy(isLoading = false, seedRetryRequired = false, registered = true)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = mapErrorToMessage(seedResult.exception),
                        )
                    }
                }
            }
            return
        }

        val emailError = validateEmail(s.email)
        val passwordError = validateNewPassword(s.password)
        val confirmError = if (s.confirmPassword != s.password) R.string.auth_error_password_mismatch else null
        if (emailError != null || passwordError != null || confirmError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError, confirmError = confirmError) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = register(s.email, s.password)) {
                is Result.Success -> {
                    when (val seedResult = accountRepository.seedDefaultsIfEmpty()) {
                        is Result.Success -> _uiState.update {
                            it.copy(isLoading = false, registered = true)
                        }
                        is Result.Error -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = mapErrorToMessage(seedResult.exception),
                                seedRetryRequired = true,
                            )
                        }
                    }
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = mapErrorToMessage(result.exception)) }
            }
        }
    }

    fun onNavigated() = _uiState.update { it.copy(registered = false, seedRetryRequired = false) }
}
