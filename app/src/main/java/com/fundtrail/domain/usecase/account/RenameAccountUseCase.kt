package com.fundtrail.domain.usecase.account

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AccountRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RenameAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(accountId: String, newName: String): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            return Result.Error(IllegalArgumentException("Account name must not be blank"))
        }
        val existing = accountRepository.observeAll().first()
        if (existing.any { it.id != accountId && it.name.equals(trimmed, ignoreCase = true) }) {
            return Result.Error(IllegalArgumentException("An account with this name already exists"))
        }
        return accountRepository.rename(accountId, trimmed)
    }
}
