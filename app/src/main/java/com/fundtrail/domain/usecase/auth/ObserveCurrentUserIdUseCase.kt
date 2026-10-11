package com.fundtrail.domain.usecase.auth

import com.fundtrail.domain.repository.AuthRepository
import javax.inject.Inject

class ObserveCurrentUserIdUseCase @Inject constructor(private val auth: AuthRepository) {
    operator fun invoke() = auth.currentUserId
}
