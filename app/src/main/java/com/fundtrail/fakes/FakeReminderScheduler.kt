package com.fundtrail.fakes

import com.fundtrail.domain.repository.ReminderScheduler
import javax.inject.Inject
import javax.inject.Singleton

/** Records scheduling calls so tests can assert the reminder was (re)scheduled or cancelled (#218). */
@Singleton
class FakeReminderScheduler @Inject constructor() : ReminderScheduler {
    var scheduledMinute: Int? = null
    var cancelled = false

    override fun schedule(minuteOfDay: Int) {
        scheduledMinute = minuteOfDay
        cancelled = false
    }

    override fun cancel() {
        cancelled = true
        scheduledMinute = null
    }
}
