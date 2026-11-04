# 0007. Daily reminder: one WorkManager chain, one notification

- **Status:** Accepted
- **Date:** 2026-10-23
- **Deciders:** @kaveen20030408

## Context

EPIC-018 needs a single smart reminder per day that tells the user about the most useful thing first: a salary that is due, then due recurring items, then a nudge if nothing was logged today. It must be exact enough to feel daily, cost little battery, respect the Android 13 notification permission and stop on sign-out.

## Decision

We will schedule one unique `OneTimeWorkRequest` ("daily-reminder", REPLACE) for the next chosen time. When it runs, `DailyReminderWorker` picks the message with `SelectReminderUseCase`, posts at most one notification, and schedules the next run. Settings (enabled, time) live in DataStore. The notification opens the app; it does not deep link into a screen yet (`TODO(M4)`).

## Options considered

| Option | Pros | Cons |
|--------|------|------|
| A (rejected): `PeriodicWorkRequest` 24h | Simple. | Cannot fire at a chosen minute; Android may shift it by hours. |
| B (rejected): `AlarmManager` exact alarm | Exact. | Needs exact-alarm permission and a boot receiver. More than a reminder needs. |
| C (chosen): self-rescheduling one-time work | Close to the chosen time, survives reboot through WorkManager, no extra permission. | The time can drift by a few minutes when the device is dozing. |

## Consequences

- **Positive:** one notification a day at most; message priority is a pure, tested use case; sign-out cancels the work and resets the toggle.
- **Negative / trade-offs:** delivery is not exact. If the user denies `POST_NOTIFICATIONS` the toggle stays off and the screen explains why.
- **Follow-ups:** `TODO(M4)`: deep link each message to its screen.
