package com.fundtrail.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.usecase.security.ObserveAppLockEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import javax.inject.Inject

/**
 * @property resolved False until the setting has been read, so neither the app nor the lock flashes on start.
 * @property enabled The app lock setting; while on, the window is also FLAG_SECURE.
 * @property locked Cover the app and ask to unlock.
 */
data class AppLockUiState(
    val resolved: Boolean = false,
    val enabled: Boolean = false,
    val locked: Boolean = false,
)

/**
 * App lock state for the whole app (#222). Locks on a cold start, and again when the app comes back after more
 * than [LOCK_AFTER_MILLIS] in the background. The activity reports background/foreground from
 * `ProcessLifecycleOwner` and a successful prompt through [onUnlocked].
 */
@HiltViewModel
class AppLockViewModel @Inject constructor(
    observeAppLockEnabled: ObserveAppLockEnabledUseCase,
    private val clock: Clock,
) : ViewModel() {

    /** True from a cold start, so the first value of an enabled lock is locked. */
    private val lockRequested = MutableStateFlow(true)
    private var backgroundedAt: Long? = null
    private var enabledNow = false

    val uiState: StateFlow<AppLockUiState> = combine(
        observeAppLockEnabled().onEach { enabled ->
            enabledNow = enabled
            // Turning the lock on mid-session mustn't lock straight away: only a cold start or a long background does.
            if (!enabled) lockRequested.value = false
        },
        lockRequested,
    ) { enabled, requested ->
        AppLockUiState(resolved = true, enabled = enabled, locked = enabled && requested)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppLockUiState())

    fun onBackgrounded() {
        backgroundedAt = clock.millis()
    }

    fun onForegrounded() {
        val since = backgroundedAt ?: return
        backgroundedAt = null
        if (enabledNow && clock.millis() - since > LOCK_AFTER_MILLIS) lockRequested.value = true
    }

    fun onUnlocked() {
        lockRequested.value = false
    }

    companion object {
        const val LOCK_AFTER_MILLIS = 60_000L
    }
}
