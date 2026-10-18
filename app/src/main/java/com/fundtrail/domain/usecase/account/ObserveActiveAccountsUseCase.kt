package com.fundtrail.domain.usecase.account

import com.fundtrail.domain.repository.AccountRepository
import javax.inject.Inject

/** Accounts the user can still book against; archived ones are left out. */
class ObserveActiveAccountsUseCase @Inject constructor(private val accounts: AccountRepository) {
    operator fun invoke() = accounts.observeActive()
}
