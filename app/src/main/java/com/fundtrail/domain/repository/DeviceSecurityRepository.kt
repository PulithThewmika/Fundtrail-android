package com.fundtrail.domain.repository

/** What the phone itself can do to confirm it's the owner (#222). */
interface DeviceSecurityRepository {
    /** True when a fingerprint, face or screen lock (PIN, pattern, password) can be used to unlock. */
    fun canAuthenticate(): Boolean
}
