# 0005. Charts: plain Compose drawing, no chart library

- **Status:** Accepted; superseded in part by [ADR-0006](0006-trend-chart-library.md) for charts with axes, scrolling or touch markers
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
| B: Vico (`com.patrykandpatrick.vico`, Apache-2.0; stable v3.3.1, minSdk 24) | An actively maintained Compose chart library on Maven Central that supports API 26. Column and line charts, axes, scrolling and markers come built in. A good fit for multi-month trends. | A new dependency and APK size for one bar. Its API has changed between major versions (2.x to 3.x). Pie charts arrived in v3.1.0 and are still marked experimental, so a donut would sit on an unstable API. Theming goes through its own `VicoTheme` mapped from Material 3. |
| C: MPAndroidChart (Apache-2.0; v4.0.1, minSdk 23) | Mature, with many chart types. 4.0 (September 2026) is a Kotlin rewrite with a Jetpack Compose module (`MPChartCompose`), built-in empty and loading states, and rounded stacked bars. | A new dependency (two modules for Compose), published only on JitPack, so we would add a third-party repository. 4.0 is weeks old after years without a stable release, so its Compose API has little track record. Theming is its own, not Material 3. A lot of library for one bar. |

## Consequences

- **Positive:** M2 charts need no new dependency and no Gradle change. They render the same on API 26 and in both themes. They are previewable with fake state, like the rest of the UI.
- **Negative / trade-offs:** Each new chart is custom code. A rich chart (axes, scrolling, interaction) would take noticeably more effort than with a library.
- **Accessibility:** Each chart sets a `contentDescription` that gives the figures in text (the split bar already does), and the legend repeats the amounts, so the chart is never the only way to read a value.
- **Revisited:** The SCR-10 trends card needs axes, ranges and a touch marker, which is the case reserved above. Settled by [ADR-0006](0006-trend-chart-library.md) (Accepted, 2026-10-21): Vico for that card, plain Compose for simple shapes. This ADR stands for everything else.
