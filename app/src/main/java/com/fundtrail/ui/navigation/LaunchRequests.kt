package com.fundtrail.ui.navigation

/**
 * Screens the app was asked to open from outside: an invoice notification, or the widget's "+ Add" (#228, #256).
 *
 * While the app lock is showing, none of these may act, but none may be lost either: the request is held and the
 * navigation effects stay idle until the unlock releases it (#222).
 */
data class LaunchRequests(
    val invoiceId: String? = null,
    val quickAdd: Boolean = false,
) {
    /** What navigation may act on now: nothing while [locked], everything once unlocked. */
    fun releasedWhen(locked: Boolean): LaunchRequests = if (locked) LaunchRequests() else this
}
