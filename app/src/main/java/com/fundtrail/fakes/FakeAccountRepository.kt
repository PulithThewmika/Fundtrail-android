package com.fundtrail.fakes

import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.defaultAccounts
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeAccountRepository @Inject constructor() : AccountRepository {

    private val accountsFlow = MutableStateFlow<List<Account>>(emptyList())

    override fun observeActive(): Flow<List<Account>> {
        return accountsFlow.map { list -> list.filter { !it.archived } }
    }

    override fun observeAll(): Flow<List<Account>> {
        return accountsFlow
    }

    override suspend fun add(account: Account): Result<Unit> = runCatchingResult {
        account.validate()
        require(accountsFlow.value.none { it.id == account.id }) { "Account already exists: ${account.id}" }
        accountsFlow.value = accountsFlow.value + account
    }

    override suspend fun rename(id: String, newName: String): Result<Unit> = runCatchingResult {
        require(newName.isNotBlank()) { "Account name must not be blank" }
        val currentList = accountsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        require(index != -1) { "Account not found: $id" }
        currentList[index] = currentList[index].copy(name = newName)
        accountsFlow.value = currentList
    }

    override suspend fun archive(id: String): Result<Unit> = runCatchingResult {
        val currentList = accountsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        require(index != -1) { "Account not found: $id" }
        currentList[index] = currentList[index].copy(archived = true)
        accountsFlow.value = currentList
    }

    override suspend fun seedDefaultsIfEmpty(): Result<Unit> = runCatchingResult {
        if (accountsFlow.value.isEmpty()) {
            accountsFlow.value = defaultAccounts
        }
    }
}
