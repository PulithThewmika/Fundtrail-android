package com.fundtrail.ui.common

import java.io.IOException
import java.util.concurrent.TimeoutException

/**
 * Centralized error-to-message mapping function as specified in EPIC-006/T4 S1.
 * Maps exceptions to user-friendly error messages in one place rather than per-screen.
 */
fun mapErrorToMessage(throwable: Throwable): String {
    return when (throwable) {
        is IOException, is TimeoutException -> "Network connection error. Please check your connection and try again."
        is SecurityException -> "Permission denied. Please verify your account access."
        else -> throwable.localizedMessage ?: throwable.message ?: "An unexpected error occurred. Please try again."
    }
}
