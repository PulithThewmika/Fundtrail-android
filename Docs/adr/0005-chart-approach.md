# 0005. Charts: plain Compose drawing, no chart library

- **Status:** Accepted
- **Date:** 2026-10-15
- **Deciders:** @PulithThewmika

## Context

The dashboard needs one chart in M2: the committed vs discretionary split, as a donut or stacked bar (FR-93, A-18). The ranked categories (FR-94) are a list, not a chart. The app must run on API 26 (minSdk 26), show an empty state for every chart (FR-112), and avoid adding a library unless it is needed. Multi-month trends are out of scope until M3 (EPIC-010). We have to choose now whether to add a chart library or draw charts ourselves.

## Decision

We will draw charts with plain Jetpack Compose and no chart library. Simple proportional shapes use layout (a `Row` of weighted boxes for the stacked bar). Anything layout can't express, such as a donut or a small trend bar set, uses Compose `Canvas`. Charts take ready-made values from the `UiState` and do no maths in the Composable (`Docs/architecture.md` §3.1).

## Options considered

| Option | Pros | Cons |
|--------|------|------|
| A (chosen): Compose layout / `Canvas` | Ships with the Compose BOM we already use, so it works on API 26 with no extra constraint. Adds no dependency or APK size. The stacked bar takes about 15 lines (`SplitCard` in `ui/dashboard/DashboardScreen.kt`). Uses Material 3 theme colours directly, so it follows light and dark themes. Empty states are a plain `if`. | Every chart is ours to build and test. Axes, labels, scrolling, zoom and touch markers would all be hand-written, which is costly for anything beyond simple shapes. Accessibility needs a `contentDescription` set by hand. |
| B: Vico (`com.patrykandpatrick.vico`, Apache-2.0) | A maintained Compose chart library. Column and line charts, axes, scrolling and markers come built in. A good fit for multi-month trends. | A new dependency and APK size for one bar. Its API has changed between major versions, the newest line is still in alpha, and pie or donut charts are marked experimental. Theming goes through its own `VicoTheme` mapped from Material 3. Its API 26 support and size must be re-checked on every version bump. |
| C: MPAndroidChart (View-based) | Mature, with many chart types. | A View inside Compose (`AndroidView`), styled through the old View theming. Not actively developed. The heaviest option for the least gain. |

## Consequences

- **Positive:** M2 charts need no new dependency and no Gradle change. They render the same on API 26 and in both themes. They are previewable with fake state, like the rest of the UI.
- **Negative / trade-offs:** Each new chart is custom code. A rich chart (axes, scrolling, interaction) would take noticeably more effort than with a library.
- **Accessibility:** Each chart sets a `contentDescription` that gives the figures in text (the split bar already does), and the legend repeats the amounts, so the chart is never the only way to read a value.
- **When to revisit:** If an M3 screen needs a chart with axes, scrolling or touch markers (for example, multi-month income or spending trends on SCR-12), evaluate Vico again in a new ADR that supersedes this one. `TODO(M3)`: decide trend-chart approach with the M3 Income/Insights trend work.
