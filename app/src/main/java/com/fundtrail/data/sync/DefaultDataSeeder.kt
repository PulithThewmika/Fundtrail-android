package com.fundtrail.data.sync

import android.util.Log
import com.fundtrail.di.ApplicationScope
import com.fundtrail.di.IoDispatcher
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AccountRepository
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.repository.CategoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the default expense categories and accounts for a signed-in user (FR-20, EPIC-008 T2).
 *
 * It runs for every sign-in and every app start, which is safe: [CategoryRepository.seedDefaultsIfEmpty]
 * first waits for the initial server sync, then only writes when the user has no categories, and always
 * under fixed ids. So a returning user, a second device or a retry after a failed or killed run never
 * gets duplicates. Offline, the wait simply lasts until the first server snapshot arrives.
 */
@Singleton
class DefaultDataSeeder @Inject constructor(
    private val auth: AuthRepository,
    private val categories: CategoryRepository,
    private val accounts: AccountRepository,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    /** Call once from `Application.onCreate`. Seeding then follows the signed-in user. */
    fun start() {
        scope.launch(io) {
            // collectLatest cancels a waiting seed when the user signs out, so it can never run for the next user.
            auth.currentUserId.collectLatest { uid ->
                if (uid == null) return@collectLatest
                val result = categories.seedDefaultsIfEmpty()
                if (result is Result.Error) Log.e(TAG, "Seeding default categories failed; retried on next launch", result.exception)
                // Quick-add books every expense against an account, so a new user needs the default accounts too.
                val accountResult = accounts.seedDefaultsIfEmpty()
                if (accountResult is Result.Error) Log.e(TAG, "Seeding default accounts failed; retried on next launch", accountResult.exception)
            }
        }
    }

    private companion object {
        const val TAG = "DefaultDataSeeder"
    }
}
