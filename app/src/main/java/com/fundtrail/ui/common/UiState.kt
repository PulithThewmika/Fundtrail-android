package com.fundtrail.ui.common

import androidx.annotation.StringRes

/**
 * UI state sealed class covering Loading, Success, Empty, and Error states for load-and-display screens.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(@StringRes val message: Int) : UiState<Nothing>
}
