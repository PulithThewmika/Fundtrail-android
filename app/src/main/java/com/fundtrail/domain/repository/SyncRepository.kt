package com.fundtrail.domain.repository

interface SyncRepository {
    /** Stops the Firestore to Room listeners and returns once none can write to Room any more. */
    suspend fun pauseSync()

    /** Lets sync follow the signed-in user again. Safe to call when nothing was paused. */
    fun resumeSync()
}
