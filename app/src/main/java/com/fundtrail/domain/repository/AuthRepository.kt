package com.fundtrail.domain.repository

import com.fundtrail.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** The signed-in user's id, or null when signed out. Repositories read the uid from here (architecture.md §3.4). */
    val currentUserId: Flow<String?>

    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun register(email: String, password: String): Result<Unit>

    suspend fun signOut(): Result<Unit>
}
