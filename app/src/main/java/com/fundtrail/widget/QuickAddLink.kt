package com.fundtrail.widget

import android.content.Context
import android.content.Intent
import com.fundtrail.MainActivity

/**
 * The "open quick add" request the home-screen widget sends to [MainActivity] (#226, #228). It is an extra on the
 * launch intent, so the app needs no new exported component and a signed-out user simply lands on login.
 */
object QuickAddLink {
    const val EXTRA_OPEN_QUICK_ADD = "com.fundtrail.extra.OPEN_QUICK_ADD"

    fun intent(context: Context): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_QUICK_ADD, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    fun Intent.wantsQuickAdd(): Boolean = getBooleanExtra(EXTRA_OPEN_QUICK_ADD, false)
}
