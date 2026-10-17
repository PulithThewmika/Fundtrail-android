# FundTrail — Requirement Gathering Document

| | |
|---|---|
| **Project** | FundTrail — personal finance management for Android |
| **Purpose** | Mid-review evidence for the requirement-gathering phase |
| **Prepared** | 2026-10-17 |
| **Primary user** | Kavindu Silva, an individual with irregular, multi-source income |
| **Related documents** | [Scenario analysis](Scenario%20analysis.md), [SRS](srs.md), [Use-case diagram](diagrams/use-case.md), [Screen inventory](ux/screens.md), [Schema](schema.md), [Architecture](architecture.md) |

## 1. Purpose of the requirement-gathering phase

The requirement-gathering phase converts the assignment scenario and the user's financial-management problems into clear, testable product requirements. The focus is not only on recording financial data, but also on removing the reasons previous tools were abandoned:

- data entry takes too long;
- the data model does not support multiple income sources or currencies;
- tools collect data without returning useful insight;
- missed days make the user feel that the record is incomplete; and
- spending is not connected to a meaningful savings goal.

The resulting requirements are written so that stakeholders, designers, developers and testers share the same understanding of what FundTrail must provide.

## 2. Stakeholder and user analysis

### 2.1 Primary stakeholder

| Stakeholder | Need or expectation |
|---|---|
| Individual FundTrail user | Record income and expenses quickly, understand the current financial position, and work toward a savings goal |

### 2.2 Supporting stakeholders

| Stakeholder | Interest in the requirements |
|---|---|
| Development team | Stable scope, traceable requirements, clear business rules and acceptance criteria |
| UX/design team | Low-friction flows, understandable financial language and non-judgemental feedback |
| Testing/review team | Requirements that can be demonstrated and verified |
| Project markers | Evidence that the solution responds to the given scenario and assignment constraints |

### 2.3 User persona

Kavindu Silva is a junior software engineer in Colombo. His income is irregular and comes from salary, freelance work, AdSense and realised crypto results. He uses several accounts and currencies, wants to save for a MacBook Pro M4, and has previously stopped using tools that required too much manual work.

Important persona implications:

1. The first interaction must be fast and amount-focused.
2. The product must support expected and received money separately.
3. The app must remain useful after a gap in recording.
4. The tone must be neutral rather than guilt-inducing.
5. Every entry should return immediate insight, especially progress toward the goal.

## 3. Problem statement

The user cannot reliably understand how much money was actually received, where it was spent, or whether the savings goal is feasible. Existing tools fail because they are manual, single-currency, single-source, card-only, or provide no useful feedback.

FundTrail therefore needs one low-friction place to record all relevant financial activity and turn those records into trustworthy summaries:

> Recording an entry should take seconds, and the app should immediately show how that entry affects the user's financial position and savings goal.

## 4. Requirement-gathering methods and evidence

| Method | Evidence produced | Result |
|---|---|---|
| Scenario and persona analysis | Four failed-tool analyses and 24 stable pain-point IDs (`A-01` to `A-24`) | Converted user frustrations into product behaviours |
| Domain and income analysis | Five analysis lenses covering income, tool failures, savings, entry friction and financial awareness | Identified the core business rules and dashboard information |
| Use-case modelling | 15 use cases (`UC-01` to `UC-15`) and actor relationships | Defined the main user goals and system boundary |
| SRS preparation | Functional, non-functional, business-rule and constraint identifiers | Made requirements testable and traceable |
| UX requirement mapping | Screen and navigation inventory (`SCR-*`, `DLG-*`) | Connected requirements to user flows and screens |
| Data requirement analysis | Firestore collection and field definitions | Identified the information required to support each behaviour |
| Architecture analysis | MVVM, domain rules, offline and synchronisation rules | Confirmed technical constraints needed to preserve the user requirements |

## 5. Scope baseline

### 5.1 In scope for version 1

- Email/password authentication and optional Google sign-in.
- User accounts such as bank, cash, savings and crypto accounts.
- User-controlled expense categories with committed/discretionary classification.
- Quick manual expense entry with smart defaults.
- Multiple income sources: salary, freelance, AdSense, crypto and other income.
- Expected versus actual income and expense records.
- Multi-currency records with the actual LKR value stored for reporting.
- Freelance projects, milestones and overdue status.
- Transfers between the user's own accounts.
- Recurring income and expense templates.
- One or more savings-goal records, including computed progress and feasibility.
- Dashboard/Insights, transaction history and filtering.
- Offline entry with later synchronisation.
- Settings, data export and account deletion.

### 5.2 Out of scope for version 1

- Automatic bank integration or SMS parsing.
- Shared or multi-user budgets.
- Live exchange-rate feeds.
- Trading or portfolio valuation.
- Tax calculation.
- iOS, web or tablet-optimised clients.

## 6. Functional requirements

The complete requirement catalogue is maintained in [srs.md](srs.md). The following grouped requirements are the requirements most important to demonstrate during the mid review.

| ID range / use case | Requirement gathered | Verification example |
|---|---|---|
| FR-01–FR-08 / UC-11 | A user can create an account, sign in, remain signed in, reset a password and sign out safely | Given valid credentials, when the user signs in, then the user reaches Insights and their own data is loaded |
| FR-10–FR-12 / UC-12 | A user can create, edit, archive and review accounts | Given an archived account, when a new entry is created, then that account is not offered in the picker but its history remains |
| FR-20–FR-25 / UC-13 | A user can manage categories and classify spending as committed or discretionary | Given a category default, when the user changes the classification for one expense, then the expense keeps its own classification |
| FR-30–FR-41 / UC-01 | A user can record an expense quickly with amount, date, account, category and optional details | Given an amount and valid required fields, when Save is selected, then the expense is stored and its effect is visible immediately |
| FR-49–FR-56 / UC-02–UC-05 | A user can record salary, freelance, AdSense and realised crypto income as expected or actual | Given an expected income item, when the received amount is confirmed, then only the actual amount contributes to actual-income totals |
| FR-60–FR-62 / UC-06 | A user can transfer money between their own accounts | Given a transfer from Bank A to Bank B, when the dashboard recalculates, then it is excluded from income and expense totals |
| FR-65–FR-70 / UC-09 | A user can manage recurring costs and review subscriptions | Given a recurring expense, when its due item is confirmed, then the actual amount can differ from the expected amount |
| FR-75–FR-85 / UC-08 | A user can create and adjust a savings goal and see required versus actual progress | Given a goal and savings account, when the account balance changes, then goal progress and feasibility are recalculated |
| FR-90–FR-99 / UC-07 | The Insights screen shows received, spent, saved, category split and goal status | Given at least one actual entry, when the user opens Insights, then the summary is useful without requiring a complete month |
| FR-100–FR-102 / UC-14 | A user can browse, filter and search transaction history | Given multiple transactions, when a filter is applied, then only matching records are displayed |
| FR-105–FR-106 / UC-10 | A user can return after a gap and back-date or catch up entries without a failure state | Given a date in the past, when the user saves an entry, then it is grouped using its entered date |
| FR-110–FR-118 / UC-15 | A user can manage settings, export data and delete the account | Given a delete confirmation, when the user confirms, then the account and associated data are removed according to the product rules |

## 7. Non-functional requirements and constraints

| Area | Requirement |
|---|---|
| Usability | Expense entry should be reachable from Insights and take no more than three taps after opening the entry flow |
| Offline use | Existing data remains visible offline; new entries can be saved offline and synchronised later |
| Reliability | No screen should become blank because of a missing network connection; loading, empty and error states are explicit |
| Performance | Common screens and summaries should feel immediate on the reference Android device, including after a new entry |
| Data correctness | Actual income and expenses drive totals; expected records remain separate; transfers never inflate income or expenses |
| Currency correctness | The LKR amount recorded at the time of entry is the reporting truth; historical entries are not recomputed using a later rate |
| Privacy and ownership | A signed-in user can access only their own financial data |
| Accessibility and clarity | Forms provide validation, actionable error messages and understandable empty states |
| Architecture | UI, domain and data responsibilities remain separated as defined in [architecture.md](architecture.md) |

## 8. Core business rules

1. Only actual income and actual expenses count toward monthly totals.
2. Expected income and pending invoices are shown separately until received.
3. Transfers between the user's own accounts are neither income nor expense.
4. The stored LKR amount is the historical truth for reporting.
5. A realised crypto result may be positive or negative; unrealised holdings are not tracked.
6. Goal progress is the balance of the goal's linked savings account.
7. A withdrawal from the linked savings account reduces goal progress.
8. A received invoice creates the corresponding actual income record as one business operation.
9. The application must remain useful with partial data and after a recording gap.
10. Financial feedback must remain neutral and non-judgemental.

## 9. Key entities identified

| Entity | Purpose |
|---|---|
| User | Owns the private financial records and preferences |
| Account | Represents a bank, cash, savings or crypto money pot |
| Category | Classifies expenses and provides the committed/discretionary default |
| Transaction | Represents an expense, income or transfer |
| Invoice | Tracks a freelance project milestone from pending to received |
| Recurring template | Represents repeating income or expense expectations |
| Goal | Represents a target, deadline and linked savings account |
| Money | Stores the LKR truth plus optional original currency details |

## 10. Representative user scenarios and acceptance criteria

### Scenario 1 — Quick expense entry

**Given** the user is on Insights and has a valid account and category,  
**when** the user opens Quick Add, enters an amount and saves,  
**then** the expense is visible in history, the month summary is updated, and the goal impact is shown where applicable.

### Scenario 2 — Actual salary confirmation

**Given** a salary is expected on a known date,  
**when** the user confirms the amount actually received,  
**then** the expected item is replaced or linked to an actual income record and only the actual amount contributes to the income summary.

### Scenario 3 — Offline recording

**Given** the device is offline and the user has previously loaded their data,  
**when** the user records an expense,  
**then** the entry is saved locally, visible immediately, and synchronised when connectivity returns.

### Scenario 4 — Transfer without double counting

**Given** the user moves money from a bank account to a savings account,  
**when** the transfer is saved,  
**then** both account balances reflect the movement while income, expense and saving calculations do not count the transfer as new income.

### Scenario 5 — Gap-tolerant catch-up

**Given** the user has not recorded transactions for several days,  
**when** the user adds an entry using a past date,  
**then** the app accepts it without a missed-day warning or incomplete-state penalty.

## 11. Traceability

The requirement chain is:

```text
Assignment scenario
    -> Scenario pain point A-01...A-24
    -> User goal UC-01...UC-15
    -> SRS requirement FR/NFR/CON/BR
    -> UX screen SCR/DLG
    -> Domain entity and business rule
    -> Implementation and test evidence
```

Examples:

| User need | Scenario | Use case | SRS / design evidence |
|---|---|---|---|
| Avoid high-friction entry | A-02, A-20 | UC-01 | FR-30–FR-41, Quick Add screen |
| Support irregular income | A-06, A-12–A-16 | UC-02–UC-05 | FR-49–FR-56, transaction schema |
| Avoid double counting | A-21 | UC-06 | FR-60–FR-62, transfer business rule |
| Make the goal meaningful | A-19, A-24 | UC-08 | FR-75–FR-85, Goal screen |
| Continue after a gap | A-03 | UC-10 | FR-105–FR-106, back-dating rule |
| Work without a network | A-05 | UC-15 | Offline/synchronisation rules in architecture |

## 12. Assumptions and decisions

- The user manually records transactions; there is no bank feed in version 1.
- LKR is the reporting currency and the local timezone is Asia/Colombo.
- A user can have multiple accounts, but financial records remain private to that user.
- Exchange rates are supplied at entry time rather than fetched live.
- Firestore and Room are implementation decisions that support the offline requirement; they do not change the user-facing business rules.

## 13. Requirement-gathering outcome

The requirement-gathering phase is complete enough to support design, implementation and testing because:

- the problem and target user are explicit;
- scope boundaries are recorded;
- functional and non-functional requirements have stable IDs;
- user scenarios have acceptance-oriented outcomes;
- data entities and core business rules are identified;
- requirements trace from the scenario to screens and architecture; and
- unresolved product questions recorded in the scenario analysis have been resolved in the SRS.

The next activity is implementation validation: demonstrate that the implemented flows satisfy the acceptance criteria and preserve the gathered business rules.
