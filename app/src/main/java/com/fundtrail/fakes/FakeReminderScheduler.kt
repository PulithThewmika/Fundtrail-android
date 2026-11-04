package com.fundtrail.fakes

import com.fundtrail.domain.repository.ReminderScheduler
import javax.inject.Inject
import javax.inject.Singleton

/** Records scheduling calls so tests can assert the reminder was (re)scheduled or cancelled (#218). */
@Singleton
class FakeReminderScheduler @Inject constructor() : ReminderScheduler {
    var scheduledMinute: Int? = null
    var cancelled = false

    /** Test hook: when set, [schedule] and [cancel] throw it, as a failing WorkManager call would. */
    var error: Throwable? = null

    override fun schedule(minuteOfDay: Int) {
        error?.let { throw it }
        scheduledMinute = minuteOfDay
        cancelled = false
    }

    override fun cancel() {
        error?.let { throw it }
        cancelled = true
        scheduledMinute = null
    }
}
