package com.fundtrail.domain.usecase.auth

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.LocalDataRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val localData: LocalDataRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        // Nobody is signed in here, so any cached data is a previous user's leftover (NFR-25),
        // e.g. from a sign-out whose cleanup failed. Sign-in is blocked until it is gone.
        val cleared = localData.clearLocalData()
        if (cleared is Result.Error) return cleared
        return auth.signIn(email.trim(), password)
    }
}
