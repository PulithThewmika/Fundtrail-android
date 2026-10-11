package com.fundtrail.ui.more

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SignOutOutcome
import com.fundtrail.domain.usecase.auth.SignOutUseCase
import com.fundtrail.ui.common.mapErrorToMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignOutState(
    val isLoading: Boolean = false,
    val confirmUnsynced: Boolean = false,
    @StringRes val error: Int? = null,
    /** One-off: the UI navigates to the auth graph, then calls [SignOutViewModel.onNavigated]. */
    val signedOut: Boolean = false,
)

@HiltViewModel
class SignOutViewModel @Inject constructor(
    private val signOut: SignOutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignOutState())
    val uiState: StateFlow<SignOutState> = _uiState.asStateFlow()

    fun onSignOutClick() = run(force = false)

    /** The user accepted losing unsynced changes (DLG-10). */
    fun onConfirmSignOut() = run(force = true)

    fun onDismissConfirm() = _uiState.update { it.copy(confirmUnsynced = false) }

    fun onNavigated() = _uiState.update { it.copy(signedOut = false) }

    private fun run(force: Boolean) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, confirmUnsynced = false, error = null) }
        viewModelScope.launch {
            when (val result = signOut(force)) {
                is Result.Success -> _uiState.update {
                    when (result.data) {
                        SignOutOutcome.SIGNED_OUT -> it.copy(isLoading = false, signedOut = true)
                        SignOutOutcome.HAS_UNSYNCED_WRITES -> it.copy(isLoading = false, confirmUnsynced = true)
                    }
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = mapErrorToMessage(result.exception)) }
            }
        }
    }
}
