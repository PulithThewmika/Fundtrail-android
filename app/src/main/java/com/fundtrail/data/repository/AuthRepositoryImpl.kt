package com.fundtrail.data.repository

import com.fundtrail.domain.model.EmailUsesPasswordSignInException
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.AuthRepository
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** The six subcollections under `users/{uid}` (schema.md §3), wiped in full on account deletion (FR-117). */
private val USER_SUBCOLLECTIONS = listOf("accounts", "categories", "transactions", "invoices", "recurring", "goals")

/**
 * The Firebase Hosting default domain, verified via `public/.well-known/assetlinks.json` (deployed with
 * `firebase deploy --only hosting`) so Android opens the reset link in-app instead of a browser.
 */
private const val RESET_PASSWORD_URL = "https://fundtrail-5632e.web.app/resetPassword"
private val RESET_PASSWORD_ACTION_CODE_SETTINGS: ActionCodeSettings = ActionCodeSettings.newBuilder()
    .setUrl(RESET_PASSWORD_URL)
    .setHandleCodeInApp(true)
    .setAndroidPackageName("com.fundtrail", /* installIfNotAvailable = */ false, /* minimumVersion = */ null)
    .build()

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    override val currentUserId: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener) // emits the current state straight away
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override val currentUserDisplayName: String?
        get() = auth.currentUser?.let { it.displayName?.takeIf(String::isNotBlank) ?: it.email }

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        runCatchingResult { auth.signInWithEmailAndPassword(email, password).await(); Unit }

    override suspend fun register(email: String, password: String): Result<Unit> =
        runCatchingResult { auth.createUserWithEmailAndPassword(email, password).await(); Unit }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatchingResult {
        try {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        } catch (e: FirebaseAuthUserCollisionException) {
            throw EmailUsesPasswordSignInException(e)
        }
        Unit
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatchingResult {
        try {
            auth.sendPasswordResetEmail(email.trim(), RESET_PASSWORD_ACTION_CODE_SETTINGS).await()
        } catch (e: FirebaseAuthInvalidUserException) {
            // No account for this email: swallowed so the UI can't be used to probe which emails are registered.
        }
    }

    override suspend fun confirmPasswordReset(oobCode: String, newPassword: String): Result<Unit> =
        runCatchingResult { auth.confirmPasswordReset(oobCode, newPassword).await(); Unit }

    override suspend fun signOut(): Result<Unit> = runCatchingResult { auth.signOut() }

    override suspend fun deleteAccount(): Result<Unit> = runCatchingResult {
        val user = auth.currentUser ?: return@runCatchingResult
        val uid = user.uid
        for (collection in USER_SUBCOLLECTIONS) {
            val docs = firestore.collection("users").document(uid).collection(collection).get().await()
            for (doc in docs) doc.reference.delete().await()
        }
        firestore.collection("users").document(uid).delete().await()
        // Requires a recent sign-in (FirebaseAuthRecentLoginRequiredException); surfaced like any other error.
        user.delete().await()
    }
}
