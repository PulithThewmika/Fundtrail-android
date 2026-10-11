package com.fundtrail.domain.usecase.auth

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.LocalDataRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val localData: LocalDataRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        // Same guard as SignInUseCase: never start a session on top of a previous user's data.
        val cleared = localData.clearLocalData()
        if (cleared is Result.Error) return cleared
        return auth.register(email.trim(), password)
    }
}
