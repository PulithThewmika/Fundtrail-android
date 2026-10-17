# FundTrail — Work Completed So Far

| | |
|---|---|
| **Purpose** | Separate mid-review summary of work completed up to 2026-10-17 |
| **Project** | FundTrail — personal finance management for Android |
| **Current branch** | `93-epic-008-t2-categories` |
| **Current HEAD** | `bdd748a` — add quick-add expense entry with category nature snapshots |

## 1. Project direction

FundTrail is an Android personal finance tracker for a user with irregular income from salary, freelance work, AdSense and crypto. The application is designed around three promises:

1. recording a transaction takes seconds;
2. all relevant accounts and income sources can be represented; and
3. the dashboard immediately explains the effect on spending, saving and the MacBook goal.

## 2. Work completed by phase

### Phase 1 — Understanding the problem and gathering requirements

- Analysed the assignment scenario and the user's four failed tools.
- Identified 24 stable pain points and requirement drivers (`A-01` to `A-24`).
- Analysed the five important product lenses:
  - irregular and multi-source income;
  - failure causes in existing tools;
  - savings-goal feasibility;
  - low-friction expense entry; and
  - committed versus discretionary spending.
- Defined the target persona and the main product promise.
- Recorded version-1 scope and explicit out-of-scope items.

**Evidence:** [Scenario analysis](Scenario%20analysis.md), [Requirement gathering](requirement-gathering.md).

### Phase 2 — Software requirements specification

- Converted the scenario findings into a structured SRS.
- Defined stable identifiers for functional requirements, non-functional requirements, constraints, business rules, decisions and use cases.
- Defined 15 primary use cases:
  - authentication and onboarding;
  - account and category management;
  - quick expense entry;
  - salary, freelance, AdSense and crypto income;
  - transfers;
  - recurring costs;
  - savings goals;
  - Insights/dashboard;
  - history;
  - gap recovery; and
  - settings and data management.
- Documented assumptions, definitions and measurable quality expectations.

**Evidence:** [SRS](srs.md), [Use-case diagram](diagrams/use-case.md).

### Phase 3 — UX and navigation definition

- Defined the authentication and main application navigation graphs.
- Defined five primary tabs: Insights, History, Income, Goal and More.
- Defined entry flows for expenses, income and transfers.
- Defined screens and dialogs for accounts, categories, recurring costs, projects, goals and settings.
- Mapped screens back to the SRS requirements.
- Included empty, loading, error and offline states in the UX rules.

**Evidence:** [Screen inventory](ux/screens.md), [Quick Add wireframe](wireframes/S2-quick-add.md), [Dashboard wireframe](wireframes/S1-dashboard.md), [Goal wireframe](wireframes/S3-goal-detail.md).

### Phase 4 — Data and architecture design

- Chosen and documented strict MVVM with a domain layer.
- Defined the dependency direction: `ui -> domain <- data`.
- Defined domain models, use-case boundaries, repository interfaces and Hilt responsibilities.
- Defined Firestore collections for users, accounts, categories, transactions, invoices, recurring items and goals.
- Defined the multi-currency model where the stored LKR amount is the reporting truth.
- Defined actual versus expected records and the transfer rules.
- Defined Firestore-to-Room synchronisation and offline behaviour.
- Recorded architecture decisions in ADRs.

**Evidence:** [Architecture](architecture.md), [Schema](schema.md), [ADR index](adr/README.md).

### Phase 5 — Implementation progress

The current branch has moved from documentation and design into feature implementation. The recent repository history shows the following completed implementation work:

| Area | Work completed |
|---|---|
| Category setup | Seeded 12 default expense categories on first sign-in: 6 committed and 6 discretionary categories |
| Category reliability | Added fixed identifiers, idempotent seeding and offline-safe support |
| Dashboard | Added a live Insights screen with month summary and committed/discretionary split |
| Sync visibility | Added sync-status domain support derived from Room and connectivity |
| Dependency wiring | Added real repositories and Hilt bindings |
| Routing | Added routing work for the challenges page |
| Quick Add | Added quick-add expense entry and preserved category nature as a snapshot on the expense |
| Delivery support | Added pull-request CI work and documented Firestore rules/index deployment |

## 3. Current implementation position

### Completed or substantially established

- Requirement baseline and traceability.
- Product scope and business rules.
- Architecture and persistence model.
- Authentication/data ownership direction.
- Category defaults and category classification.
- Insights summary and spending split foundation.
- Quick-add expense flow foundation.
- Offline/synchronisation design and sync-status foundation.

### Still requiring implementation or validation

- Complete end-to-end validation of all SRS use cases.
- Full income flows: salary, freelance milestones, AdSense and crypto results.
- Transfer flow and double-counting verification.
- Recurring-cost generation and subscription review.
- Goal creation, progress, feasibility and adjustment flows.
- Complete history filtering/search experience.
- Settings, export and account deletion validation.
- UI accessibility, error-state and offline-state verification across all screens.
- Acceptance-test evidence for the requirements before final sign-off.

These items are not missing requirements; they are the remaining implementation and validation work needed to prove the requirements in the running application.

## 4. Current technical baseline

| Area | Current direction |
|---|---|
| Platform | Android |
| Language/UI | Kotlin and Jetpack Compose |
| Application pattern | MVVM with a domain layer |
| Dependency injection | Hilt |
| Local data | Room mirror/cache |
| Remote data | Firebase Authentication and Cloud Firestore |
| Offline approach | Local Firestore queue, Room read mirror and later synchronisation |
| Reporting currency | LKR |
| Source control | Feature branch from `Dev`, reviewed through pull requests |

## 5. Evidence available for the mid review

1. [Requirement gathering document](requirement-gathering.md) — how the requirements were identified and verified.
2. [Scenario analysis](Scenario%20analysis.md) — problem, persona, pain points and analysis lenses.
3. [SRS](srs.md) — stable requirement catalogue and business rules.
4. [Use-case diagram](diagrams/use-case.md) — actors and user goals.
5. [Screen inventory](ux/screens.md) — navigation and requirement-to-screen mapping.
6. [Schema](schema.md) — entities, fields and data rules.
7. [Architecture](architecture.md) — layers, dependency rules and offline/sync behaviour.
8. [ADRs](adr/README.md) — recorded technical decisions.

## 6. Suggested mid-review explanation

> First, I analysed the given scenario and identified why the user's previous tools failed. I then converted those pain points into a structured SRS with stable requirement IDs, use cases, business rules, assumptions and scope boundaries. After that, I mapped the requirements to screens, navigation, data entities and the MVVM architecture. The project has now moved into implementation: default categories, Insights summaries, synchronisation support and the Quick Add expense flow are in place. The remaining work is to implement and validate the remaining income, transfer, recurring-cost, goal, history and settings flows against the gathered requirements.

## 7. Next steps

1. Complete the highest-value flows around expense entry, income and the savings goal.
2. Add or execute acceptance tests using the scenarios in the requirement-gathering document.
3. Verify offline, error, empty and validation states.
4. Check requirement-to-screen-to-code traceability before the final demonstration.
5. Update the implementation status after each completed use case rather than treating documentation completion as feature completion.
