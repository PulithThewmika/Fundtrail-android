# S3 — Goal detail wireframe

| | |
|---|---|
| **Document** | `Docs/wireframes/S3-goal-detail.md` |
| **Issue** | EPIC-002 / T2 · S3 (#14) · Parent: #11 |
| **Screen** | SCR-13 Goal (`Docs/ux/screens.md`) |
| **Fidelity** | Low: boxes and labels only. Visual polish is M4. |
| **Failure causes from** | `Docs/Scenario analysis.md` (rows `A-xx`) |
| **Index** | `Docs/wireframes/failure-causes.md` |

**Job of the screen.** Show whether the MacBook goal is reachable, with no judgement. It uses the scenario arithmetic: (490,000 − 11,200) ÷ 12 = LKR 39,900 per month.

Legend: `[n]` is a callout that matches the annotation table below. Dates are DD/MM/YYYY (NFR-20). Status is a neutral chip, never a red alarm (NFR-14, NFR-16).

---

## 1. Wireframe — Frame 1: default state

```text
+----------------------------------------------+
| Goal                          ( ... )        |
+----------------------------------------------+
| [1] MacBook Pro M4                           |
| [##.........................]  2%            |
| Saved LKR 11,200 (MacBook fund)              |
| of LKR 490,000                               |
| Remaining LKR 478,800                        |
| Deadline 07/10/2027                          |
+----------------------------------------------+
| [2] REQUIRED vs ACTUAL, per month            |
| +-------------------+ +-----------------+    |
| | Required          | | Your pace       |    |
| | LKR 39,900        | | LKR 20,000      |    |
| +-------------------+ +-----------------+    |
| ( Behind by LKR 19,900 / month )             |
| Pace = average of your last 3 completed      |
| months. Received income only.                |
|                                              |
|  Net saving, last 3 months                   |
|  50k|                                        |
|  40k|- - - - - - - - - required 39,900       |
|  30k|                                        |
|  20k| ###  ###  ###                          |
|  10k| ###  ###  ###                          |
|     +-Jul--Aug--Sep--                        |
|       18k  24k  18k                          |
+----------------------------------------------+
| [3] PROJECTED COMPLETION                     |
| 05/10/2028   at your current pace            |
| Deadline  07/10/2027                         |
| |-----o==========================o--->       |
|    deadline                    projected     |
| (about 12 months after the deadline)         |
+----------------------------------------------+
| [4] WHAT IF I SAVE A DIFFERENT AMOUNT?       |
| per month   [ 20,000 ]                       |
| 0 |----o--------------------| 100,000        |
| (My pace 20,000) (Required 39,900)           |
| (50,000)                                     |
| You would finish on  05/10/2028              |
| 12 months after your deadline                |
| ( Behind )                                   |
|            [ Reset to my pace ]              |
| Preview only. Nothing is saved.              |
+----------------------------------------------+
| [5] TRANSFERS IN        [ Add to goal ]      |
| Into MacBook fund                            |
|  LKR 10,000   02/10/2026                     |
|  LKR  5,000   18/09/2026                     |
| Transfers into this account raise "saved".   |
| They do not change your monthly net saving.  |
+----------------------------------------------+
| [6] Include crypto in projection  [ off ]    |
| Off keeps the projection conservative.       |
+----------------------------------------------+
| [ Edit goal ]     [ Mark as reached ]        |
+----------------------------------------------+
|  Insights | History | Income | Goal | More   |
+----------------------------------------------+
```

---

## 2. Wireframe — Frame 2: what-if moved

Only the what-if card and the timeline change. The date moves with the control.

```text
+----------------------------------------------+
| [3] PROJECTED COMPLETION (follows the        |
|     what-if value)                           |
| 07/10/2027                                   |
| |-----o--->                                  |
|    deadline = projected                      |
+----------------------------------------------+
| [4] WHAT IF I SAVE A DIFFERENT AMOUNT?       |
| per month   [ 39,900 ]                       |
| 0 |---------------o--------| 100,000         |
| You would finish on  07/10/2027              |
| On your deadline                             |
| ( On track )                                 |
+----------------------------------------------+
| Inset: thumb at 50,000                       |
| You would finish on  25/07/2027              |
| About 2 months before your deadline          |
| ( Ahead )                                    |
+----------------------------------------------+
```

### What-if values

Today is 07/10/2026. Months = remaining ÷ monthly amount (BR-06, BR-09).

| Monthly saving | Months to finish | Finish date | Status against 07/10/2027 |
|---|---|---|---|
| 20,000 (pace) | 23.9 | 05/10/2028 | Behind by 19,900 per month |
| 30,000 | 16.0 | 05/02/2028 | Behind by 9,900 per month |
| 39,900 (required) | 12.0 | 07/10/2027 | On track |
| 50,000 | 9.6 | 25/07/2027 | Ahead |

---

## 3. Wireframe — Frame 3: demanding goal (variant, neutral)

Shown only when required saving is above 40% of average received income (BR-10). The scenario goal is about 30%, so this banner **does not** appear by default.

```text
+----------------------------------------------+
| Goal                          ( ... )        |
+----------------------------------------------+
| This goal needs more than 40% of your        |
| average income each month.                   |
|  ( Move the deadline to MM/YYYY )            |
|  ( Lower the target to LKR X )               |
|  Edit manually    Dismiss                    |
+----------------------------------------------+
| MacBook Pro M4 ... (rest as Frame 1)         |
+----------------------------------------------+
```

Choosing an option opens the goal edit form pre-filled (SCR-40). Offers: new deadline = remaining ÷ (30% × average income) months from today; reduced target = saved + 30% × average income × months (BR-11).

---

## 4. Wireframe — Frame 4: no goal, and no projection

```text
+----------------------------------------------+
| (a) No goal yet                              |
| Set a goal to see what you need to           |
| save each month.                             |
|            [ Set a goal ]                    |
+----------------------------------------------+
| (b) Pace is zero or less                     |
| Projected completion                         |
| No projection yet. Add a few entries         |
| to see one.                                  |
|                                              |
| What-if still works: type an amount and      |
| the date it gives appears below.             |
+----------------------------------------------+
```

---

## 5. Annotations — the failure each element fixes

| Callout | Wireframe element | Cause | Failure cause (from the analysis) | How the element answers it | FRs |
|:-:|---|---|---|---|---|
| 1 | Goal header: target, saved, deadline, remaining | A-19 | Goal is emotionally present and financially invisible; does not know his savings rate | The goal is a visible entity with a progress bar. "Saved" is the balance of the goal's linked savings account, and the account name (here "MacBook fund") is shown beside it so the source is clear (BR-07). | FR-75 |
| 2 | Required per month vs your pace, with status and gap | A-19 | Goal is emotionally present and financially invisible; does not know his savings rate | Required saving, actual saving and status sit side by side. | FR-76, FR-77, FR-78 |
| 2 | Figures are computed, never typed in | A-01 | Hand-built formulas broke after row insertions | The user never maintains formulas. The app computes every figure. | FR-76 |
| 2 | Pace uses received income only | A-23 | Impulsive with discretionary spending, optimistic about income, avoidant about expenses | Projections use received income only; status is a neutral chip. | FR-77, FR-78 |
| 3 | Projected completion date against the deadline | A-19 | Goal is emotionally present and financially invisible; does not know his savings rate | The date at the current pace is shown against the deadline. | FR-79 |
| 4 | What-if slider: change the monthly amount, watch the date move | A-19 | Goal is emotionally present and financially invisible; does not know his savings rate | Lets him see what a different monthly amount does before committing. A preview only; nothing is saved. | FR-85 (proposed; see note) |
| 5 | Transfers-in list and "Add to goal" | A-19 | Goal is emotionally present and financially invisible; does not know his savings rate | "Add to goal" opens Record transfer (SCR-22) with the goal's linked savings account (here "MacBook fund") prefilled as the to-account. The list shows transfers into that account. They raise "saved" and leave net saving unchanged. | FR-80, FR-60, FR-61 |
| 6 | "Include crypto in projection" switch, default off | A-23 | Impulsive with discretionary spending, optimistic about income, avoidant about expenses | Keeps the projection conservative; crypto can be a loss. | FR-83 |
| 6 | Same switch | A-15 | No running crypto net position; cannot state year-to-date earnings | Crypto stays a separate, signed, realised figure. | FR-55, FR-83 |
| Frame 3 | Neutral banner with deadline and target offers | A-24 | The required saving may exceed what he can realistically save | Detects an infeasible goal, shows the gap, and offers a new deadline or target instead of a permanent "behind" status. | FR-81 |
| Frame 4 | "No goal yet" and "No projection yet" states | A-03 | After 13 days of diligent entry and a 3-week gap, the incomplete entries felt discouraging, so he never returned | A thin screen is helpful, not a failure. | FR-79, FR-112 |
| Whole screen | Calm layout, no alarm colour | A-20 | Every tool was too unpleasant to open daily | The screen invites a look rather than avoidance. | FR-106, NFR-14 |

**Lens 2 shared cause addressed:** *no connection to the goal* (callouts 1–5).

**Open point — the what-if control.** No analysis row names it, and SRS v1.2 has no FR for it. It extends A-19 and A-24. Issue #14 requires the control, so the decision is to keep it and add the requirement to the SRS, not to drop it. The SRS change is being raised separately. The proposed text is FR-85, Should, tracing to A-19: "preview the projected completion date for a different monthly saving, without saving it". The calculation is pure and client-side, so it needs no new data (NFR-18). Once FR-85 is merged, replace "FR-85 (proposed)" in callout 4 with "FR-85" and close this point.

---

## 6. Acceptance criteria (issue #14)

| Criterion | Where it is met |
|---|---|
| Shows required vs actual monthly savings rate | Callout 2: Required LKR 39,900 beside Your pace LKR 20,000, plus the 3-month chart |
| Shows the projected completion date | Callout 3: 05/10/2028 against the 07/10/2027 deadline |
| Includes a what-if control | Callout 4: slider, typed field and quick-pick chips, with Frame 2 showing the date moving |
| Uses the 490,000 / 11,200 / ~39,900 arithmetic | Callout 1 and 2, with the table in section 2 |
