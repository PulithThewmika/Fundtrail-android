package com.fundtrail.data.security

import android.content.Context
import androidx.biometric.BiometricManager
import com.fundtrail.domain.repository.DeviceSecurityRepository
import com.fundtrail.security.APP_LOCK_AUTHENTICATORS
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class BiometricDeviceSecurity @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceSecurityRepository {
    override fun canAuthenticate(): Boolean =
        BiometricManager.from(context).canAuthenticate(APP_LOCK_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
}
