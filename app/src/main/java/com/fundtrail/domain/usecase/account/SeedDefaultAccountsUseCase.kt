package com.fundtrail.domain.usecase.account

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AccountRepository
import javax.inject.Inject

class SeedDefaultAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return accountRepository.seedDefaultsIfEmpty()
    }
}
