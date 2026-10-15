package com.fundtrail.data.repository

import android.util.Log
import com.fundtrail.data.local.dao.AccountDao
import com.fundtrail.data.local.dao.SyncStateDao
import com.fundtrail.data.remote.source.AccountRemoteDataSource
import com.fundtrail.di.ApplicationScope
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.defaultAccounts
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.AccountRepository
import com.fundtrail.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val auth: AuthRepository,
    private val accountDao: AccountDao,
    private val syncStateDao: SyncStateDao,
    private val remoteDataSource: AccountRemoteDataSource,
    @ApplicationScope private val scope: CoroutineScope,
) : AccountRepository {

    override fun observeActive(): Flow<List<Account>> {
        return accountDao.observeAll().map { entities ->
            entities.map { it.toDomain() }.filter { !it.archived }
        }
    }

    override fun observeAll(): Flow<List<Account>> {
        return accountDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun add(account: Account): Result<Unit> {
        val validationResult = runCatchingResult { account.validate() }
        if (validationResult is Result.Error) return validationResult
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.addAccount(uid, account)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun rename(id: String, newName: String): Result<Unit> {
        val validationResult = runCatchingResult { require(newName.isNotBlank()) { "Account name must not be blank" } }
        if (validationResult is Result.Error) return validationResult
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.updateAccount(uid, id, mapOf("name" to newName))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun archive(id: String): Result<Unit> {
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.updateAccount(uid, id, mapOf("archived" to true))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun seedDefaultsIfEmpty(): Result<Unit> {
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        // Runs in the background so sign-in never waits for the first sync (architecture.md §3.4).
        scope.launch {
            try {
                syncStateDao.observe().first { it?.initialSyncDone == true }
                // The user may have signed out while this waited; never seed into another session.
                if (auth.currentUserId.first() != uid) return@launch
                if (accountDao.observeAll().first().isNotEmpty()) return@launch
                defaultAccounts.forEach { account ->
                    val res = remoteDataSource.addAccount(uid, account)
                    if (res is Result.Error) Log.e(TAG, "Seeding ${account.id} failed", res.exception)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Seeding default accounts failed", e)
            }
        }
        return Result.Success(Unit)
    }

    private companion object {
        const val TAG = "AccountRepository"
    }
}
