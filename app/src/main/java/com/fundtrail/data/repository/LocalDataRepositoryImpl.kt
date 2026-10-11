package com.fundtrail.data.repository

import com.fundtrail.data.local.FundtrailDatabase
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.LocalDataRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Provider

class LocalDataRepositoryImpl @Inject constructor(
    private val firestore: Provider<FirebaseFirestore>, // unscoped: terminate() invalidates the instance (§4.7)
    private val db: FundtrailDatabase,
) : LocalDataRepository {

    override suspend fun awaitPendingWrites(): Boolean = try {
        withTimeoutOrNull(PENDING_WRITES_TIMEOUT_MS) { firestore.get().waitForPendingWrites().await() } != null
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    override suspend fun clearLocalData(): Result<Unit> = runCatchingResult {
        val fs = firestore.get()
        fs.terminate().await()
        fs.clearPersistence().await()
        withContext(Dispatchers.IO) { db.clearAllTables() }
    }

    private companion object {
        const val PENDING_WRITES_TIMEOUT_MS = 3_000L
    }
}
