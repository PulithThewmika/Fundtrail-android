package com.fundtrail.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.model.ReminderSettings
import com.fundtrail.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.fundtrail.domain.usecase.reminder.SetReminderEnabledUseCase
import com.fundtrail.domain.usecase.reminder.SetReminderTimeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderUiState(
    val settings: ReminderSettings = ReminderSettings(),
    /** True after the user turned the toggle on but notifications were denied; the screen shows an explanation. */
    val permissionDenied: Boolean = false,
)

@HiltViewModel
class ReminderViewModel @Inject constructor(
    observeSettings: ObserveReminderSettingsUseCase,
    private val setEnabled: SetReminderEnabledUseCase,
    private val setTime: SetReminderTimeUseCase,
) : ViewModel() {

    private val permissionDenied = MutableStateFlow(false)

    val uiState: StateFlow<ReminderUiState> =
        combine(observeSettings(), permissionDenied) { settings, denied ->
            ReminderUiState(settings = settings, permissionDenied = denied)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderUiState())

    /** The screen calls this only once the POST_NOTIFICATIONS permission is granted (or not needed). */
    fun onEnableGranted() {
        permissionDenied.update { false }
        viewModelScope.launch { setEnabled(true) }
    }

    /** API 33+ denied the permission: leave the toggle off and surface the explanation (#221). */
    fun onEnableDenied() {
        permissionDenied.update { true }
        viewModelScope.launch { setEnabled(false) }
    }

    fun onDisable() {
        permissionDenied.update { false }
        viewModelScope.launch { setEnabled(false) }
    }

    fun onTimeChange(hour: Int, minute: Int) {
        viewModelScope.launch { setTime(hour * 60 + minute) }
    }
}
