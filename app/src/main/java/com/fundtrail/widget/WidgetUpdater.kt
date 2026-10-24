package com.fundtrail.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.fundtrail.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
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
                .catch { Log.w(TAG, "Widget updates stopped", it) }
                .collect { FundtrailWidget().updateAll(context) }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 500L
        const val TAG = "WidgetUpdater"
    }
}
