package com.fundtrail.data.repository

import com.fundtrail.data.local.dao.AccountDao
import com.fundtrail.data.local.dao.SyncStateDao
import com.fundtrail.data.remote.source.AccountRemoteDataSource
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.AccountRepository
import com.fundtrail.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val auth: AuthRepository,
    private val accountDao: AccountDao,
    private val syncStateDao: SyncStateDao,
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
        val validationResult = runCatchingResult { account.validate() }
        if (validationResult is Result.Error) return validationResult
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        accountDao.upsertAll(listOf(account.toEntity()))
        return when (val res = remoteDataSource.addAccount(uid, account)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun rename(id: String, newName: String): Result<Unit> {
        val validationResult = runCatchingResult { require(newName.isNotBlank()) { "Account name must not be blank" } }
        if (validationResult is Result.Error) return validationResult
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        val existing = accountDao.observeAll().first().find { it.id == id }
        if (existing != null) {
            accountDao.upsertAll(listOf(existing.copy(name = newName)))
        }
        return when (val res = remoteDataSource.updateAccount(uid, id, mapOf("name" to newName))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
        }
    }

    override suspend fun archive(id: String): Result<Unit> {
        val uid = auth.currentUserId.first() ?: return Result.Error(IllegalStateException("Not signed in"))
        val existing = accountDao.observeAll().first().find { it.id == id }
        if (existing != null) {
            accountDao.upsertAll(listOf(existing.copy(archived = true)))
        }
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
                accountDao.upsertAll(defaultAccounts.map { it.toEntity() })
                for (acc in defaultAccounts) {
                    remoteDataSource.addAccount(uid, acc)
                }
            }
        }
    }

    private companion object {
        val defaultAccounts = listOf(
            Account(id = "default-main-bank", name = "Main bank", kind = AccountKind.BANK, currency = "LKR", sortOrder = 1),
            Account(id = "default-secondary-bank", name = "Secondary bank", kind = AccountKind.BANK, currency = "LKR", sortOrder = 2),
            Account(id = "default-cash", name = "Cash", kind = AccountKind.CASH, currency = "LKR", sortOrder = 3),
            Account(id = "default-binance", name = "Binance", kind = AccountKind.CRYPTO_EXCHANGE, currency = "USDT", sortOrder = 4),
        )
    }
}
