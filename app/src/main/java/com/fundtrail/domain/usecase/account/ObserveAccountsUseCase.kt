package com.fundtrail.domain.usecase.account

import com.fundtrail.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Observes all accounts ordered by sortOrder.
 */
class ObserveAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    operator fun invoke() = accountRepository.observeAll()
}
