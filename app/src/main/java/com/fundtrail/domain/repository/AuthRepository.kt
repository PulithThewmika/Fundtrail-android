package com.fundtrail.domain.repository

import com.fundtrail.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** The signed-in user's id, or null when signed out. Repositories read the uid from here (architecture.md §3.4). */
    val currentUserId: Flow<String?>

    /** A display name for the signed-in user, or null when signed out. Used only to seed a new `users/{uid}` profile document. */
    val currentUserDisplayName: String?

    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun register(email: String, password: String): Result<Unit>

    /**
     * Signs in (creating the account on first use) with a Google ID token. Fails with
     * [com.fundtrail.domain.model.EmailUsesPasswordSignInException] when the email already has a password account.
     */
    suspend fun signInWithGoogle(idToken: String): Result<Unit>

    /** FR-04: always succeeds for a well-formed email, even when no account uses it (no email enumeration). */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /** FR-04: completes the reset from the email link's `oobCode`. Fails if it's wrong, expired, or already used. */
    suspend fun confirmPasswordReset(oobCode: String, newPassword: String): Result<Unit>

    suspend fun signOut(): Result<Unit>

    /** FR-117: deletes every Firestore document under `users/{uid}` and then the Firebase Auth account itself. */
    suspend fun deleteAccount(): Result<Unit>
}
