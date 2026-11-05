package com.fundtrail.domain.usecase.account

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AccountRepository
import javax.inject.Inject

/** FR-11: reactivates a previously archived account. No last-active guard needed - this only adds an active one. */
class UnarchiveAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(accountId: String): Result<Unit> = accountRepository.unarchive(accountId)
}
