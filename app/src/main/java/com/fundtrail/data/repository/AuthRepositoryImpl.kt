package com.fundtrail.data.repository

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.fundtrail.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(private val auth: FirebaseAuth) : AuthRepository {

    override val currentUserId: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener) // emits the current state straight away
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        runCatchingResult { auth.signInWithEmailAndPassword(email, password).await(); Unit }

    override suspend fun register(email: String, password: String): Result<Unit> =
        runCatchingResult { auth.createUserWithEmailAndPassword(email, password).await(); Unit }

    override suspend fun signOut(): Result<Unit> = runCatchingResult { auth.signOut() }
}
