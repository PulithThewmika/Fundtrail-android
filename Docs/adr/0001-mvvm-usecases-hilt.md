# 0001. MVVM with a use-case layer and Hilt

- **Status:** Accepted
- **Date:** 2026-10-08
- **Deciders:** @PulithThewmika

## Context

Kavindu's money rules are where FundTrail earns his trust: only `ACTUAL` income counts (A-16), transfers are neither income nor expense (A-21), goal progress is the savings account's balance (A-19), and the goal cost of a spend is shown at entry (A-23). The same rules feed several screens (dashboard, goal, quick-add), so they must live in one place and be unit-tested with the scenario figures (NFR-19). The assignment fixes MVVM with ViewModels that never build their own dependencies (CON-04, CON-07), and three people build features in parallel against one pattern.

## Decision

We will use MVVM with a pure-Kotlin domain layer of single-action use cases (`ui → domain ← data`), and Hilt for dependency injection, as laid out in `Docs/architecture.md`.

## Options considered

| Option | Pros | Cons |
|--------|------|------|
| A (chosen): MVVM + use cases + Hilt | Each money rule lives once, in a class with no Android imports, so it is unit-testable. ViewModels only map results to UI state. Hilt gives `hiltViewModel()`, scoped Firebase types and test overrides. | More classes per feature. Hilt adds KSP build time and annotations to learn. |
| B: Plain MVVM, logic in ViewModels or repositories | Fewer files; quicker for simple screens. | Rules such as "only `ACTUAL` counts" get copied into the dashboard, goal and quick-add ViewModels and drift apart. Testing them needs Android and coroutine setup. |
| C: MVVM + use cases with manual DI (a hand-written container) | No annotation processing; every dependency is visible. | Wiring ViewModels with constructor arguments needs a hand-written factory per screen. Scoping (the `@ApplicationScope` sync scope, unscoped `FirebaseFirestore`) is easy to get wrong. CON-07 names Hilt as preferred. |
| D: Koin | Lighter setup, no code generation. | Errors appear at runtime instead of compile time. CON-07 names Hilt as preferred. |

## Consequences

- **Positive:** Kavindu's rules are tested once against the worked figures (39,900 required saving, 4.9% goal cost). The UI can't reach Firestore or Room directly (`ui` never imports `data`), which reviewers check with a grep. Every feature copies one reference pattern (Auth, #48).
- **Negative / trade-offs:** More boilerplate for trivial screens. Every new rule needs a use case, even a one-liner.
- **Follow-ups:** The Hilt application class, `@AndroidEntryPoint` and the Firebase module land in #42.
