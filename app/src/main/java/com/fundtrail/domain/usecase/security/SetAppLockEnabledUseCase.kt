package com.fundtrail.domain.usecase.security

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.ScreenLockRequiredException
import com.fundtrail.domain.repository.AppLockRepository
import com.fundtrail.domain.repository.DeviceSecurityRepository
import javax.inject.Inject

/**
 * Turns the app lock on or off (#222). Turning it on needs a way to unlock: without a screen lock the user could
 * lock themselves out, so the setting stays off and [ScreenLockRequiredException] says why.
 */
class SetAppLockEnabledUseCase @Inject constructor(
    private val appLock: AppLockRepository,
    private val device: DeviceSecurityRepository,
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> {
        if (enabled && !device.canAuthenticate()) return Result.Error(ScreenLockRequiredException())
        return appLock.setEnabled(enabled)
    }
}
