package com.fundtrail.domain.model

/** Google sign-in was refused: this email already has an email-and-password account (#213). */
class EmailUsesPasswordSignInException(cause: Throwable? = null) :
    IllegalStateException("This email already uses password sign-in", cause)
