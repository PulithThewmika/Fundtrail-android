package com.fundtrail.domain.repository

/**
 * Schedules (or cancels) the daily reminder (#218, #219). Pure interface so a use case can drive it; the
 * implementation enqueues a unique one-time work request so re-scheduling REPLACEs the old one and the time
 * never drifts, and the worker re-schedules the next day itself.
 */
interface ReminderScheduler {
    /** Schedule the next reminder for [minuteOfDay] (minutes past midnight), replacing any pending one. */
    fun schedule(minuteOfDay: Int)

    /** Cancel any pending reminder (toggle off, or logout). */
    fun cancel()
}
