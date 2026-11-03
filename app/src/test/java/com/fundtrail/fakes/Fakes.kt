package com.fundtrail.fakes

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SyncStatus
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.LocalDataRepository
import com.fundtrail.domain.repository.SyncRepository
import com.fundtrail.domain.repository.SyncStatusRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository : AuthRepository {
    val user = MutableStateFlow<String?>(null)
    override val currentUserId = user
    override var currentUserDisplayName: String? = null
    val calls = mutableListOf<String>()
    var result: Result<Unit> = Result.Success(Unit)

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        calls += "signIn:$email"
        return result
    }

    override suspend fun register(email: String, password: String): Result<Unit> {
        calls += "register:$email"
        return result
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        calls += "google:$idToken"
        return result
    }

    override suspend fun signOut(): Result<Unit> {
        calls += "signOut"
        return result
    }
}

class FakeLocalDataRepository(private val calls: MutableList<String>) : LocalDataRepository {
    var pendingWritesDone = true
    var clearResult: Result<Unit> = Result.Success(Unit)

    override suspend fun awaitPendingWrites() = pendingWritesDone

    override suspend fun clearLocalData(): Result<Unit> {
        calls += "clear"
        return clearResult
    }
}

class FakeSyncRepository(private val calls: MutableList<String>) : SyncRepository {
    override suspend fun pauseSync() {
        calls += "pauseSync"
    }

    override fun resumeSync() {
        calls += "resumeSync"
    }
}

class FakeSyncStatusRepository : SyncStatusRepository {
    val status = MutableStateFlow(SyncStatus(initialSyncDone = true, isOnline = true))
    override fun observe() = status
}
