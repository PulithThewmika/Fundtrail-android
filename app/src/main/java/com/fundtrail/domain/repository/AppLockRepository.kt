package com.fundtrail.domain.repository

import com.fundtrail.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * The optional app lock setting (#222). Stored on the device only: it protects this phone, so it never syncs.
 */
interface AppLockRepository {
    /** Emits the stored setting; false until it has been turned on. */
    val enabled: Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean): Result<Unit>
}
