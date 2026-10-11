package com.fundtrail.ui.auth

import androidx.annotation.StringRes
import com.fundtrail.R

/** Input-format checks only (architecture.md §3.1). Each returns an error string, or null when valid. */
private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
const val MIN_PASSWORD_LENGTH = 6 // Firebase Auth minimum

@StringRes
fun validateEmail(email: String): Int? = when {
    email.isBlank() -> R.string.auth_error_email_required
    !EMAIL.matches(email.trim()) -> R.string.auth_error_email_invalid
    else -> null
}

@StringRes
fun validateNewPassword(password: String): Int? =
    if (password.length < MIN_PASSWORD_LENGTH) R.string.auth_error_password_short else null
