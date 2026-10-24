package com.fundtrail.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.fundtrail.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the home-screen widget current (#228). It watches the same [WidgetState] the widget draws, and whenever
 * it changes (an entry, a sync, a goal or recurring change, midnight, sign-in or sign-out) asks the widget to
 * redraw. Started once from `FundtrailApplication`, like the sync and reminder starters.
 *
 * A failed redraw is logged and skipped, never allowed to end the watching: one bad redraw must not leave the
 * widget stuck on an old message for good.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stateProvider: WidgetStateProvider,
    @ApplicationScope private val scope: CoroutineScope,
) {
    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch {
            stateProvider.observe()
                .distinctUntilChanged()
                // A burst of writes (a sync, a confirm that changes several figures) redraws the widget once.
                .debounce(DEBOUNCE_MS)
                .catch { Log.w(TAG, "Widget state stream failed", it) }
                .collect { refreshNow() }
        }
    }

    /** Redraws every FundTrail widget now (also used after the look is changed). Never throws. */
    suspend fun refreshNow() {
        try {
            FundtrailWidget().updateAll(context)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.w(TAG, "Widget redraw failed", error)
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 500L
        const val TAG = "WidgetUpdater"
    }
}
