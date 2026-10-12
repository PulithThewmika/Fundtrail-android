package com.fundtrail.fakes

import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeAccountRepository @Inject constructor() : AccountRepository {

    private val accountsFlow = MutableStateFlow<List<Account>>(KavinduSampleData.defaultAccounts)

    override fun observeActive(): Flow<List<Account>> {
        return accountsFlow.map { list -> list.filter { !it.isArchived } }
    }

    override fun observeAll(): Flow<List<Account>> {
        return accountsFlow
    }

    override suspend fun add(account: Account): Result<Unit> {
        return try {
            account.validate()
            accountsFlow.value = accountsFlow.value + account
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Error(e)
        }
    }

    override suspend fun rename(id: String, newName: String): Result<Unit> {
        if (newName.isBlank()) return Result.Error(IllegalArgumentException("Account name must not be blank"))
        val currentList = accountsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        return if (index != -1) {
            currentList[index] = currentList[index].copy(name = newName)
            accountsFlow.value = currentList
            Result.Success(Unit)
        } else {
            Result.Error(IllegalArgumentException("Account not found: $id"))
        }
    }

    override suspend fun archive(id: String): Result<Unit> {
        val currentList = accountsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        return if (index != -1) {
            currentList[index] = currentList[index].copy(isArchived = true)
            accountsFlow.value = currentList
            Result.Success(Unit)
        } else {
            Result.Error(IllegalArgumentException("Account not found: $id"))
        }
    }

    override suspend fun seedDefaultsIfEmpty(): Result<Unit> {
        if (accountsFlow.value.isEmpty()) {
            accountsFlow.value = KavinduSampleData.defaultAccounts
        }
        return Result.Success(Unit)
    }
}
