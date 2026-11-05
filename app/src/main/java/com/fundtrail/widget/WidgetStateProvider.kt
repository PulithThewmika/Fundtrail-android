package com.fundtrail.widget

import com.fundtrail.domain.model.SafeToSpend
import com.fundtrail.domain.repository.AuthRepository
import com.fundtrail.domain.usecase.security.ObserveAppLockEnabledUseCase
import com.fundtrail.domain.usecase.summary.GetSafeToSpendUseCase
import com.fundtrail.domain.usecase.summary.GetTodaySpendUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns the existing use cases into one [WidgetState] (#226). It only combines; the maths stays in the use cases.
 * [observe] drives live updates, [load] gives a one-shot snapshot for `provideGlance`.
 *
 * [observe] never ends because of a read error: a failure shows [WidgetState.Unavailable] and the read is retried,
 * so the widget recovers on its own (for example right after signing back in) instead of staying stuck.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetStateProvider @Inject constructor(
    private val auth: AuthRepository,
    private val getTodaySpend: GetTodaySpendUseCase,
    private val getSafeToSpend: GetSafeToSpendUseCase,
    private val observeAppLockEnabled: ObserveAppLockEnabledUseCase,
) {
    fun observe(): Flow<WidgetState> =
        auth.currentUserId.distinctUntilChanged().flatMapLatest { uid ->
            if (uid == null) flowOf(WidgetState.SignedOut) else lockedOrReady()
        }

    /**
     * While the app lock is on, the money flows are not collected at all (`flatMapLatest` cancels them), so a locked
     * widget never even reads the figures. Turning the lock off or on redraws the widget through [observe].
     */
    private fun lockedOrReady(): Flow<WidgetState> =
        lockEnabled().flatMapLatest { locked -> if (locked) flowOf(WidgetState.Locked) else readyStates() }

    /**
     * Fails closed: if the setting cannot be read, the widget shows Locked (no figures) and tries again, rather than
     * assuming the lock is off and showing money on a phone that may be protecting it.
     */
    private fun lockEnabled(): Flow<Boolean> =
        observeAppLockEnabled()
            .retryWhen { cause, attempt ->
                if (cause is CancellationException) {
                    false
                } else {
                    emit(true)
                    delay(retryDelayMs(attempt))
                    true
                }
            }
            .distinctUntilChanged()

    private fun readyStates(): Flow<WidgetState> {
        // Safe to spend failing must not hide today's spend, so it degrades to null on its own. It retries inside
        // its own flow: a plain catch would end it, leaving Safe to spend hidden for the rest of the session.
        val safe = getSafeToSpend()
            .map<SafeToSpend, SafeToSpend?> { it }
            .retryWhen { cause, attempt ->
                if (cause is CancellationException) {
                    false
                } else {
                    emit(null)
                    delay(retryDelayMs(attempt))
                    true
                }
            }
        return combine(getTodaySpend(), safe) { today, safeToSpend -> WidgetState.Ready(today, safeToSpend) as WidgetState }
            .retryWhen { cause, attempt ->
                if (cause is CancellationException) {
                    false
                } else {
                    emit(WidgetState.Unavailable)
                    delay(retryDelayMs(attempt))
                    true
                }
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
        const val RETRY_STEP_MS = 2_000L
        const val MAX_RETRY_DELAY_MS = 30_000L

        fun retryDelayMs(attempt: Long) = minOf(MAX_RETRY_DELAY_MS, RETRY_STEP_MS * (attempt + 1))
    }
}
