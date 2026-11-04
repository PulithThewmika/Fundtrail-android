package com.fundtrail.domain.usecase.reminder

import com.fundtrail.domain.model.ReminderMessage
import com.fundtrail.domain.model.SalaryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectReminderUseCaseTest {
    private val select = SelectReminderUseCase()

    private val due = SalaryStatus.Due(expectedAmountMinor = 13_000_000L, salaryPayday = 25)
    private val notDue = SalaryStatus.NotDue
    private val received = SalaryStatus.Received(amountMinor = 13_000_000L, differenceFromExpected = 0L)

    @Test
    fun salaryDueWins() {
        assertEquals(ReminderMessage.SALARY_DUE, select(due, dueCount = 3, loggedToday = true))
    }

    @Test
    fun recurringDueWhenNoSalaryDue() {
        assertEquals(ReminderMessage.RECURRING_DUE, select(notDue, dueCount = 2, loggedToday = true))
    }

    @Test
    fun nothingLoggedWhenNoSalaryOrBills() {
        assertEquals(ReminderMessage.NOTHING_LOGGED, select(notDue, dueCount = 0, loggedToday = false))
    }

    @Test
    fun noMessageWhenAllQuietAndSomethingLogged() {
        assertNull(select(received, dueCount = 0, loggedToday = true))
    }

    @Test
    fun billsStillFireEvenWhenSomethingWasLogged() {
        assertEquals(ReminderMessage.RECURRING_DUE, select(received, dueCount = 1, loggedToday = true))
    }

    @Test
    fun nothingLoggedDefersToToday() {
        // Logged today and nothing else pending: no nag.
        assertNull(select(received, dueCount = 0, loggedToday = true))
        // Not logged today: the gentle prompt.
        assertEquals(ReminderMessage.NOTHING_LOGGED, select(received, dueCount = 0, loggedToday = false))
    }
}
