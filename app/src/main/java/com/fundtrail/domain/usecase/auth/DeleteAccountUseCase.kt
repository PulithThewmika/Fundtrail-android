package com.fundtrail.domain.usecase.auth

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AppLockRepository
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.LocalDataRepository
import com.fundtrail.domain.repository.SyncRepository
import javax.inject.Inject

/** FR-117: delete the account and all its data. Same sequence as [SignOutUseCase], plus the remote wipe. */
class DeleteAccountUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val localData: LocalDataRepository,
    private val sync: SyncRepository,
    private val appLock: AppLockRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        sync.pauseSync()
        val deleted = auth.deleteAccount()
        if (deleted is Result.Error) {
            sync.resumeSync() // still signed in (e.g. requires a recent login), so keep mirroring
            return deleted
        }
        val cleared = localData.clearLocalData()
        if (cleared is Result.Error) return cleared
        appLock.setEnabled(false)
        return Result.Success(Unit)
    }
}
