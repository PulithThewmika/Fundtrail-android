# S5 — Goal setup, adjustment offers and history wireframe

| | |
|---|---|
| **Document** | `Docs/wireframes/S5-goal-setup.md` |
| **Issue** | EPIC-011 / T4 · Secondary wireframes for M3 screens (#124) |
| **Screens** | SCR-40 Goal edit, DLG-05 Goal adjustment offers, SCR-41 Goal history and archive, and the infeasible-goal and no-income states of SCR-13 Goal (`Docs/ux/screens.md`) |
| **Fidelity** | Low: boxes and labels only. Visual polish is M4. |
| **Failure causes from** | `Docs/Scenario analysis.md` (rows `A-xx`) |
| **Index** | `Docs/wireframes/failure-causes.md` |
| **Builds on** | [S3-goal-detail.md](S3-goal-detail.md), which holds the main SCR-13 wireframe, the what-if control and the empty states |

**Job of the screens.** Let the user set a goal in one short form and see at once what it needs each month (A-19). When the goal asks for more than he can realistically save (infeasible, BR-10), say so calmly, show the gap and offer a new deadline or a lower target instead of a permanent "behind" (A-24). Until income exists the goal makes no judgement at all.

Legend: `[n]` is a callout that matches the annotation table below. Dates are DD/MM/YYYY (NFR-20). Amounts are drawn as whole rupees to keep the frames readable; the app formats them as `LKR 1,000.50` (NFR-20, BR-02), see §10. Today is 07/10/2026. Status is a neutral chip, never a red alarm (NFR-14, NFR-16).

---

## 1. Wireframe — SCR-40 Goal edit (create and edit)

```text
+----------------------------------------------+
| < Set a goal                                 |
+----------------------------------------------+
| Goal name      [ MacBook Pro M4         ]    |
| Target (LKR)   [ 490,000                ]    |
| Deadline       [ 07/10/2027        (cal) ]   |
| Linked savings [ MacBook fund         v ]    |
|                (Saved LKR 11,200)            |
+----------------------------------------------+
| [1] WHAT THIS NEEDS                          |
| Required per month       LKR 39,900          |
| Your pace                LKR 20,000          |
| ( Behind by LKR 19,900 / month )             |
| Projected finish at your pace  05/10/2028    |
+----------------------------------------------+
|        [ Save goal ]       [ Cancel ]        |
+----------------------------------------------+
```

The preview in [1] recalculates as any field changes (FR-76). Nothing is stored until Save.

### Edit variant

```text
+----------------------------------------------+
| < Edit goal                                  |
+----------------------------------------------+
| ... same fields, prefilled ...               |
| [2] Changing the target or deadline keeps    |
| your progress and logs the old value.        |
+----------------------------------------------+
```

### Prefilled from an offer

```text
+----------------------------------------------+
| < Edit goal                                  |
+----------------------------------------------+
| Goal name      [ MacBook Pro M4         ]    |
| Target (LKR)   [ 490,000                ]    |
| Deadline       [ 07/10/2027        (cal) ]   |
|                 Suggested                    |
| ... preview: Required 39,900 per month ...   |
+----------------------------------------------+
```

Opened from DLG-05 with the chosen value in place and a small "Suggested" label. The user can still change it before saving.

### Preview when the goal is infeasible

```text
| [1] WHAT THIS NEEDS                          |
| Required per month       LKR 79,800          |
| Your pace                LKR 20,000          |
| ( Needs adjusting )                          |
| That is about 60% of your average income.    |
```

---

## 2. Wireframe — SCR-13 Goal, infeasible goal variant

The same screen as S3 Frame 1, with a deadline of 07/04/2027. The banner is shown only when the required saving is above 40% of average received income (BR-10).

```text
+----------------------------------------------+
| Goal                          ( ... )        |
+----------------------------------------------+
| [1] This goal needs more than 40% of your    |
| average income each month.                   |
|  ( Move the deadline to 10/2027 )            |
|  ( Lower the target to LKR 250,600 )         |
|  Edit manually    Dismiss                    |
+----------------------------------------------+
| MacBook Pro M4                               |
| [#..........................]  2%            |
| LKR 11,200 of LKR 490,000                    |
| Deadline 07/04/2027                          |
+----------------------------------------------+
| [2] REQUIRED vs ACTUAL, per month            |
| +-------------------+ +-----------------+    |
| | Required          | | Your pace       |    |
| | LKR 79,800        | | LKR 20,000      |    |
| +-------------------+ +-----------------+    |
| Gap LKR 59,800 / month                       |
| ( Needs adjusting )                          |
+----------------------------------------------+
| [3] PROJECTED COMPLETION                     |
| 05/10/2028   at your current pace            |
| Deadline  07/04/2027                         |
+----------------------------------------------+
| ... rest as S3 Frame 1 ...                   |
+----------------------------------------------+
```

While the goal is infeasible (BR-10) the chip says "Needs adjusting" and keeps the gap in view. It does not stay on a permanent "Behind". *Dismiss* hides the banner and the chip falls back to the normal status (*ahead*, *on track* or *behind*, BR-09). "Needs adjusting" is the only status that depends on BR-10; it is named so it cannot be confused with the 12-month case, which is within the limit.

---

## 3. Wireframe — SCR-13 Goal, before any income

The state a new user sees first. BR-10 runs only once at least one month of received income exists, so there is no banner, no offers and no status chip. The goal still shows what it needs each month, which does not depend on income (BR-06).

```text
+----------------------------------------------+
| Goal                          ( ... )        |
+----------------------------------------------+
| [1] Add your income to see whether this      |
| goal fits. We check it once a month of       |
| income is recorded.                          |
|            [ Record income ]                 |
+----------------------------------------------+
| MacBook Pro M4                               |
| [#..........................]  2%            |
| LKR 11,200 of LKR 490,000                    |
| Deadline 07/10/2027                          |
+----------------------------------------------+
| [2] REQUIRED PER MONTH                       |
| LKR 39,900                                   |
| Your pace appears once you have a full       |
| month of entries.                            |
+----------------------------------------------+
| ... rest as S3 Frame 1 and Frame 4 (b) ...   |
+----------------------------------------------+
```

*Record income* opens SCR-21. In SCR-40 the preview shows the required amount only, with no "% of your average income" line.

---

## 4. Wireframe — DLG-05 Goal adjustment offers

A bottom sheet hosted in the screen (UX-07), shown over a dimmed SCR-13.

```text
+----------------------------------------------+
| (dimmed Goal screen)                         |
+==============================================+
|  Adjust your goal                            |
|                                              |
|  [1] New deadline                            |
|      07/10/2027                              |
|      Needs LKR 39,900 / month                |
|      (about 30% of your income)              |
|                                              |
|  [2] Lower target                            |
|      LKR 250,600 by 07/04/2027               |
|      Needs LKR 39,900 / month                |
|                                              |
|  [ Edit manually ]        [ Dismiss ]        |
|                                              |
|  Choosing an offer opens the goal form       |
|  prefilled. Your saved amount is kept.       |
+----------------------------------------------+
```

---

## 5. Wireframe — SCR-41 Goal history and archive

```text
+----------------------------------------------+
| < Goal history                               |
+----------------------------------------------+
| [1] CHANGES TO THIS GOAL                     |
| 07/10/2026                                   |
|  Deadline  07/04/2027 -> 07/10/2027          |
| 02/10/2026                                   |
|  Target    500,000 -> 490,000                |
+----------------------------------------------+
| [2] ARCHIVED GOALS                           |
| Laptop fund                                  |
|  LKR 180,000   ( Reached )   12/03/2026      |
+----------------------------------------------+
|     [ Create a new goal ]                    |
|  (available once the last goal is archived)  |
+----------------------------------------------+
```

### Empty state

```text
+----------------------------------------------+
| < Goal history                               |
+----------------------------------------------+
| No changes yet. Edits to your target or      |
| deadline appear here.                        |
+----------------------------------------------+
```

---

## 6. Flow

```text
SCR-13 --"Edit goal" / "Set a goal"--> SCR-40
SCR-13 --banner (BR-10)--> DLG-05 --offer--> SCR-40 (prefilled)
DLG-05 --"Edit manually"--> SCR-40
SCR-13 --"..." menu--> SCR-41
SCR-13 --"Add to goal"--> SCR-22 Record transfer
SCR-13 (no income yet) --"Record income"--> SCR-21
```

---

## 7. Annotations — the failure each element fixes

| Callout | Wireframe element | Cause | Failure cause (from the analysis) | How the element answers it | FRs |
|:-:|---|---|---|---|---|
| SCR-40 | Name, target, deadline, linked savings account | A-19 | Goal is emotionally present and financially invisible; does not know his savings rate | The goal is a real entity with a number, a date and the account that holds its money. | FR-75, BR-07 |
| SCR-40 [1] | Live "what this needs": required, pace, status, projected date | A-19 | Does not know his savings rate | The cost of the goal is visible before it is saved. | FR-76, FR-77, FR-78, FR-79 |
| SCR-40 [1] | Figures are computed, never typed in | A-01 | Hand-built formulas broke after row insertions | The user never maintains formulas. | FR-76 |
| SCR-40 [2] | "Keeps your progress and logs the old value" | A-24 | The required saving may exceed what he can realistically save | Moving the goal is safe, so he can adjust it instead of abandoning it. | FR-82 |
| SCR-13 [1] | Neutral banner above 40% of income with two offers | A-24 | The required saving may exceed what he can realistically save | Detects an infeasible goal, shows the gap and offers a new deadline or target. | FR-81 |
| SCR-13 [2] | Required vs pace with the gap and a "Needs adjusting" chip | A-19, A-24 | Does not know his savings rate; required saving may be unrealistic | Required and actual sit side by side and the gap is named, with no permanent alarm. | FR-77, FR-78, FR-81 |
| SCR-13 (no income) [1] | Neutral prompt to record income, no banner or offers | A-03, A-24 | After 13 days of diligent entry and a 3-week gap, the incomplete entries felt discouraging, so he never returned; required saving may be unrealistic | A goal with no income data is not judged. The prompt invites the next step and nothing is flagged. | FR-81, FR-106 |
| SCR-13 [3] | Projected date against the deadline | A-19 | Goal is financially invisible | The date at the current pace is shown next to the deadline. | FR-79 |
| DLG-05 [1] | New deadline offer | A-24 | The required saving may exceed what he can realistically save | Remaining ÷ (30% × average income) months from today (BR-11 a). | FR-81 |
| DLG-05 [2] | Lower target offer | A-24 | The required saving may exceed what he can realistically save | Saved + 30% × average income × months (BR-11 b). | FR-81 |
| SCR-41 [1] | Revision history | A-24 | The required saving may exceed what he can realistically save | Every change is kept, so adjusting is not hidden or lost. | FR-82 |
| SCR-41 [2] | Archived goals, Create a new goal | A-03 | After 13 days of diligent entry and a 3-week gap, the incomplete entries felt discouraging, so he never returned | A reached goal is archived with a neutral label and a new one can start. | FR-84 |
| Whole set | Calm wording, no red | A-23 | Impulsive with discretionary spending, optimistic about income, avoidant about expenses | An infeasible goal is a prompt to adjust, not a judgement. | FR-106, NFR-14 |

**Lens 2 shared causes addressed:** *no connection to the goal* (SCR-40, SCR-13), *no tolerance for lapses* (SCR-41, DLG-05).

---

## 8. Acceptance criteria (issue #124)

| Criterion | Where it is met |
|---|---|
| Low-fi wireframe covering required vs actual (A-19) | SCR-40 callout 1, SCR-13 callout 2 (and S3 Frame 1) |
| Low-fi wireframe covering the projected date | SCR-40 callout 1, SCR-13 callout 3 |
| Low-fi wireframe covering an infeasible goal (A-24) | SCR-13 callouts 1 and 2, DLG-05, SCR-40 prefilled |
| Goal state before any income (BR-10) | SCR-13 no-income frame |
| Goal setup and detail screens | SCR-40, SCR-13 variants, DLG-05, SCR-41 |

## 9. Arithmetic used

Average received income LKR 133,000 (the scenario income floor). Today is 07/10/2026. Saved is 11,200, so remaining is 478,800. Pace is 20,000.

| Figure | Value | Rule |
|---|---|---|
| Required, deadline 07/10/2027 (12 months) | 478,800 ÷ 12 = 39,900 (30% of income) | BR-06 |
| Gap at that deadline | 39,900 − 20,000 = 19,900 per month | BR-09 |
| Required, deadline 07/04/2027 (6 months) | 478,800 ÷ 6 = 79,800 (60% of income) | BR-06 |
| Infeasible limit | 40% × 133,000 = 53,200, and 79,800 is above it | BR-10 |
| Gap at the tight deadline | 79,800 − 20,000 = 59,800 per month | BR-09 |
| Offer (a): new deadline | ⌈478,800 ÷ (30% × 133,000 = 39,900)⌉ = 12 months, giving 07/10/2027 | BR-11 |
| Offer (b): lower target | 11,200 + 39,900 × 6 = 250,600 | BR-11 |
| Projected finish at pace | 478,800 ÷ 20,000 = 23.9 months, giving 05/10/2028 | BR-09 |

## 10. Open points

| Point | Detail | Owner action |
|---|---|---|
| New status | "Needs adjusting" is a fourth goal status, shown while a goal is infeasible (BR-10). SRS v1.4 adds it to BR-09 and FR-78. | Confirm the label in review. |
| Pace before income | BR-09 gives *behind* when pace ≤ 0. The no-income frame shows no chip, because BR-10 asks for a neutral prompt. | Confirm that no status is shown until the first month of income exists, and add it to BR-09 if so. |
| Whole-rupee display | NFR-20 and BR-02 specify two decimals. The frames show whole rupees. | Settle in `failure-causes.md` §4. |
