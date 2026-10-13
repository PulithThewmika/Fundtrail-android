package com.fundtrail.ui.common

import androidx.annotation.StringRes
import com.fundtrail.R
import com.fundtrail.domain.model.SyncStatus

/**
 * UI state sealed class covering Loading, Success, Empty, and Error states for load-and-display screens.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(@StringRes val message: Int) : UiState<Nothing>
}

/** Maps a screen's data and the sync status to one [UiState], the same way on every screen (architecture.md §4.6). */
fun <T> uiStateOf(hasData: Boolean, sync: SyncStatus, content: () -> T): UiState<T> = when {
    hasData -> UiState.Success(content())
    sync.initialSyncDone -> UiState.Empty
    sync.isOnline -> UiState.Loading
    else -> UiState.Error(R.string.offline_no_data)
}
