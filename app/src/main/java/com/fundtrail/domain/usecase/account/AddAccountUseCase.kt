package com.fundtrail.domain.usecase.account

import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AccountRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class AddAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(name: String, kind: AccountKind, currency: String): Result<Unit> {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return Result.Error(IllegalArgumentException("Account name must not be blank"))
        }
        val existing = accountRepository.observeAll().first()
        if (existing.any { it.name.equals(trimmed, ignoreCase = true) }) {
            return Result.Error(IllegalArgumentException("An account with this name already exists"))
        }
        val nextSortOrder = (existing.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val newAccount = Account(
            id = "acc_${UUID.randomUUID().toString().take(8)}",
            name = trimmed,
            kind = kind,
            currency = currency.ifBlank { "LKR" },
            sortOrder = nextSortOrder
        )
        return accountRepository.add(newAccount)
    }
}
