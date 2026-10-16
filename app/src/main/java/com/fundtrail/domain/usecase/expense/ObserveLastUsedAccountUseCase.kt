package com.fundtrail.domain.usecase.expense

import com.fundtrail.domain.repository.TransactionRepository
import javax.inject.Inject

/** Id of the account the newest expense was booked to, or null before the first expense (FR-31). */
class ObserveLastUsedAccountUseCase @Inject constructor(private val transactions: TransactionRepository) {
    operator fun invoke() = transactions.observeLastUsedAccountId()
}
