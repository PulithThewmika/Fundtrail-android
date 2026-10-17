package com.fundtrail.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.usecase.auth.ObserveCurrentUserIdUseCase
import com.fundtrail.domain.usecase.sync.ObserveSyncStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Start-up gate (screens.md UX-01): null while Firebase restores the session, then signed-in or not. */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeCurrentUserId: ObserveCurrentUserIdUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
) : ViewModel() {
    val signedIn: StateFlow<Boolean?> = observeCurrentUserId()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Drives the app-wide offline banner (FR-110). Starts as online so the banner never flashes on launch. */
    val isOnline: StateFlow<Boolean> = observeSyncStatus()
        .map { it.isOnline }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
}
