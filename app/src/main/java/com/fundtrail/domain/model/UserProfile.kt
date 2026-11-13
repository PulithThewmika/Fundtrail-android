package com.fundtrail.domain.model

/** Who is signed in, for display only. [name] and [photoUrl] are null for an email/password account. */
data class UserProfile(
    val name: String?,
    val email: String?,
    val photoUrl: String?,
    /** True when the account signs in with Google rather than email and password. */
    val viaGoogle: Boolean = false,
)
