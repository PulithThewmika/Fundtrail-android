package com.fundtrail.domain.usecase.auth

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AuthRepository
import javax.inject.Inject

class ConfirmPasswordResetUseCase @Inject constructor(
    private val auth: AuthRepository,
) {
    suspend operator fun invoke(oobCode: String, newPassword: String): Result<Unit> =
        auth.confirmPasswordReset(oobCode, newPassword)
}
