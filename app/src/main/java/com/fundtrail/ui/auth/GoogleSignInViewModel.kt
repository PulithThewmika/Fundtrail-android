package com.fundtrail.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.EmailUsesPasswordSignInException
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.auth.SignInWithGoogleUseCase
import com.fundtrail.ui.common.mapErrorToMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoogleSignInState(
    val isLoading: Boolean = false,
    @StringRes val error: Int? = null,
    /** One-off: the UI navigates, then calls [GoogleSignInViewModel.onNavigated]. */
    val signedIn: Boolean = false,
)

/** "Continue with Google" on Sign in and Sign up (#213). The screen runs the account picker and reports back here. */
@HiltViewModel
class GoogleSignInViewModel @Inject constructor(
    private val signInWithGoogle: SignInWithGoogleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoogleSignInState())
    val uiState: StateFlow<GoogleSignInState> = _uiState.asStateFlow()

    /** Returns false when a sign-in is already running, so the screen doesn't open a second picker. */
    fun onPickerStarted(): Boolean {
        if (_uiState.value.isLoading) return false
        _uiState.update { it.copy(isLoading = true, error = null) }
        return true
    }

    fun onPickerResult(result: GoogleIdTokenResult) {
        when (result) {
            is GoogleIdTokenResult.Token -> viewModelScope.launch {
                _uiState.update {
                    when (val signedIn = signInWithGoogle(result.idToken)) {
                        is Result.Success -> it.copy(isLoading = false, signedIn = true)
                        is Result.Error -> it.copy(isLoading = false, error = messageFor(signedIn.exception))
                    }
                }
            }
            // Closing the picker is a choice, not a failure: back to the form with nothing shown.
            GoogleIdTokenResult.Cancelled -> _uiState.update { it.copy(isLoading = false) }
            GoogleIdTokenResult.NoAccount -> _uiState.update { it.copy(isLoading = false, error = R.string.auth_google_no_account) }
            GoogleIdTokenResult.NotConfigured -> _uiState.update { it.copy(isLoading = false, error = R.string.auth_google_not_configured) }
            is GoogleIdTokenResult.Failed -> _uiState.update { it.copy(isLoading = false, error = mapErrorToMessage(result.error)) }
        }
    }

    fun onNavigated() = _uiState.update { it.copy(signedIn = false) }

    private fun messageFor(error: Throwable): Int =
        if (error is EmailUsesPasswordSignInException) R.string.auth_google_email_uses_password else mapErrorToMessage(error)
}
