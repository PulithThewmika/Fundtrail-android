package com.fundtrail.domain.usecase.auth

import com.fundtrail.domain.model.UserProfile
import com.fundtrail.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** The signed-in user's name, email and photo; null while signed out. Re-read whenever the signed-in user changes. */
class ObserveUserProfileUseCase @Inject constructor(private val auth: AuthRepository) {
    operator fun invoke(): Flow<UserProfile?> = auth.currentUserId.map { uid -> uid?.let { auth.currentUserProfile } }
}
