<div align="center">

<img src="https://github.com/user-attachments/assets/350e0d8a-6be5-44df-b7da-678b47feb843#gh-light-mode-only" alt="FundTrail logo" width="260">
<img src="https://github.com/user-attachments/assets/924dc45a-9aca-46a3-91be-2c0ad859f644#gh-dark-mode-only" alt="FundTrail logo" width="260">

**Know where your money goes, whatever way it comes in.**

Personal finance tracker for people with irregular, multi-source income. Track salary, freelance, ad revenue and crypto across currencies, log an expense in seconds, and see how today's spending moves your savings goal.

<p>
  <img src="https://img.shields.io/badge/release-v1.0.0-0B6E4F?style=for-the-badge" alt="Release v1.0.0">
  <img src="https://img.shields.io/badge/status-complete-0B6E4F?style=for-the-badge" alt="Status: complete">
  <img src="https://img.shields.io/badge/minSdk-26-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="minSdk 26">
  <img src="https://img.shields.io/github/actions/workflow/status/PulithThewmika/Fundtrail-android/ci.yml?branch=main&style=for-the-badge&logo=githubactions&logoColor=white&label=CI" alt="CI">
</p>

<p>
  <img src="https://skillicons.dev/icons?i=kotlin,androidstudio,firebase,gradle,githubactions,nodejs&perline=6" alt="Kotlin, Android Studio, Firebase, Gradle, GitHub Actions, Node.js">
</p>

<p>
  <a href="#demo">Demo</a> ·
  <a href="#features">Features</a> ·
  <a href="#tech-stack">Tech stack</a> ·
  <a href="#architecture">Architecture</a> ·
  <a href="#documentation">Docs</a> ·
  <a href="#getting-started">Get started</a>
</p>

</div>

---

## Demo

https://github.com/user-attachments/assets/d57ece8d-14c4-47fa-8241-902a94e24962

## The problem

FundTrail is built around one scenario ([`Docs/Scenario analysis.pdf`](Docs/Scenario%20analysis.pdf)). Kavindu earns from four channels: a salary in LKR, freelance invoices, USD AdSense revenue and USDT on Binance. Budgeting apps assume a single currency and a predictable monthly paycheque, and they make every entry slow. So he stops logging, and he can't tell whether a purchase today pushes his savings goal back.

The product promise: logging an entry takes seconds, and the dashboard reflects it immediately, online or offline.

## Features

| Area | What it does |
|---|---|
| Quick add | Bottom-sheet entry for expenses, income and transfers, with a "this costs your goal N days" hint before you save |
| Multi-currency | Income and expenses in any currency; the LKR value and rate are locked at entry, so history never changes when rates do |
| Live dashboard | Month summary, balances per account, safe-to-spend, recent activity |
| Savings goal | Target, deadline, required per month, feasibility check, and progress driven by the linked savings account |
| Expected income & invoices | Estimated income (e.g. AdSense) you confirm when it lands; freelance invoices with overdue tracking and "mark received" |
| Recurring costs | Weekly, monthly and yearly bills that post themselves |
| Insights | This month vs. 3-month average, 6-month income range, category and trend charts |
| History | Search and filter by account, category and income source |
| Accounts & categories | Cash, bank, card and crypto accounts; archive and unarchive; custom categories |
| Offline first | Entries save locally in under a second and sync when the connection returns |
| Security | Email/password and Google sign-in, password reset, app lock (biometric or device PIN) with screenshot blocking, owner-only Firestore rules |
| Your data | CSV export of every record and full account deletion |
| Reminders | One daily reminder to log the day's spending |

## Tech stack

<table>
  <tr>
    <th align="left">Layer</th>
    <th align="left">Technology</th>
  </tr>
  <tr>
    <td>Language</td>
    <td><img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin"> <img src="https://img.shields.io/badge/Coroutines%20%26%20Flow-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Coroutines and Flow"></td>
  </tr>
  <tr>
    <td>UI</td>
    <td><img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"> <img src="https://img.shields.io/badge/Material%203-757575?style=flat-square&logo=materialdesign&logoColor=white" alt="Material 3"> <img src="https://img.shields.io/badge/Glance%20widget-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Glance"> <img src="https://img.shields.io/badge/Vico%20charts-0B6E4F?style=flat-square" alt="Vico"> <img src="https://img.shields.io/badge/Coil-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Coil"></td>
  </tr>
  <tr>
    <td>Architecture</td>
    <td><img src="https://img.shields.io/badge/MVVM%20%2B%20use%20cases-0B6E4F?style=flat-square" alt="MVVM"> <img src="https://img.shields.io/badge/Hilt-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Hilt"> <img src="https://img.shields.io/badge/Navigation%20Compose-4285F4?style=flat-square&logo=android&logoColor=white" alt="Navigation Compose"> <img src="https://img.shields.io/badge/WorkManager-3DDC84?style=flat-square&logo=android&logoColor=white" alt="WorkManager"></td>
  </tr>
  <tr>
    <td>Data</td>
    <td><img src="https://img.shields.io/badge/Room-4285F4?style=flat-square&logo=sqlite&logoColor=white" alt="Room"> <img src="https://img.shields.io/badge/Cloud%20Firestore-FFCA28?style=flat-square&logo=firebase&logoColor=black" alt="Cloud Firestore"></td>
  </tr>
  <tr>
    <td>Auth &amp; backend</td>
    <td><img src="https://img.shields.io/badge/Firebase%20Auth-FFCA28?style=flat-square&logo=firebase&logoColor=black" alt="Firebase Auth"> <img src="https://img.shields.io/badge/Google%20Sign--In-4285F4?style=flat-square&logo=google&logoColor=white" alt="Google Sign-In"> <img src="https://img.shields.io/badge/Firestore%20rules-FFCA28?style=flat-square&logo=firebase&logoColor=black" alt="Firestore rules"></td>
  </tr>
  <tr>
    <td>Build &amp; CI</td>
    <td><img src="https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white" alt="Gradle"> <img src="https://img.shields.io/badge/KSP-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="KSP"> <img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white" alt="GitHub Actions"> <img src="https://img.shields.io/badge/CodeRabbit-FF570A?style=flat-square" alt="CodeRabbit"></td>
  </tr>
  <tr>
    <td>Testing</td>
    <td><img src="https://img.shields.io/badge/JUnit4-25A162?style=flat-square&logo=junit5&logoColor=white" alt="JUnit"> <img src="https://img.shields.io/badge/Robolectric-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Robolectric"> <img src="https://img.shields.io/badge/Espresso%20%2F%20Compose%20tests-4285F4?style=flat-square&logo=android&logoColor=white" alt="Compose UI tests"> <img src="https://img.shields.io/badge/Firebase%20Emulator-FFCA28?style=flat-square&logo=firebase&logoColor=black" alt="Firebase Emulator"></td>
  </tr>
</table>

## Architecture

Strict MVVM with a pure-Kotlin domain layer. Room is the read source; writes go to Firestore, and a sync listener mirrors them back into Room, so screens work the same offline.

```
ui/      Compose screens + ViewModels (StateFlow<UiState>)
domain/  models, repository interfaces, one use case per action  (no Android imports)
data/    Room (local), Firestore + DTOs (remote), repository impls, sync
di/      Hilt modules
```

Money is stored as `Long` minor units, never `Float`/`Double`. Transfers between your own accounts are neither income nor expense.

Key decisions are recorded as ADRs in [`Docs/adr/`](Docs/adr/README.md):

| ADR | Decision |
|---|---|
| [0001](Docs/adr/0001-mvvm-usecases-hilt.md) | MVVM with a use-case layer and Hilt |
| [0002](Docs/adr/0002-firestore-structure.md) | Firestore: user subcollections and one `transactions` collection |
| [0003](Docs/adr/0003-money-and-fx.md) | Money as `Long` minor units, LKR value frozen at entry |
| [0004](Docs/adr/0004-offline-room-mirror.md) | Offline: Firestore cache for writes, Room mirror for reads |
| [0005](Docs/adr/0005-chart-approach.md) / [0006](Docs/adr/0006-trend-chart-library.md) | Plain Compose charts; Vico for the Insights trend card |
| [0007](Docs/adr/0007-daily-reminder.md) | Daily reminder: one WorkManager chain, one notification |

## Documentation

| Document | |
|---|---|
| [Scenario analysis](Docs/Scenario%20analysis.pdf) | Persona, pains, assumptions |
| [SRS](Docs/SRS.pdf) | Functional and non-functional requirements, business rules |
| [Architecture](Docs/Architecture.pdf) | Layers, package rules, write and sync path |
| [Schema](Docs/Schema.pdf) | Firestore schema and Room mirror |
| [Screens](Docs/ux/screens.pdf), [wireframes](Docs/wireframes/) | Screen inventory and wireframes S1–S6 |
| [Feature designs](Docs/design/README.md) | Goal, multi-currency, invoices, recurring, insights, safe-to-spend, security |
| [Use cases](Docs/diagrams/use-case.pdf) | Use-case diagram |
| [Testing](Docs/testing/) | Offline, regression and rules test evidence |

## Quality

- **CI** on every PR and push to `Dev`/`main`: `./gradlew assembleDebug testDebugUnitTest lintDebug` (JDK 21, ubuntu-latest).
- **Tests:** 94 unit test classes (use cases, ViewModels, repositories, sync) plus instrumented UI tests; Firestore rules tests in [`rules-tests/`](rules-tests/).
- **Review:** branch protection on `main` and `Dev` with one required approval, plus CodeRabbit on every PR.
- **Final QA** ([evidence](Docs/testing/)): crash-free run on API 37; every finding (app lock on resume, FX on expenses, goal progress, onboarding, CSV export, delete account) was fixed before the final build.

## Getting started

Requirements: **JDK 21** (or the latest Android Studio) and **Android SDK 37**. minSdk 26.

1. Clone the repo.
2. Put your `google-services.json` in `app/` (see below).
3. Build and test:

   ```bash
   ./gradlew assembleDebug testDebugUnitTest lintDebug
   ```

Without `google-services.json` the build still works with a placeholder, but sign-in and sync need a real Firebase project.

### Firebase

- **Project:** `FundTrail` (ID `fundtrail-5632e`), Spark plan. Authentication (Email/Password, Google) and Cloud Firestore.
- **Access:** the repo owner is the project Owner and the only one who deploys. Teammates have the Firebase Viewer role, enough to download `google-services.json`.
- **`google-services.json`** is not committed. Download it from Firebase console → Project settings → *FundTrail Android* (`com.fundtrail`) and place it at `app/google-services.json`. Data is protected by the Firestore rules, not by hiding this file.
- **Google sign-in:** add your debug keystore's SHA-1 (`./gradlew signingReport`) to the Firebase app, then re-download `google-services.json`. Without it the Google button says it isn't set up and email sign-in still works. On an emulator use a Google Play system image.
- **Rules and indexes:** `firestore.rules` and `firestore.indexes.json` are the source of truth. Change them in a PR, never in the console. The owner deploys with `firebase deploy --only firestore`.

### Release build

The release APK is signed with a key only the project lead holds. Keystores and `keystore.properties` are gitignored.

1. Create the keystore once, outside the repo, and back it up:

   ```bash
   keytool -genkeypair -v -keystore fundtrail-release.jks -alias fundtrail -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Add `keystore.properties` to the repo root:

   ```properties
   storeFile=C:/path/outside/the/repo/fundtrail-release.jks
   storePassword=...
   keyAlias=fundtrail
   keyPassword=...
   ```

3. Build and verify:

   ```bash
   ./gradlew assembleRelease
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```

Without `keystore.properties` the same command produces an unsigned APK, so CI and fresh clones don't break. Register the release SHA-1 and SHA-256 in Firebase, or Google sign-in fails on the release build.

Minification is off on purpose: R8 can strip the reflection-based Firestore DTOs and Hilt entry points in ways that only fail at runtime, and a shrunk build was never regression-tested.

## Project history

Planned and tracked on the [PBD Finance Tracker – Roadmap](https://github.com/users/PulithThewmika/projects/5) board: 22 epics, 300+ issues and ~100 merged PRs between 2026-10-05 and 2026-11-24.

| Milestone | Release | Scope |
|---|---|---|
| M1 – Design & Foundation | [`v1.0.0-m1`](https://github.com/PulithThewmika/Fundtrail-android/releases/tag/v1.0.0-m1) | Requirements, UX, architecture, schema, ADRs, team workflow, app skeleton |
| M2 – Core flows | [`v0.2.0-m2`](https://github.com/PulithThewmika/Fundtrail-android/releases/tag/v0.2.0-m2) | Data layer and offline sync, accounts, quick add, live dashboard, CI |
| M3 – Full features | [`v0.3.0-m3`](https://github.com/PulithThewmika/Fundtrail-android/releases/tag/v0.3.0-m3) | Savings goal, multi-currency, invoices, recurring costs, insights, value additions |
| M4 – Harden & Ship | [`v1.0.0`](https://github.com/PulithThewmika/Fundtrail-android/releases/tag/v1.0.0) | Hardening and QA, signed release, technical design document, demo |

## Team

| | |
|---|---|
| [@PulithThewmika](https://github.com/PulithThewmika) | Project lead, developer |
| [@Jayaru2003](https://github.com/Jayaru2003) | Developer |
| [@kaveen20030408](https://github.com/kaveen20030408) | Developer |

Branch naming, commit conventions and the Definition of Done are in [CONTRIBUTING.md](CONTRIBUTING.md).
