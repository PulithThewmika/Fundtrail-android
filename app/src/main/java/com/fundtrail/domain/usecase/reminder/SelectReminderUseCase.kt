package com.fundtrail.domain.usecase.reminder

import com.fundtrail.domain.model.ReminderMessage
import com.fundtrail.domain.model.SalaryStatus
import javax.inject.Inject

/**
 * Chooses the single daily-reminder message by priority (#218, #220), or null for no notification:
 *
 *  1. salary DUE,
 *  2. recurring items due,
 *  3. nothing recorded today,
 *  4. otherwise nothing.
 *
 * Priorities 1 and 2 fire even if something was already logged today; only priority 3 defers to [loggedToday].
 * Pure, so every branch is unit-testable (#220 AC).
 */
class SelectReminderUseCase @Inject constructor() {
    operator fun invoke(salaryStatus: SalaryStatus, dueCount: Int, loggedToday: Boolean): ReminderMessage? = when {
        salaryStatus is SalaryStatus.Due -> ReminderMessage.SALARY_DUE
        dueCount > 0 -> ReminderMessage.RECURRING_DUE
        !loggedToday -> ReminderMessage.NOTHING_LOGGED
        else -> null
    }
}
