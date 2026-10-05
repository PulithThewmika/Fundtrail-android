# FundTrail — Architecture (MVVM)

**Issue:** EPIC-003 / T1 (#16)
**Use:** Every feature follows the structure and rules in this document. PR review checks against §3 — these are rules, not suggestions.

FundTrail follows strict MVVM with a domain layer.

---

## 1. Architecture diagram

![FundTrail MVVM architecture](https://github.com/user-attachments/assets/fcf261ac-ab01-406b-afc3-a68074a85135)

Data flows up as `Flow`; commands flow down as `suspend` calls. Room is the read source, so screens work offline. Writes go to Firestore, whose snapshot listener syncs changes back into Room.

Core daily screens are shown. Secondary screens (Auth, Onboarding, Transactions, Recurring, Settings) follow the same chain.

### Write and sync path (provisional — finalised in #22)

![Write and sync path](https://github.com/user-attachments/assets/88884e8a-e5d8-4ad1-9f14-06d5b9158861)

---

## 2. Package structure

![Package structure and dependency direction](https://github.com/user-attachments/assets/212f4a2f-3e78-48b0-bd17-f78296e1da62)

```
com.fundtrail
├── ui                  Screens (Composables) and ViewModels, grouped by feature
│   ├── navigation      NavHost, typed routes
│   ├── theme           Material 3 theme
│   ├── common          UiState, shared components (empty, error, loading states)
│   ├── dashboard       DashboardRoute, DashboardScreen, DashboardViewModel
│   ├── quickadd        QuickAddSheet, QuickAddViewModel
│   ├── goal            GoalScreen, GoalViewModel
│   └── …               auth, onboarding, transactions, income, insights, recurring, settings
│
├── domain              Pure Kotlin: no Android, Firebase or Room imports
│   ├── model           Expense, Income, Transfer, Goal, Money, Account, Category,
│   │                   Invoice, RecurringTemplate, Result, …
│   ├── repository      Repository interfaces (incl. AuthRepository)
│   └── usecase         One class per business action, grouped by feature
│
├── data                Implementations of domain interfaces
│   ├── local           Room database, DAOs, entities
│   ├── remote          Firestore access and DTOs
│   ├── repository      *RepositoryImpl classes and mappers
│   └── sync            Firestore listeners that update Room
│
└── di                  Hilt modules: bind repository interfaces to implementations;
                        provide the @ApplicationScope CoroutineScope
```

Dependency direction: `ui → domain ← data`. `ui` never imports `data`. `di` is the only package that knows both sides.

### Core domain models

| Model | What it carries |
|---|---|
| `Money` | Original amount + currency, the LKR value, and the rate locked at entry |
| `Account` | Main bank, secondary bank, cash, Binance, other — every record has one |
| `Category` | Name + committed or discretionary flag (overridable per entry) |
| `Expense` / `Income` | `Money`, date, category, account; income has expected or received status |
| `Transfer` | Movement between the user's own accounts — never income or expense |
| `Invoice` | Freelance project and milestone, status pending / received / overdue |
| `RecurringTemplate` | Rent, gym, subscriptions — expected vs actual amount |
| `Goal` | Target, deadline, saved amount |

---

## 3. Rules

### 3.1 No logic in Composables

Composables only render state and pass user actions to the ViewModel as lambdas.

- Each screen is split into `XRoute` and `XScreen`. `XRoute` gets the ViewModel with `hiltViewModel()` and collects its state; `XScreen` is stateless and takes only state and lambdas, so it can be previewed with fake state.
- A ViewModel is never created by hand.
- ViewModels only map domain results to UI state and check input format (empty field, unparseable number). Any rule about money, goals, transfers or currency lives in a use case.

### 3.2 ViewModels expose `StateFlow` only

- Each ViewModel exposes one public `StateFlow` of its screen state. Any `MutableStateFlow` is `private`.
- No `LiveData`, `Channel`, `SharedFlow` or Compose `MutableState` is exposed.
- One-off outcomes such as "saved" or a snackbar message are fields in the state. The UI reports it handled them through a ViewModel callback (e.g. `onMessageShown()`), and the ViewModel clears the field.
- `UiState<T>` is for load-and-display screens. Form screens such as Quick-add use their own state data class.
- ViewModels call use cases only, never repositories.

### 3.3 `viewModelScope` for async work

- ViewModels launch coroutines only in `viewModelScope`, so work is cancelled when the screen is gone.
- No `GlobalScope` and no `runBlocking`.
- Composables never launch coroutines for data or business work. They may use `LaunchedEffect` / `rememberCoroutineScope` only for Compose UI APIs (e.g. `showSnackbar`, `SheetState.hide()`, scroll or animation), and then call back to the ViewModel.
- Long-lived data-layer work (sync listeners) runs in the `@ApplicationScope` `CoroutineScope` provided by Hilt in `di` — never in a `viewModelScope`.

### 3.4 Layer rules

- Use cases are named `VerbNounUseCase` and expose a single `operator fun invoke`. `Observe*` use cases return `Flow`; all others are `suspend`.
- `ui` never imports `data`. `domain` imports no Android, Room or Firebase classes.
- Repository interfaces return domain models only. Room entities and Firestore DTOs never leave `data`.
- `AuthRepository` exposes `currentUserId: Flow<String?>`. Repositories read the user id from it internally; ViewModels and use cases never pass a user id.
- Repository writes do not await Firestore server acknowledgement. They issue the write and return; the local cache applies it immediately (even offline) and the sync listener updates Room.
- Sync listeners are user-scoped: they start when `currentUserId` becomes non-null and stop when it becomes null. Sign-out is a `SignOutUseCase` that stops sync, then clears Room.

### 3.5 Domain rules every use case respects

- Currency is converted once, at entry. The stored LKR value and locked rate are authoritative and are never recomputed.
- Transfers between the user's own accounts are excluded from income and expense totals.
- Only **received** income counts toward the month summary and actual saving. Expected salary and pending or overdue invoices are shown separately. `MarkInvoiceReceivedUseCase` creates a received `Income` entry.

---

## Example

```kotlin
// ui/common
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Error(@StringRes val message: Int) : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
}

// ui/dashboard
@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeMonthSummary: ObserveMonthSummaryUseCase,
    observeGoalProgress: ObserveGoalProgressUseCase,
    observeSpendSplit: ObserveSpendSplitUseCase,
) : ViewModel() {

    val uiState: StateFlow<UiState<DashboardContent>> =
        combine(observeMonthSummary(), observeGoalProgress(), observeSpendSplit()) { summary, goal, split ->
            if (summary.hasNoEntries && goal == null) UiState.Empty
            else UiState.Success(DashboardContent(summary, goal, split))
        }
            .catch { emit(UiState.Error(R.string.error_dashboard)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}

@Composable
fun DashboardRoute(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(state = state)
}
```

The example needs `lifecycle-runtime-compose` (`collectAsStateWithLifecycle`) and `hilt-navigation-compose` (`hiltViewModel`). Retry after an error is out of scope here.
