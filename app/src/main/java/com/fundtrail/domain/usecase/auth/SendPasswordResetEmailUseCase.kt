package com.fundtrail.domain.usecase.auth

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AuthRepository
import javax.inject.Inject

class SendPasswordResetEmailUseCase @Inject constructor(
    private val auth: AuthRepository,
) {
    suspend operator fun invoke(email: String): Result<Unit> = auth.sendPasswordResetEmail(email.trim())
}
