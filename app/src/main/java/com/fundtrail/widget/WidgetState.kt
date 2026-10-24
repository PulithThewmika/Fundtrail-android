package com.fundtrail.widget

import com.fundtrail.domain.model.SafeToSpend

/**
 * Everything the home-screen widget can show (#226). The widget only renders this; the figures come from the
 * domain use cases. `TODO(M4)`: a Locked state once the app lock (#222) lands.
 */
sealed interface WidgetState {
    /** Nobody is signed in: the widget says "Sign in to FundTrail" and opens the app. */
    data object SignedOut : WidgetState

    /** Today's spend in LKR minor units, and Safe to spend when it could be worked out (null hides that row). */
    data class Ready(val todaySpendMinor: Long, val safeToSpend: SafeToSpend?) : WidgetState

    /** The figures could not be read; the widget just opens the app. */
    data object Unavailable : WidgetState
}
