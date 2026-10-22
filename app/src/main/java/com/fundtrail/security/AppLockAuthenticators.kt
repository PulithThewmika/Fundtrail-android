package com.fundtrail.security

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL

/**
 * Fingerprint or face, with the phone's PIN, pattern or password as the fallback (#222). Shared by the capability
 * check and the unlock prompt, so "can turn the lock on" and "can unlock" never disagree.
 */
const val APP_LOCK_AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
