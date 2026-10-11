package com.fundtrail.fakes

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.LocalDataRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository : AuthRepository {
    val user = MutableStateFlow<String?>(null)
    override val currentUserId = user
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
