package com.fundtrail.domain.usecase.account

import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.DuplicateAccountNameException
import com.fundtrail.domain.model.InvalidCurrencyException
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SUPPORTED_CURRENCIES
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
        val resolvedCurrency = currency.ifBlank { "LKR" }
        if (resolvedCurrency !in SUPPORTED_CURRENCIES) {
            return Result.Error(InvalidCurrencyException())
        }
        val existing = accountRepository.observeAll().first()
        if (existing.any { it.name.equals(trimmed, ignoreCase = true) }) {
            return Result.Error(DuplicateAccountNameException())
        }
        val nextSortOrder = (existing.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val newAccount = Account(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            kind = kind,
            currency = resolvedCurrency,
            sortOrder = nextSortOrder
        )
        return accountRepository.add(newAccount)
    }
}
