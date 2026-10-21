package com.fundtrail.ui.more

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.ScreenLockRequiredException
import com.fundtrail.domain.usecase.security.ObserveAppLockEnabledUseCase
import com.fundtrail.domain.usecase.security.SetAppLockEnabledUseCase
import com.fundtrail.ui.common.mapErrorToMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecuritySettingsUiState(
    val appLockEnabled: Boolean = false,
    @StringRes val message: Int? = null,
)

/** More → Security → App lock (#222). */
@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    observeAppLockEnabled: ObserveAppLockEnabledUseCase,
    private val setAppLockEnabled: SetAppLockEnabledUseCase,
) : ViewModel() {

    private val message = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<SecuritySettingsUiState> =
        combine(observeAppLockEnabled(), message) { enabled, msg -> SecuritySettingsUiState(enabled, msg) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SecuritySettingsUiState())

    fun onAppLockToggled(enabled: Boolean) {
        message.value = null
        viewModelScope.launch {
            val result = setAppLockEnabled(enabled)
            if (result is Result.Error) {
                message.value = if (result.exception is ScreenLockRequiredException) {
                    R.string.app_lock_needs_screen_lock
                } else {
                    mapErrorToMessage(result.exception)
                }
            }
        }
    }
}
