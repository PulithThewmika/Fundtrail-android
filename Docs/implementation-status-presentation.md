# FundTrail — Current Implementation and Requirements Status

## 1. Purpose

This document provides a presentation-ready summary of how the current FundTrail Android application responds to the scenario problems and the requirements in the Software Requirements Specification (SRS).

It distinguishes between:

- **Implemented** — available in the current application.
- **Partially implemented** — supported by the data model or architecture, but incomplete in the user interface or workflow.
- **Not implemented** — planned in the requirements, but not currently available.

The assessment is based on the current source code and the requirements documented in [`srs.md`](./srs.md), [`Scenario analysis.md`](./Scenario%20analysis.md), and [`architecture.md`](./architecture.md).

## 2. Product response to the scenario

FundTrail is designed for a user with irregular income from salary, freelance work, AdSense and crypto, several accounts, variable spending and a savings goal.

The current implementation provides a working foundation:

- Email/password authentication.
- Account management.
- Category management.
- Fast expense entry.
- Structured transactions.
- Monthly dashboard summaries.
- Committed versus discretionary spending analysis.
- Room local caching.
- Firestore synchronization.
- Offline write support.
- Loading, empty and error states.
- Material 3 user interface.

The main unfinished areas are income management, freelance invoices, transfers, recurring expenses, history, savings goals and settings.

## 3. Scenario problem coverage

| ID | Scenario problem | FundTrail response | Status |
|---|---|---|---|
| A-01 | Spreadsheet formulas broke when rows changed | Dashboard values are calculated from stored transactions and summary use cases rather than user-maintained formulas. | **Partially implemented** — goal calculations are not complete. |
| A-02 | Expense entry required too much effort | Quick Add starts from the dashboard and provides amount-first entry, date defaults, account suggestions and category suggestions. | **Implemented** |
| A-03 | A long recording gap felt like failure | The app supports back-dating, month navigation, neutral empty states and no streak or missed-day counters. | **Partially implemented** — save-and-add-another is not complete. |
| A-04 | Previous app supported only USD | Transactions support an LKR reporting amount and optional original currency, amount and exchange-rate details. | **Partially implemented** — the complete multi-currency income UI is missing. |
| A-05 | No cloud synchronization or offline use | Firestore provides cloud storage and offline queuing; Room mirrors data locally and `SyncManager` applies snapshots. | **Implemented at architecture level** |
| A-06 | Previous app assumed one income source | The model supports salary, freelance, AdSense, crypto and other income sources. | **Partially implemented** — the Income screen is a placeholder. |
| A-07 | Fixed budgets did not reflect irregular income | Monthly summaries use actual recorded income and expenses for the selected month. | **Partially implemented** — income estimates and averages are missing. |
| A-08 | Notes became unstructured figures | Transactions store amount, date, account, category, type, status, note and source fields. | **Implemented for current transaction scope** |
| A-09 | Data entry produced no useful insight | Insights shows received, spent, saved, income by source, spending nature and top categories. | **Partially implemented** — goal insight is still a placeholder. |
| A-10 | Bank summaries omitted cash and other accounts | Accounts can be created, renamed, archived and selected when adding an expense. | **Partially implemented** — transfer and balance workflows are missing. |
| A-11 | Spending was categorised incorrectly | Users can manage categories, choose suggested categories and override committed/discretionary type. | **Partially implemented** — merchant matching and remembered corrections are missing. |
| A-12 | Salary amount varies | `EXPECTED` and `ACTUAL` transaction statuses exist in the domain model. | **Model support only** |
| A-13 | Freelance invoices were forgotten | Requirements define invoice/project support, but no invoice or project implementation exists. | **Not implemented** |
| A-14 | AdSense income was paid in USD | Income source and original-currency fields exist in the model. | **Model support only** |
| A-15 | Crypto gain/loss was difficult to track | Income validation allows signed crypto results, and Binance can be represented as an account. | **Model support only** |
| A-16 | Actual income required multiple tools | The dashboard can summarize actual income and group it by source. | **Partially implemented** |
| A-17 | Recurring costs and subscriptions were fragmented | Recurring templates and review prompts are specified but not implemented. | **Not implemented** |
| A-18 | No committed/discretionary visibility | Categories store a default spending nature and the dashboard displays both categories separately. | **Mostly implemented** |
| A-19 | The savings goal was financially invisible | The dashboard contains a goal placeholder, but no goal entity or calculations are implemented. | **Not implemented** |
| A-20 | Previous tools were unpleasant to use | Quick Add, short forms, suggestions and neutral language reduce entry friction. | **Partially implemented** |
| A-21 | Transfers caused double counting | `TRANSFER` exists in the domain model and is excluded from summary calculations. | **Model support only** — no transfer UI/use case. |
| A-22 | Variable utilities changed monthly costs | Expected amounts exist on transactions, but recurring confirmation is not implemented. | **Model support only** |
| A-23 | The user needed neutral feedback | UI wording is neutral and ordinary spending is not presented as a failure or alarm. | **Mostly implemented** |
| A-24 | The savings target might be unrealistic | Goal feasibility and target/deadline adjustment offers are specified but not implemented. | **Not implemented** |

## 4. Functional requirements status

### 4.1 Authentication — FR-01 to FR-08

| Requirement area | Current status |
|---|---|
| Email/password sign-up | **Implemented** |
| Email/password sign-in | **Implemented** |
| Session-aware start destination | **Implemented** |
| Sign-out | **Implemented** |
| Onboarding | **Partially implemented** — currently a continuation screen; goal, account and income setup are not complete. |
| Google sign-in and password reset | **Not implemented in the current UI** |

Relevant implementation:

- `ui/auth/`
- [`MainViewModel.kt`](../app/src/main/java/com/fundtrail/ui/MainViewModel.kt)
- [`FundtrailNavHost.kt`](../app/src/main/java/com/fundtrail/ui/navigation/FundtrailNavHost.kt)

### 4.2 Accounts — FR-10 to FR-12

| Requirement | Status |
|---|---|
| Seed default accounts | **Implemented** |
| Add accounts | **Implemented** |
| Rename accounts | **Implemented** |
| Archive accounts | **Implemented** |
| Hide archived accounts from active entry selection | **Implemented** |
| Per-account balance calculation | **Not implemented** |
| Transfer between accounts | **Not implemented in the UI** |

Relevant implementation:

- [`AccountsScreen.kt`](../app/src/main/java/com/fundtrail/ui/accounts/AccountsScreen.kt)
- [`AccountsViewModel.kt`](../app/src/main/java/com/fundtrail/ui/accounts/AccountsViewModel.kt)
- `domain/model/Account.kt`

### 4.3 Categories — FR-20 to FR-25

| Requirement | Status |
|---|---|
| Seed locally relevant default categories | **Implemented** |
| Add categories | **Implemented** |
| Rename categories | **Implemented** |
| Archive categories | **Implemented** |
| Set committed/discretionary default | **Implemented** |
| Override spending nature for one expense | **Implemented** |
| Suggest recent or most-used category | **Implemented** |
| Match merchant text | **Not implemented** |
| Remember merchant corrections | **Not implemented** |
| Require visible user confirmation of suggestions | **Implemented** |

The missing merchant matching and correction memory are recorded as TODOs in `CategoryRepositoryImpl`.

### 4.4 Expense entry — FR-30 to FR-41

| Requirement | Status |
|---|---|
| Quick Add from Insights | **Implemented** |
| Amount-first entry | **Implemented** |
| Date defaults to today | **Implemented** |
| Last-used account suggestion | **Implemented** |
| Category suggestion | **Implemented** |
| Optional note or merchant field | **Implemented** |
| Back-date an expense | **Implemented** |
| Structured transaction storage | **Implemented** |
| Undo after save | **Implemented** |
| Offline save | **Implemented through Firestore offline persistence** |
| Save without a category as Uncategorised | **Not implemented** — the current form requires a selected category. |
| Save and add another | **Not implemented** |
| Foreign-currency expense entry UI | **Not implemented** |
| Daily reminder | **Not implemented** |
| Goal-cost message after discretionary spending | **Not implemented** |

Relevant implementation:

- [`QuickAddSheet.kt`](../app/src/main/java/com/fundtrail/ui/quickadd/QuickAddSheet.kt)
- [`QuickAddViewModel.kt`](../app/src/main/java/com/fundtrail/ui/quickadd/QuickAddViewModel.kt)
- [`AddExpenseUseCase.kt`](../app/src/main/java/com/fundtrail/domain/usecase/expense/AddExpenseUseCase.kt)

### 4.5 Income — FR-45 to FR-59

| Requirement | Status |
|---|---|
| Income source types | **Implemented in the domain model** |
| Expected and actual status | **Implemented in the transaction model** |
| Count only actual income in summaries | **Implemented** |
| Income by source | **Implemented in the dashboard summary** |
| Income entry screen | **Not implemented** |
| Salary confirmation | **Not implemented** |
| Freelance project and milestone management | **Not implemented** |
| Mark invoice received | **Not implemented** |
| Create one linked actual income transaction | **Not implemented** |
| Update invoice status atomically | **Not implemented** |
| Prevent duplicate invoice income | **Not implemented** |
| Overdue milestone calculation | **Not implemented** |
| Overdue milestone notification | **Not implemented** |
| AdSense expected-to-actual workflow | **Not implemented** |
| Crypto realised gain/loss workflow | **Model validation only** |
| Income estimate | **Not implemented** |
| Three-month average and six-month range | **Not implemented** |
| Pending and overdue income display | **Not implemented** |

The current transaction model has an optional `invoiceId`, but there is no `Invoice` model, invoice collection, invoice repository or invoice screen.

### 4.6 Transfers — FR-60 to FR-62

The domain model supports `TRANSFER` transactions and validates that:

- A destination account is present.
- Source and destination accounts differ.
- Transfers are actual transactions.
- Transfers have no expense category or income source.

However, the transfer screen, use case and UI workflow are not implemented.

**Status: Model support only.**

### 4.7 Recurring costs — FR-65 to FR-70

Recurring templates, due prompts, subscription review, pause/end actions and expected-versus-actual recurring confirmations are not implemented.

**Status: Not implemented.**

### 4.8 Savings goal — FR-75 to FR-85

The dashboard currently displays a neutral goal placeholder. The following are not implemented:

- Goal creation and editing.
- Target and deadline.
- Linked savings account.
- Required monthly saving.
- Actual saving pace.
- Ahead, on-track or behind status.
- Feasibility warning.
- Goal revision history.
- What-if projection.
- Goal archive.

**Status: Not implemented.**

### 4.9 Insights dashboard — FR-90 to FR-99

Currently implemented:

- Selected-month navigation.
- Received income.
- Spent amount.
- Saved amount.
- Income by source.
- Committed spending.
- Discretionary spending.
- Top spending categories.
- Loading state.
- Empty state.
- Error state with retry.
- Offline banner.

Not implemented:

- Goal progress.
- Pending income strip.
- Expected and overdue income.
- Three-month average.
- Six-month income range.
- Complete goal calculations.

**Status: Partially implemented.**

### 4.10 History — FR-100 to FR-102

The History tab currently displays a placeholder screen. Browsing, filtering and searching transactions are not implemented.

**Status: Not implemented.**

### 4.11 Reliability and settings — FR-106 and FR-110 to FR-118

| Requirement | Status |
|---|---|
| No streaks, missed-day counters or incomplete-month indicators | **Implemented** |
| App-wide offline banner | **Implemented** |
| Loading, empty and error states | **Implemented for current screens** |
| Retry after loading failures | **Implemented for current screens** |
| Inline form validation | **Implemented for current forms** |
| Settings screen | **Not implemented** |
| Income estimate settings | **Not implemented** |
| CSV export | **Not implemented** |
| Account and data deletion | **Not implemented** |
| System light/dark theme support | **Theme infrastructure exists; full validation is pending** |

## 5. Non-functional requirements status

| ID | Requirement | Current evidence | Status |
|---|---|---|---|
| NFR-01 | Basic expense entry within three taps/about ten seconds | Quick Add is opened from Insights and uses amount-first entry. | **Mostly implemented** |
| NFR-02 | Cached dashboard cold start within 2.5 seconds | Room cache and state flows support this design. | **Needs performance measurement** |
| NFR-03 | Dashboard recomputation within 300 ms | Room observation automatically recalculates summaries after changes. | **Needs performance measurement** |
| NFR-04 | History pagination and smooth scrolling at 5,000 entries | History is still a placeholder. | **Not implemented** |
| NFR-05 | Cached reads/writes work offline and sync after reconnection | Firestore offline persistence, Room and `SyncManager` are present. | **Implemented at architecture level** |
| NFR-06 | Exact integer money calculations | Monetary values use `Long` minor units. | **Implemented** |
| NFR-07 | Idempotent client-generated record IDs | Transactions use client-generated IDs. | **Implemented for transactions** |
| NFR-08 | Partial-field concurrent updates | Repository updates send changed fields only. | **Implemented for current repositories** |
| NFR-09 | No crashes on empty datasets | Current screens have loading, empty and error states. | **Partially implemented; full testing pending** |
| NFR-10 | Owner-only Firestore access and field validation | Firestore rules restrict access to the authenticated user. | **Partially implemented** — field-level validation is still pending. |
| NFR-11 | No secrets committed | Firebase setup and excluded configuration are documented in the README. | **Implemented** |
| NFR-12 | TLS and no financial values in logs | Firebase provides TLS; normal logs do not intentionally log financial values. | **Needs final review** |
| NFR-13 | User data can be deleted and is not shared | No advertising or third-party sharing is present. | **Partially implemented** — deletion is missing. |
| NFR-14 | Neutral and non-judgemental language | Current dashboard and empty-state copy is neutral. | **Mostly implemented** |
| NFR-15 | TalkBack, contrast, touch targets and large fonts | Some content descriptions and semantics exist. | **Needs accessibility audit** |
| NFR-16 | Material 3, light/dark themes and calm palette | Compose Material 3 theme is present. | **Mostly implemented** |
| NFR-17 | API 26 to latest Android compatibility | Android project configuration exists. | **Needs device-matrix validation** |
| NFR-18 | Maintainable layered architecture | UI, ViewModel, use case, repository, Room and Firestore layers are separated. | **Mostly implemented** |
| NFR-19 | Scenario-based automated tests | Unit tests cover transactions, summaries, categories, repositories and ViewModels. | **Partially implemented** |
| NFR-20 | Resource-based English UI and money/date formatting | Android string resources and `MoneyFormatter` are used. | **Mostly implemented** |
| NFR-21 | Support at least 10,000 transactions | Bounded month queries exist, but History pagination is missing. | **Partially implemented** |
| NFR-22 | Scrubbed observability | Sync and Firestore errors are logged. | **Needs final review** |
| NFR-23 | Asia/Colombo calendar and month logic | `APP_ZONE` and `YearMonth` are used. | **Implemented for current date/month logic** |
| NFR-24 | Clean clone build | Gradle Kotlin DSL and version catalog are configured. | **Needs clean-build evidence** |
| NFR-25 | Clear Room data on sign-out | Sign-out and local-data clearing are part of the architecture. | **Implemented at architecture level; manual evidence pending** |

## 6. Architecture and technical implementation

The current application follows the planned MVVM structure:

```text
Compose UI
    ↓
ViewModel
    ↓
Use case
    ↓
Repository interface
    ↓
Room / Firestore data sources
```

Important technical decisions currently visible in the code:

- Domain models do not depend on Android, Room or Firebase.
- ViewModels expose `StateFlow`.
- Composables render state and send user actions to ViewModels.
- Room is used as the local read source.
- Firestore is the cloud source of truth.
- Firestore offline persistence queues writes when the device is offline.
- `SyncManager` mirrors account, category and transaction collections into Room.
- Monetary amounts are stored as integer minor units.
- Firebase rules restrict access to the authenticated owner.

## 7. Current feature maturity

### Working or mostly working

- Authentication.
- Sign-up, sign-in and sign-out.
- Account management.
- Category management.
- Quick expense entry.
- Account and category suggestions.
- Committed/discretionary expense classification.
- Monthly dashboard summaries.
- Income-by-source summary support.
- Room local cache.
- Firestore synchronization.
- Offline writes.
- Empty, loading and error states.
- Material 3 UI.
- Integer-based money calculations.
- Unit-test foundation.
- Owner-only Firestore access.

### Partially implemented

- Multi-currency support.
- Expected versus actual income.
- Income tracking.
- Crypto support.
- Transfer support.
- Merchant-based category suggestions.
- Dashboard insights.
- Accessibility.
- Firestore field validation.
- Performance verification.
- Settings and privacy features.

### Not implemented

- Income screen.
- History screen.
- Freelance projects and invoices.
- Mark invoice received.
- Automatic linked actual-income creation.
- Duplicate invoice-income prevention.
- Transfer entry UI.
- Recurring expenses.
- Savings goals.
- Goal feasibility calculations.
- Overdue invoice reminders.
- CSV export.
- Account deletion.
- Income estimates.
- Complete AdSense workflow.
- Complete crypto workflow.

## 8. Presentation conclusion

FundTrail has implemented the core foundation of a low-friction personal finance tracker. It directly addresses the original problems of slow expense entry, unstructured records, lack of immediate insight, missing account coverage and lack of offline support.

The current version should be presented as a **working partial implementation of the SRS**, not as the complete product. The architecture and transaction model already anticipate expected/actual records, multiple income sources, transfers and invoice links. However, the full income-management layer, freelance invoice workflow, recurring costs, transfer UI, savings goals, history and settings remain future implementation work.

The most important next feature for the freelance-income scenario is:

1. Add the `Invoice` domain model and persistence.
2. Add project and milestone screens.
3. Implement `MarkInvoiceReceivedUseCase`.
4. Create the linked `ACTUAL` freelance income transaction.
5. Update the invoice status and `receivedTxId` atomically.
6. Prevent duplicate income records when an invoice is already received.
7. Add unit and integration tests for successful, duplicate and offline receipt flows.

