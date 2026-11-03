package com.fundtrail.reminders

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkManagerReminderSchedulerTest {
    @Test
    fun laterTodayIsTheSameDay() {
        // 09:00 now, 21:00 target -> 12 hours.
        assertEquals(12 * 60, WorkManagerReminderScheduler.minutesUntilNext(9 * 60, 21 * 60))
    }

    @Test
    fun earlierThanNowRollsToTomorrow() {
        // 22:00 now, 21:00 target -> 23 hours.
        assertEquals(23 * 60, WorkManagerReminderScheduler.minutesUntilNext(22 * 60, 21 * 60))
    }

    @Test
    fun exactlyNowIsTomorrow() {
        assertEquals(24 * 60, WorkManagerReminderScheduler.minutesUntilNext(21 * 60, 21 * 60))
    }
}
