package com.fundtrail.ui.common

/**
 * UI state sealed class covering Loading, Success, Empty, and Error states for load-and-display screens.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>
}
