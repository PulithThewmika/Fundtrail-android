package com.fundtrail.ui.more

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.auth.DeleteAccountUseCase
import com.fundtrail.ui.common.mapErrorToMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeleteAccountState(
    val isLoading: Boolean = false,
    val confirming: Boolean = false,
    @StringRes val error: Int? = null,
    /** One-off: the UI navigates to the auth graph, then calls [onNavigated]. */
    val deleted: Boolean = false,
)

/** FR-117: delete the account and all its data. */
@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val deleteAccount: DeleteAccountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeleteAccountState())
    val uiState: StateFlow<DeleteAccountState> = _uiState.asStateFlow()

    fun onDeleteClick() = _uiState.update { it.copy(confirming = true, error = null) }
    fun onDismissConfirm() = _uiState.update { it.copy(confirming = false) }
    fun onNavigated() = _uiState.update { it.copy(deleted = false) }

    fun onConfirmDelete() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, confirming = false, error = null) }
        viewModelScope.launch {
            when (val result = deleteAccount()) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, deleted = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = mapErrorToMessage(result.exception)) }
            }
        }
    }
}
