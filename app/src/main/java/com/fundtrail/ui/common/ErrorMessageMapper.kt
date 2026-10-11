package com.fundtrail.ui.common

import androidx.annotation.StringRes
import com.fundtrail.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.util.concurrent.TimeoutException

/**
 * Centralized error-to-message resource mapping function as specified in EPIC-006/T4 S1.
 * Maps exceptions to user-friendly string resource IDs in one place rather than per-screen.
 */
@StringRes
fun mapErrorToMessage(throwable: Throwable): Int {
    return when (throwable) {
        is FirebaseNetworkException, is IOException, is TimeoutException -> R.string.error_network
        is FirebaseFirestoreException -> when (throwable.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.error_permission
            FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_network
            else -> R.string.error_generic
        }
        is FirebaseAuthException -> R.string.error_permission
        is SecurityException -> R.string.error_permission
        else -> R.string.error_generic
    }
}
