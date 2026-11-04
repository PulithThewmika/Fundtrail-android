# M4 regression test plan

Manual regression suite for the M4 release (EPIC-019, #263). Every case traces to an `FR-xx` /
`A-xx` / `NFR-xx` in [`Docs/srs.md`](../srs.md), so a pass is evidence against a requirement
rather than an opinion.

- **116 cases** across 17 groups.
- Run each case on **two device/API combinations** (columns A and B below).
- A **Fail always carries a bug number**. Open it with the Bug template and link it as a
  sub-issue of [#272](https://github.com/PulithThewmika/Fundtrail-android/issues/272)
  (`[EPIC-019] T4: Bug bash + fix window`). The full workflow is on
  [#262](https://github.com/PulithThewmika/Fundtrail-android/issues/262).

## Device matrix (#267)

Fill this in before running anything. Column A is the oldest supported level, column B the newest.

| Col | Device / emulator | Android (API) | Build | Signed in as | Tester |
|---|---|---|---|---|---|
| **A** | _e.g. Pixel 4 emulator, Google Play image_ | 8.0 (API 26) | debug / _commit_ | `demo` | |
| **B** | _e.g. Pixel 8 emulator_ | 15 (API 35) | debug / _commit_ | `demo` | |
| **C** _(optional)_ | _physical phone_ | | release / _commit_ | `demo` | |

API 26 must use a **Google Play** system image, otherwise Google Sign-In (FR-05) cannot be tested.

## How to read a row

- **Pre** — state the app must be in before step 1. `demo` = seeded demo account, `fresh` = empty account.
- **Steps** — numbered actions, `→` between them. Everything needed to reproduce.
- **Expected** — the pass bar. If the observed result differs in any way, it is a Fail.
- **Req** — the requirement this proves.
- **A / B** — enter `P` (pass), `F` (fail) or `-` (not applicable/not run).

Scenario figures used throughout: goal target **LKR 490,000**, already saved **LKR 11,200**,
required monthly **LKR 39,900**.

---

## REG-AUTH — Authentication and onboarding (9)

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-AUTH-01 | Signed out | 1. Register with a new email + password → 2. Submit | Account is created and onboarding opens | FR-01 | | | |
| REG-AUTH-02 | Signed out | 1. Register using an email that already exists | A specific error names the duplicate email; the form is not cleared | FR-01 | | | |
| REG-AUTH-03 | Signed out, `demo` exists | 1. Sign in with correct credentials | The dashboard opens directly | FR-02 | | | |
| REG-AUTH-04 | Signed out | 1. Sign in with a wrong password | An error shows **and the email field keeps its value** | FR-02 | | | |
| REG-AUTH-05 | Signed in as `demo` | 1. More → Sign out → 2. Inspect the app | Sign-in screen opens, no cached figures visible anywhere | FR-03, A-20 | | | |
| REG-AUTH-06 | Signed in as `demo` | 1. Force-stop the app → 2. Relaunch | Dashboard opens with no sign-in prompt | FR-06 | | | |
| REG-AUTH-07 | Two accounts exist | 1. Sign in as `demo`, note a figure → 2. Sign out → 3. Sign in as `fresh` | `fresh` shows none of `demo`'s records | FR-07, CON-15 | | | |
| REG-AUTH-08 | Signed out | 1. Register → 2. Skip all three onboarding steps | A working dashboard is reached with empty states, no dead end | FR-08, A-03 | | | |
| REG-AUTH-09 | Signed out, API 26 Play image | 1. Tap Google Sign-In → 2. Pick an account | Sign-in succeeds into the same per-user data space | FR-05 | | | |

## REG-QA — Quick add and categories (12)

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-QA-01 | Dashboard, `demo` | 1. Tap the quick-add FAB | Sheet opens in one tap, amount field focused, numeric keypad up | FR-30, A-02 | | | |
| REG-QA-02 | Quick add open | 1. Observe the prefilled fields | Date/time = now, account = last used, category = suggestion, type from category | FR-31 | | | |
| REG-QA-03 | Quick add open, categories exist | 1. Type `450` → 2. Leave the preselected category chip as it is → 3. Save | Saves with no error and no further input; the entry carries the preselected category; Spent rises by 450.00 | FR-32, FR-31, A-02 | | | |
| REG-QA-04 | `demo` has 30 days of history | 1. Open quick add → 2. Read the chip order | Chips are ordered by usage over the last 30 days, most-used first | FR-33 | | | |
| REG-QA-05 | Quick add open | 1. Enter `0` → 2. Try to save → 3. Enter `abc` | Save is blocked each time with an inline message | FR-113, CON-13 | | | |
| REG-QA-06 | Quick add open | 1. Set the date to last month → 2. Save → 3. Page the dashboard to that month | The entry appears in the back-dated month, not the current one | FR-35, A-03 | | | |
| REG-QA-07 | Quick add open | 1. Save with *Save and add another* | A fresh form opens with **date and account unchanged** | FR-36 | | | |
| REG-QA-08 | A saved expense exists | 1. Delete it → 2. Tap Undo in the snackbar | The entry and the month totals are restored | FR-37 | | | |
| REG-QA-09 | Goal active (490,000 / 39,900) | 1. Save a discretionary expense of 24,000 | A neutral message shows the goal cost (≈ % of goal and % of required saving), no guilt wording | FR-41, A-23, NFR-14 | | | |
| REG-QA-10 | Quick add open | 1. Type `PickMe` in the note | Ride-hailing is **suggested as a selected chip**, changeable in one tap, never silently applied | FR-23, FR-25 | | | |
| REG-QA-11 | After REG-QA-10 | 1. Change the suggestion to another category → 2. Save → 3. Enter `PickMe` again | The corrected category is suggested this time | FR-24 | | | |
| REG-QA-12 | Every category archived (Categories → archive each one), or a brand-new account before its first sync | 1. Open quick add → 2. Type `450` → 3. Save | Save is enabled with no chip selected; the entry saves and is listed as **Uncategorised**; Spent rises by 450.00 and counts as discretionary (Safe to spend falls by 450.00) | FR-32, A-02 | | | |

## REG-TXN — History, accounts and transfers (10)

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-TXN-01 | `demo` | 1. Open the transactions list | Newest first, grouped by day; each row shows type, amount, category/source, account | FR-100, A-08 | | | |
| REG-TXN-02 | `demo` | 1. Filter by type, then account, then date range | Only matching records are listed each time | FR-101 | | | |
| REG-TXN-03 | `demo` with a "gym" note | 1. Search `gym` | Matching entries are listed | FR-102 | | | |
| REG-TXN-04 | `fresh`, first sign-in | 1. Open any entry form | Five default accounts exist: main bank, secondary bank, cash, Binance, other | FR-10, A-10 | | | |
| REG-TXN-05 | `demo` | 1. Archive an account → 2. Open quick add → 3. Open history | Hidden from the entry form, **history still shows it** | FR-11 | | | |
| REG-TXN-06 | `demo`, note cash and bank balances | 1. Transfer 20,000 cash → bank | Cash falls 20,000, bank rises 20,000 | FR-12, A-10 | | | |
| REG-TXN-07 | `demo`, note month totals | 1. Transfer 20,000 between own accounts → 2. Re-read the dashboard | Received, Spent, Saved and the committed/discretionary split are **all unchanged** | FR-61, A-21 | | | |
| REG-TXN-08 | Transfer form open | 1. Set the same account as from and to → 2. Save | Rejected with a message | FR-60 | | | |
| REG-TXN-09 | `demo` | 1. Record a deposit into Binance | Treated as a transfer; income and expense do not change | FR-62, A-15 | | | |
| REG-TXN-10 | `demo` | 1. Change a category's committed/discretionary type → 2. Open the split | The split changes without editing any past entry | FR-22, A-18 | | | |

## REG-INC — Income (7)

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-INC-01 | `demo` | 1. Create sources of each type: salary, freelance, AdSense, crypto | All four save with cadence, currency and optional expected amount/day | FR-45, A-06 | | | |
| REG-INC-02 | Salary source with an expected day | 1. Open the app on/after the expected day | An expected salary entry exists and is confirmable | FR-49, A-12 | | | |
| REG-INC-03 | After REG-INC-02 | 1. Confirm with a received amount different from expected | The variance from expected is shown | FR-49 | | | |
| REG-INC-04 | Expected and overdue items exist | 1. Read the income total → 2. Read the pending strip | Total counts **received only**; expected/pending/overdue are listed separately | FR-56, A-12 | | | |
| REG-INC-05 | `demo` with ≥ 3 months of income | 1. Open the income summary | Shows this month, a trailing 3-month average and a lowest–highest range | FR-57, A-16 | | | |
| REG-INC-06 | Settings | 1. Enter an estimate of 160,000–210,000 → 2. Open the income screen | The estimate shows beside the computed figure | FR-58 | | | |
| REG-INC-07 | `demo`, a month with several sources | 1. Open income by source | Source amounts **sum exactly** to that month's actual income | FR-59 | | | |

## REG-FX — Multi-currency and crypto (6)

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-FX-01 | Income form | 1. Enter USD 10 received as LKR 3,100 | Stores USD 10, LKR 3,100 and a derived rate of 310 | FR-47, FR-38, A-04 | | | |
| REG-FX-02 | One USD entry at rate 300 exists | 1. Note its LKR value → 2. Add a USD entry at rate 320 → 3. Reopen the first | The first entry's LKR value **and** rate are unchanged | FR-48, A-14 | | | |
| REG-FX-03 | AdSense source | 1. Save a monthly AdSense entry as `EXPECTED` with an estimate | It is excluded from received income and shown as expected | FR-54, FR-56 | | | |
| REG-FX-04 | After REG-FX-03 | 1. Confirm it as `ACTUAL` with the credited amount | It moves into received income with the credited LKR value | FR-54 | | | |
| REG-FX-05 | Crypto source | 1. Enter +80,000 → 2. Enter −30,000 → 3. Read the YTD card | Net shows **+50,000** | FR-55, A-15 | | | |
| REG-FX-06 | Goal active, crypto entries exist | 1. Toggle "include crypto" in settings | Pace and status recalculate; default state is excluded | FR-83, A-23 | | | |

## REG-INV — Freelance projects and invoices (5)

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-INV-01 | Income screen | 1. Create a project with a client, total and three milestones | Saves and lists all three milestones | FR-50, A-13 | | | |
| REG-INV-02 | A milestone due yesterday, unreceived | 1. Reopen the app | That milestone shows as **overdue** | FR-51 | | | |
| REG-INV-03 | Overdue milestone | 1. Mark it received | An income entry is created, linked to that project and milestone | FR-52 | | | |
| REG-INV-04 | After REG-INV-03 | 1. Open the created income entry | It names which project and milestone it paid | FR-52 | | | |
| REG-INV-05 | Overdue milestone, notifications allowed | 1. Wait for / trigger the reminder → 2. Keep it overdue a second day | Exactly one notification; **not more than once per week** | FR-53 | | | |

## REG-GOAL — Savings goal (9)

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-GOAL-01 | `fresh` | 1. Create a goal: 490,000, deadline 12 months out, linked savings account holding 11,200 | Saves and opens the detail screen | FR-75, A-19 | | | |
| REG-GOAL-02 | After REG-GOAL-01 | 1. Read the required monthly saving | Shows **LKR 39,900** | FR-76, A-01 | | | |
| REG-GOAL-03 | Goal with ≥ 1 complete month | 1. Read the goal card | Required and actual pace appear **on the same card** | FR-77 | | | |
| REG-GOAL-04 | Pace 20,000 vs required 39,900 | 1. Read the status and gap | Status is behind; the LKR-per-month gap is shown | FR-78, A-19 | | | |
| REG-GOAL-05 | Goal with pace ≤ 0 | 1. Read the projection | Says no projection is available yet — **not a date** | FR-79 | | | |
| REG-GOAL-06 | Goal whose required saving > 40% of average income | 1. Open the goal | A banner shows the gap and offers both BR-11 adjustments | FR-81, A-24 | | | |
| REG-GOAL-07 | Goal detail | 1. Move the what-if slider to 39,900 | Projected date and status preview update and **nothing is saved** | FR-85 | | | |
| REG-GOAL-08 | Goal with a linked savings account | 1. Transfer 10,000 into it | Saved rises 10,000; net saving is unchanged | FR-80, A-19 | | | |
| REG-GOAL-09 | Goal active | 1. Edit the target → 2. Reopen | Saved progress is kept and the old value is logged | FR-82 | | | |

## REG-STS — Safe to spend (6)

See [`Docs/design/safe-to-spend.md`](../design/safe-to-spend.md) for the formula.

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-STS-01 | `demo`, income received this month | 1. Read the card → 2. Open the explainer sheet | Amount = received − committed − goal saving − discretionary; the sheet's four parts reconcile to it | A-23, A-19 | | | |
| REG-STS-02 | Safe to spend positive | 1. Read the card | Shows a per-day figure and the days left in the month | A-23 | | | |
| REG-STS-03 | Month fully used | 1. Read the card | Says fully used with the amount over, in **neutral colours — never red** | A-23, NFR-14 | | | |
| REG-STS-04 | Expected salary set, nothing received | 1. Read the card | Says waiting for salary rather than a negative figure | A-23, A-12 | | | |
| REG-STS-05 | Current month | 1. Page back to a past month | The card is **hidden** on past months | A-23 | | | |
| REG-STS-06 | Card visible | 1. Save a new discretionary expense | The figure updates within ~1 s with no manual refresh | FR-96, A-09 | | | |

## REG-REC — Recurring costs (6)

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-REC-01 | Recurring screen | 1. Create: Rent, 34,000, monthly, due 1st, committed | Saves as a monthly committed template | FR-65, A-17 | | | |
| REG-REC-02 | A template due today | 1. Read the dashboard due strip → 2. Confirm it at 37,500 | **37,500** is counted, not 34,000 | FR-66, A-22 | | | |
| REG-REC-03 | A template due today, unconfirmed | 1. Read the month totals | The unconfirmed amount is **not** counted as spent | FR-66 | | | |
| REG-REC-04 | After REG-REC-02 | 1. Open that entry in history | Shows expected, actual and the variance | FR-67, A-22 | | | |
| REG-REC-05 | Several active templates | 1. Read the recurring list total | Equals the sum of active templates normalised to monthly | FR-68 | | | |
| REG-REC-06 | An active template | 1. Pause it → 2. Wait past its due day | No new prompt appears; past instances remain | FR-70, A-17 | | | |

## REG-INS — Dashboard and insights (11)

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-INS-01 | Signed in | 1. Observe the first screen after sign-in | The dashboard answers "where do I stand?" with no further navigation | FR-90, A-20 | | | |
| REG-INS-02 | `demo` | 1. Add up the month's records by hand → 2. Compare with the summary card | Received, Spent and Saved match the manual sums | FR-91, A-01 | | | |
| REG-INS-03 | `demo` | 1. Read the committed vs discretionary split | Segments follow BR-13 and **transfers are absent** | FR-93, A-18 | | | |
| REG-INS-04 | `demo` with a 24,000 coffee and dining month | 1. Read the ranked categories | Ranked by total, that category placed accordingly | FR-94, A-18 | | | |
| REG-INS-05 | Dashboard open on another device | 1. Save an entry on device 1 → 2. Watch device 2 | Totals update within ~1 s with no manual refresh | FR-96, A-09 | | | |
| REG-INS-06 | `fresh` with exactly one entry | 1. Open the dashboard | Meaningful figures, **no "incomplete" or "failed" state, no warnings** | FR-97, A-03 | | | |
| REG-INS-07 | `demo` | 1. Page to the previous month | That month's own totals are shown | FR-98 | | | |
| REG-INS-08 | Any screen | 1. Review every screen for streaks, missed-day counters, "incomplete month" | None found anywhere | FR-106, A-03 | | | |
| REG-INS-09 | `fresh` | 1. Open every list and chart | Every one has an empty state and no crash | FR-112, CON-13 | | | |
| REG-INS-10 | No entry for ≥ 7 days | 1. Open the app | A neutral welcome-back message with a one-tap catch-up path, no blame wording | FR-105, NFR-14 | | | |
| REG-INS-11 | Insights screen | 1. Read the trend and income-by-source charts | Values agree with the month figures; charts render on both API levels | FR-57, FR-59 | | | |

## REG-NOTIF — Notifications (4)

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-NOTIF-01 | `fresh`, reminder never set | 1. Wait a day / check settings | Reminder is **off by default** and nothing is sent | FR-40, A-02 | | | |
| REG-NOTIF-02 | Settings | 1. Set the daily reminder 2 minutes ahead → 2. Background the app | One notification arrives at that time | FR-40 | | | |
| REG-NOTIF-03 | API 33+ device | 1. Enable a reminder on a fresh install | The runtime notification permission is requested, and denial is handled without a crash | FR-40, CON-13 | | | |
| REG-NOTIF-04 | Notification shown | 1. Tap the invoice/digest notification | The app opens on the right screen (see also REG-LOCK-05) | FR-53 | | | |

## REG-WID — Home-screen widget (5)

Also log widget results under this group for [#308](https://github.com/PulithThewmika/Fundtrail-android/issues/308).

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-WID-01 | `demo` signed in | 1. Add the widget from the launcher | Widget appears; today's spend and safe to spend match the app | A-23 | | | |
| REG-WID-02 | Widget placed | 1. Save a new expense in the app | The widget updates within seconds | FR-96 | | | |
| REG-WID-03 | Widget placed | 1. Sign out | Widget shows "Sign in to FundTrail" and **no figures** | FR-03, A-20 | | | |
| REG-WID-04 | Widget settings | 1. Change opacity, corners and text size → 2. Save | The widget redraws with the chosen look; text stays legible | NFR-15 | | | |
| REG-WID-05 | App lock enabled (#228) | 1. Read the widget | Locked state, **no money figures** | A-20 | | | |

## REG-LOCK — App lock (6)

The full device matrix for the lock lives in [`Docs/testing/app-lock.md`](app-lock.md)
([#307](https://github.com/PulithThewmika/Fundtrail-android/issues/307)). These are the
cross-cutting cases that belong in the regression run.

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-LOCK-01 | Screen lock set, app lock on | 1. Background > 60 s → 2. Reopen | The lock screen appears and the device credential unlocks it | A-20 | | | |
| REG-LOCK-02 | App lock on | 1. Background < 60 s → 2. Reopen | **No** relock | A-20 | | | |
| REG-LOCK-03 | No screen lock set | 1. Try to enable the app lock | Refused with "Set a screen lock in phone settings first"; the toggle stays off | A-20 | | | |
| REG-LOCK-04 | App lock on | 1. Open Recents → 2. Attempt a screenshot | Recents preview is blank and the screenshot is blocked | A-20, NFR-12 | | | |
| REG-LOCK-05 | App lock on, locked | 1. Tap the widget "+ Add" → 2. Unlock | Lock screen first, **then** the quick-add sheet opens | A-20 | | | |
| REG-LOCK-06 | App lock on | 1. Sign out → 2. Sign back in | The lock is disabled and its setting cleared | FR-03 | | | |

## REG-NET — Network and offline (5)

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-NET-01 | Signed in, online | 1. Airplane mode on → 2. Save an expense of 1,200 → 3. Read the dashboard → 4. Airplane mode off, wait 30 s | Save succeeds, Spent includes 1,200 at once, and the entry reaches Firestore after reconnect | FR-39, A-05 | | | |
| REG-NET-02 | Signed in, online | 1. Airplane mode on → 2. Watch the top of the screen → 3. Airplane mode off | Offline banner appears within 5 s and hides when back online | FR-110 | | | |
| REG-NET-03 | Offline with unsynced entries | 1. Force-stop the app → 2. Reopen still offline | The entries are still there and still counted | FR-39, A-05 | | | |
| REG-NET-04 | Online | 1. Throttle the emulator network to GPRS → 2. Use quick add and the dashboard | No crash, no blank screen; saves still succeed | FR-111, CON-13 | | | |
| REG-NET-05 | Simulated read failure | 1. Trigger a load error | An actionable message with retry; **a save is never failed for being offline** | FR-111 | | | |

## REG-INT — Interrupts and configuration changes (5)

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-INT-01 | Quick add, amount and note typed | 1. Rotate the device | The typed data survives; no crash | CON-13 | | | |
| REG-INT-02 | Quick add, data typed | 1. Trigger an incoming call or notification → 2. Return to the app | The entered data is intact | CON-13 | | | |
| REG-INT-03 | Quick add, data typed | 1. Switch to another app → 2. Switch back | The entered data is intact | CON-13 | | | |
| REG-INT-04 | Dashboard | 1. Switch the system theme light ↔ dark | The app follows the system theme; nothing illegible | FR-118 | | | |
| REG-INT-05 | Dashboard, quick add | 1. Set font scale 1.3 → 2. Use a screen ≤ 5" | No money figure is truncated or clipped | NFR-15 | | | |

## REG-INST — Installation and release build (5)

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-INST-01 | No app installed | 1. Install the release APK → 2. Register → 3. Use the app | Fresh install works end to end | CON-08 | | | |
| REG-INST-02 | `v0.3.0-m3` build installed and signed in | 1. Install the new build over it → 2. Open | Session and data are kept | CON-08 | | | |
| REG-INST-03 | Signed in | 1. Uninstall → 2. Reinstall → 3. Sign in | Data is restored from Firestore | FR-07, A-05 | | | |
| REG-INST-04 | Release APK installed | 1. Open More / settings | The debug demo seeder is **absent** in the release build | CON-11 | | | |
| REG-INST-05 | Release APK on a physical device | 1. Sign in with Google | Succeeds (needs the release SHA-1 in Firebase, [#278](https://github.com/PulithThewmika/Fundtrail-android/issues/278)) | FR-05 | | | |

## REG-A11Y — Accessibility (5)

Findings here are usually `P2`. Summarise them for the PDF
([#273](https://github.com/PulithThewmika/Fundtrail-android/issues/273)).

| ID | Pre | Steps | Expected | Req | A | B | Bug |
|---|---|---|---|---|---|---|---|
| REG-A11Y-01 | Dashboard | 1. Enable TalkBack → 2. Swipe through every control | Every control, icon buttons included, is announced meaningfully | NFR-15 | | | |
| REG-A11Y-02 | Quick add | 1. Navigate the whole save flow with TalkBack only | The entry can be completed without sight | NFR-15 | | | |
| REG-A11Y-03 | Any screen | 1. Measure icon buttons and chips | Every touch target is ≥ 48 dp | NFR-15 | | | |
| REG-A11Y-04 | Light and dark theme | 1. Check text against its background with a contrast checker | All text ≥ 4.5:1 | NFR-15 | | | |
| REG-A11Y-05 | Dashboard, goal, income | 1. Set the font scale to 200% | Screens remain usable; no money figure is cut off | NFR-15 | | | |

---

## Ownership

Each member runs the groups they did **not** build, so every area gets fresh eyes and every
member has commits in this file ([#266](https://github.com/PulithThewmika/Fundtrail-android/issues/266)).

| Member | Groups | Cases |
|---|---|---|
| `PulithThewmika` | REG-INC (7), REG-FX (6), REG-INV (5), REG-REC (6), REG-INS (11) | 35 |
| `Jayaru2003` | REG-GOAL (9), REG-STS (6), REG-WID (5), REG-LOCK (6), REG-NET (5), REG-INST (5), REG-A11Y (5) | 41 |
| `kaveen20030408` | REG-AUTH (9), REG-QA (12), REG-TXN (10), REG-NOTIF (4), REG-INT (5) | 40 |

The split keeps builders away from their own work: Jayaru takes the goal, safe-to-spend, widget
and app-lock groups, since Pulith built the app lock and goal screens and Kaveen built safe to
spend and the widget. One overlap is left on purpose — Pulith has REG-FX, which includes the
crypto YTD card he worked on; swap REG-FX with Kaveen if you want it clean.

Swap any other group if you built the feature it covers, and note the swap in the PR.

## Pass rate

Fill in once the run is complete; the PDF's testing chapter cites this
([#295](https://github.com/PulithThewmika/Fundtrail-android/issues/295)).

| | Col A | Col B |
|---|---|---|
| Cases run | | |
| Pass | | |
| Fail | | |
| Not applicable | | |
| **Pass rate** | | |
