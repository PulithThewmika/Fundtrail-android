package com.fundtrail.domain.model

/**
 * The one message a daily reminder may carry (#218, #220), highest priority first. A null selection means no
 * notification at all — the deliberate "nothing to nag about" case (A-22).
 */
enum class ReminderMessage {
    /** Salary is due (on or after payday, none recorded): confirm how much came in. Deep links to quick add. */
    SALARY_DUE,

    /** Recurring items are due: confirm them in one tap. Deep links to the due list. */
    RECURRING_DUE,

    /** Nothing was recorded today: a gentle "anything to add?". Deep links to quick add. */
    NOTHING_LOGGED,
}
