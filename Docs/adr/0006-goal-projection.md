# 0006. Goal projection: thresholds, complete-months pace, and infeasibility

- **Status:** Accepted
- **Date:** 2026-10-20
- **Deciders:** @PulithThewmika

## Context

`GetGoalProgressUseCase` (#162) turns one `Goal` and the monthly summaries since it was created
into `GoalProgress`: what Kavindu must save per month, what he is actually saving, a status, a
projected date, and - when the goal no longer fits his income - a more realistic suggestion
(A-19, A-24; schema §6.3). The issue fixed the shape of the maths but left several numbers and
edge cases for implementation to decide: the AHEAD/ON_TRACK/BEHIND thresholds, what counts as a
"complete" month for the pace average, the infeasibility income share, and the fallback rate used
to suggest a deadline when the current pace can never close the gap.

## Decision

- **Status thresholds:** AHEAD at pace >= 1.1x `requiredMonthly`, ON_TRACK at >= 0.9x, otherwise
  BEHIND, matching the issue's own wording. NOT_ENOUGH_DATA replaces all three before the first
  complete month.
- **Complete month:** any month from the goal's creation month up to (not including) the current
  month. A month with zero entries still counts as one complete month of zero net saving - we do
  not skip it. This is simpler and more honest about a real gap in saving than excluding it would
  be; a true gap-tolerant *display* (A-03) is a UI concern for the goal card, not a reason to hide
  data from the pace average.
- **Pace window:** the average net saved over the last up to 3 complete months. Fewer than 3 are
  available early in a goal's life; the average is taken over what exists.
- **Infeasible when either:**
  - `requiredMonthly > 0.5 x trailing 3-month average received income` (same window as pace), or
  - the pace is defined and `<= 0` while `remaining > 0`. NOT_ENOUGH_DATA (pace undefined) is
    never itself infeasible - there isn't enough information yet to say so.
- **Suggestions**, only filled when infeasible:
  - `suggestedTargetMinor = savedToDate + pace x monthsLeft`, only when pace > 0 (a flat or
    shrinking pace cannot support a smaller-target suggestion).
  - `suggestedDeadline`: the month `remaining` is reached at the current pace if pace > 0,
    otherwise at a fallback rate of 30% of the trailing average income - still null if that income
    is also zero.
- **Rounding:** `requiredMonthly`, `projectedDate` and `suggestedDeadline` all round up
  (`BigDecimal`, `RoundingMode.CEILING`) - rounding down would under-ask for savings or promise an
  earlier date than the numbers support. The status thresholds round their 0.9x/1.1x comparison
  points up for the same reason: we'd rather call a borderline pace BEHIND than AHEAD.
- **Month boundaries** use `Asia/Colombo` (`APP_ZONE`), consistent with the rest of the app.

## Options considered

| Option | Pros | Cons |
|--------|------|------|
| A (chosen): thresholds and shares exactly as stated in #162/#163/#164/#165 (0.9, 1.1, 0.5, and a 0.3 fallback we added) | Matches the issue text; the fallback rate is the only number not already specified, and 30% is a deliberately conservative "save something" floor below the 50% infeasibility line | The fallback rate is a guess with no scenario backing it; easy to tune later since it only affects the suggestion, never the status |
| B: skip zero-entry months in the pace average instead of counting them as zero | Rewards a user who back-fills sporadically, in the spirit of A-03's gap tolerance | Lets a goal with a 3-week gap look falsely on pace; the dashboard would say "on track" on stale data, which is worse than a BEHIND status that prompts a catch-up |

## Consequences

- **Positive:** every number in `GoalProgress` is reproducible from `Goal` and `MonthlySummary` data alone, with no hidden state; the scenario figures (490,000 target, 11,200 baseline, 12 months -> 39,900 required) and the A-24 "4 months left" reading both check out in `GetGoalProgressUseCaseTest`.
- **Negative / trade-offs:** a user who genuinely stopped entering data for a month (A-03) will see their pace drop as if they stopped saving, not just stopped logging. The goal card (future UI work) should distinguish "no entries" from "spent everything" if that distinction matters to Kavindu.
- **When to revisit:** if usability testing shows the 30% fallback rate or the zero-entry-month rule gives bad suggestions in practice, write a new ADR with the revised numbers and mark this one superseded.
