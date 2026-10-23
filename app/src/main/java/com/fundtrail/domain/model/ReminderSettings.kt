package com.fundtrail.domain.model

/**
 * Per-device daily-reminder settings (#218, #219). Not synced: it belongs to the device, not the account,
 * so it lives in Preferences DataStore, not Room or Firestore.
 *
 * @property enabled whether the daily reminder is on (default off)
 * @property minuteOfDay the reminder time as minutes past midnight (default 21:00 = 1260)
 */
data class ReminderSettings(
    val enabled: Boolean = false,
    val minuteOfDay: Int = DEFAULT_MINUTE_OF_DAY,
) {
    val hour: Int get() = minuteOfDay / 60
    val minute: Int get() = minuteOfDay % 60

    companion object {
        const val DEFAULT_MINUTE_OF_DAY = 21 * 60
    }
}
