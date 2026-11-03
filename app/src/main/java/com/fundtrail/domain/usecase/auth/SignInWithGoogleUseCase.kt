package com.fundtrail.domain.usecase.auth

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.LocalDataRepository
import javax.inject.Inject

/**
 * "Continue with Google" (#213). The UI gets the Google ID token from the account picker; this signs in with it.
 * A new Google user gets the default accounts and categories from `DefaultDataSeeder`, like any new user.
 */
class SignInWithGoogleUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val localData: LocalDataRepository,
) {
    suspend operator fun invoke(idToken: String): Result<Unit> {
        // Same guard as SignInUseCase: never start a session on top of a previous user's data (NFR-25).
        val cleared = localData.clearLocalData()
        if (cleared is Result.Error) return cleared
        return auth.signInWithGoogle(idToken)
    }
}
