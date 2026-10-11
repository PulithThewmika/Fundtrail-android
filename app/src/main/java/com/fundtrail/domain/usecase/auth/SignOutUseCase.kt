package com.fundtrail.domain.usecase.auth

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SignOutOutcome
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.LocalDataRepository
import javax.inject.Inject

/** Sign-out sequence from architecture.md §4.7. Navigation (step 6) is the UI's job. */
class SignOutUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val localData: LocalDataRepository,
) {
    /** [force] skips the unsynced-writes check after the user confirmed they accept losing them. */
    suspend operator fun invoke(force: Boolean = false): Result<SignOutOutcome> {
        if (!force && !localData.awaitPendingWrites()) {
            return Result.Success(SignOutOutcome.HAS_UNSYNCED_WRITES)
        }
        // TODO(M2): stop the sync listeners here, before auth goes away (§4.7 step 2).
        val signedOut = auth.signOut()
        if (signedOut is Result.Error) return signedOut
        val cleared = localData.clearLocalData()
        if (cleared is Result.Error) return cleared
        return Result.Success(SignOutOutcome.SIGNED_OUT)
    }
}
