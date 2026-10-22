package com.fundtrail.domain.usecase.auth

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SignOutOutcome
import com.fundtrail.domain.repository.AppLockRepository
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.LocalDataRepository
import com.fundtrail.domain.repository.SyncRepository
import javax.inject.Inject

/** Sign-out sequence from architecture.md §4.7. Navigation (step 6) is the UI's job. */
class SignOutUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val localData: LocalDataRepository,
    private val sync: SyncRepository,
    private val appLock: AppLockRepository,
) {
    /** [force] skips the unsynced-writes check after the user confirmed they accept losing them. */
    suspend operator fun invoke(force: Boolean = false): Result<SignOutOutcome> {
        if (!force && !localData.awaitPendingWrites()) {
            return Result.Success(SignOutOutcome.HAS_UNSYNCED_WRITES)
        }
        // Wait for the listeners to finish so no snapshot lands in Room after it is cleared (§4.7 step 2).
        sync.pauseSync()
        val signedOut = auth.signOut()
        if (signedOut is Result.Error) {
            sync.resumeSync() // still signed in, so keep mirroring
            return signedOut
        }
        val cleared = localData.clearLocalData()
        if (cleared is Result.Error) return cleared
        // The lock belongs to the signed-in user; the next one starts without it (#222).
        val unlocked = appLock.setEnabled(false)
        if (unlocked is Result.Error) return unlocked
        return Result.Success(SignOutOutcome.SIGNED_OUT)
    }
}
