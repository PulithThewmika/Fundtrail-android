package com.fundtrail.widget

import com.fundtrail.domain.model.SafeToSpend
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.usecase.summary.GetSafeToSpendUseCase
import com.fundtrail.domain.usecase.summary.GetTodaySpendUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns the existing use cases into one [WidgetState] (#226). It only combines; the maths stays in the use cases.
 * [observe] drives live updates, [load] gives a one-shot snapshot for `provideGlance`.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetStateProvider @Inject constructor(
    private val auth: AuthRepository,
    private val getTodaySpend: GetTodaySpendUseCase,
    private val getSafeToSpend: GetSafeToSpendUseCase,
) {
    fun observe(): Flow<WidgetState> =
        auth.currentUserId.distinctUntilChanged().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(WidgetState.SignedOut)
            } else {
                // Safe to spend failing must not hide today's spend, so it degrades to null on its own.
                val safe = getSafeToSpend().map<SafeToSpend, SafeToSpend?> { it }.catch { emit(null) }
                combine(getTodaySpend(), safe) { today, safeToSpend -> WidgetState.Ready(today, safeToSpend) }
            }
        }

    /** The current state, or [WidgetState.Unavailable] if it cannot be read quickly. A cancel still propagates. */
    suspend fun load(): WidgetState =
        try {
            withTimeout(LOAD_TIMEOUT_MS) { observe().first() }
        } catch (timeout: TimeoutCancellationException) {
            WidgetState.Unavailable
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            WidgetState.Unavailable
        }

    private companion object {
        const val LOAD_TIMEOUT_MS = 8_000L
    }
}
