# 0006. Trend chart: add Vico for the Insights trends card

- **Status:** Accepted
- **Date:** 2026-10-21
- **Deciders:** @PulithThewmika

## Context

[ADR-0005](0005-chart-approach.md) chose plain Compose drawing for M2 and said to evaluate Vico and MPAndroidChart 4 again if an M3 screen needed a chart with axes, scrolling or touch markers. The SCR-10 trends card in `Docs/wireframes/S6-insights-recurring.md` is that case: a value axis, a month axis, 3, 6 and 12 month ranges, three toggleable series (received, spent, saved) and a touch marker that shows one month's figures. It is a dependency decision, so it is recorded here and not in a wireframe note. The SRS has no requirement for a trend chart yet (S6 open points), so this decision covers the trends card once that FR exists; nothing else about charts changes. The app must still run on API 26 and show an empty state (FR-112).

## Decision

We will add Vico (`com.patrykandpatrick.vico`) for the trends card only, mapped to the Material 3 theme. The M2 split bar and any other simple shape stay as plain Compose layout, as in ADR-0005. This ADR supersedes [ADR-0005](0005-chart-approach.md) in part: ADR-0005 still holds for simple proportional shapes and is superseded only for charts with axes, scrolling or touch markers.

## Options considered

| Option | Pros | Cons |
|--------|------|------|
| A (rejected): Keep plain Compose `Canvas` (ADR-0005) | No dependency or APK size. Same rendering on API 26 and in both themes. | The axes, three ranges, series toggles and touch marker are all hand-written and hand-tested. ADR-0005 already called this costly for anything beyond simple shapes. |
| B (chosen): Vico (Apache-2.0; stable v3.3.1, minSdk 24, Maven Central) | Column and line charts, axes, scrolling and markers come built in. Supports API 26. Actively maintained. | A new dependency. The API changed between 2.x and 3.x. Theming goes through `VicoTheme`, mapped from Material 3. Only the stable column and line charts are needed, so the experimental pie chart is not a concern. |
| C (rejected): MPAndroidChart 4.0.1 (Apache-2.0; minSdk 23) | Mature, with a Compose module. | Published only on JitPack, so a third-party repository. 4.0 is weeks old with little track record. Own theming, not Material 3. |

## Consequences

- **Positive:** the trends card needs no custom axis or marker code, and the empty slot for a quiet month is handled by the library's data model.
- **Negative / trade-offs:** one new dependency and some APK size. Dark and light themes must be checked against the mapped colours.
- **Accessibility:** the chart sets a `contentDescription` with the figures in text, and the marker detail and legend repeat the amounts, so the chart is never the only way to read a value.
- **Follow-ups:** `TODO(M3)`: confirm the trends card and add its FR (S6 open points). `TODO(M3)`: pin the Vico version in the version catalog. `TODO(M3)`: build the trends card behind a small `TrendChart` Composable so the library can be swapped.
