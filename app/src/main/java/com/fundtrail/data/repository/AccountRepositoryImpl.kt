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
        return runCatchingResult {
            syncStateDao.observe().first { it?.initialSyncDone == true }
            val current = accountDao.observeAll().first()
            if (current.isEmpty()) {
                for (acc in defaultAccounts) {
                    when (val res = remoteDataSource.addAccount(uid, acc)) {
                        is Result.Success -> {}
                        is Result.Error -> return res
                    }
                }
            }
        }
    }

    private companion object {
        val defaultAccounts = listOf(
            Account(id = "acc_main_bank", name = "Main Bank", kind = AccountKind.BANK, currency = "LKR", openingBalanceLkrMinor = 0L, sortOrder = 1),
            Account(id = "acc_secondary_bank", name = "Secondary Bank", kind = AccountKind.BANK, currency = "LKR", openingBalanceLkrMinor = 0L, sortOrder = 2),
            Account(id = "acc_cash", name = "Cash Wallet", kind = AccountKind.CASH, currency = "LKR", openingBalanceLkrMinor = 0L, sortOrder = 3),
            Account(id = "acc_binance", name = "Binance", kind = AccountKind.CRYPTO_EXCHANGE, currency = "USDT", openingBalanceLkrMinor = 0L, sortOrder = 4),
            Account(id = "acc_other", name = "Other", kind = AccountKind.OTHER, currency = "LKR", openingBalanceLkrMinor = 0L, sortOrder = 5)
        )
    }
}
