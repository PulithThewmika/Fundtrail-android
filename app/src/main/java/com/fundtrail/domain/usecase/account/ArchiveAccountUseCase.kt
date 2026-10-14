package com.fundtrail.domain.usecase.account

import com.fundtrail.domain.model.LastActiveAccountException
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AccountRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ArchiveAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(accountId: String): Result<Unit> {
        val existing = accountRepository.observeAll().first()
        val target = existing.find { it.id == accountId } ?: return Result.Error(IllegalArgumentException("Account not found"))
        if (!target.archived) {
            val activeCount = existing.count { !it.archived }
            if (activeCount <= 1) {
                return Result.Error(LastActiveAccountException())
            }
        }
        return accountRepository.archive(accountId)
    }
}
