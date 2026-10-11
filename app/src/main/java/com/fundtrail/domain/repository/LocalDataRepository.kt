package com.fundtrail.domain.repository

import com.fundtrail.domain.model.Result

interface LocalDataRepository {
    /** True when every queued write reached the server; false if still pending after a short timeout. */
    suspend fun awaitPendingWrites(): Boolean

    /** Wipes the previous user's data from the device: Firestore cache, then Room. */
    suspend fun clearLocalData(): Result<Unit>
}
