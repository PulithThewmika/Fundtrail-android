package com.fundtrail.domain.usecase.security

import com.fundtrail.domain.repository.AppLockRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Whether the app lock is on (#222). */
class ObserveAppLockEnabledUseCase @Inject constructor(private val appLock: AppLockRepository) {
    operator fun invoke(): Flow<Boolean> = appLock.enabled
}
