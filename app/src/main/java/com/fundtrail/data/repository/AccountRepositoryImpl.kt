package com.fundtrail.data.repository

import com.fundtrail.data.local.dao.AccountDao
import com.fundtrail.data.remote.source.AccountRemoteDataSource
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.AccountRepository
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.fakes.KavinduSampleData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val auth: AuthRepository,
    private val accountDao: AccountDao,
    private val remoteDataSource: AccountRemoteDataSource,
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
        account.validate()
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        return when (val res = remoteDataSource.addAccount(uid, account)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun rename(id: String, newName: String): Result<Unit> {
        require(newName.isNotBlank()) { "Account name must not be blank" }
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
        return runCatchingResult {
            val current = accountDao.observeAll().first()
            if (current.isEmpty()) {
                for (acc in KavinduSampleData.defaultAccounts) {
                    when (val res = remoteDataSource.addAccount(uid, acc)) {
                        is Result.Success -> {}
                        is Result.Error -> return res
                    }
                }
            }
        }
    }
}
