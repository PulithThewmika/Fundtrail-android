# Fundtrail-android

![CodeRabbit Pull Request Reviews](https://img.shields.io/coderabbit/prs/github/PulithThewmika/Fundtrail-android?utm_source=oss&utm_medium=github&utm_campaign=PulithThewmika%2FFundtrail-android&labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit+Reviews)

Personal finance tracker for people with irregular, multi-source income. Track salary, freelance, ad revenue and crypto across currencies, log expenses in seconds, and see exactly how daily spending moves your savings goal. Kotlin · Jetpack Compose · MVVM · Hilt · Room · Firebase.

## Overview

FundTrail is built around a single scenario (see [`Docs/Scenario analysis.md`](Docs/Scenario%20analysis.md)): a user with four income channels and no tolerance for high-friction entry or rigid, single-currency budgeting apps. The product promise is that logging an entry costs seconds and the dashboard reflects it immediately.

- **Requirements:** [`Docs/srs.md`](Docs/srs.md) (SRS: functional and non-functional requirements, constraints, business rules).
- **Architecture:** [`Docs/architecture.md`](Docs/architecture.md). ADRs under `Docs/adr/` — TODO(M2), tracked under EPIC-004.
- **UX:** wireframes under `Docs/ux/` — TODO(M2), tracked under EPIC-002.

## Firebase setup

- **Project:** `FundTrail` (ID `fundtrail-5632e`), Spark (free) plan.
- **Services:** Authentication (Email/Password) and Cloud Firestore.
- **Access:** least privilege. The repo owner (`PulithThewmika`) is the project Owner and the only one who deploys. Teammates get the **Firebase Viewer** role, which is enough to open the console and download `google-services.json` but can't change or delete resources. Ask the owner for access if you can't open the console.

### `google-services.json`

The file is **not** committed to this repo.

- **Who has it:** anyone with project access downloads it themselves. Firebase console → Project settings → General → Your apps → *FundTrail Android* (`com.fundtrail`) → `google-services.json`.
- **Where it goes:** `app/google-services.json`, next to `app/build.gradle.kts`.
- **Why it's gitignored:** it ties a build to our Firebase project. Keeping it out of a public repo stops other people's builds pointing at our project and lets each machine use its own copy. Data is protected by the Firestore rules, not by hiding this file.

### Firestore rules and indexes

`firestore.rules` and `firestore.indexes.json` in the repo root are the source of truth. Don't edit rules in the console; change the files in a PR instead. `.firebaserc` sets the default project, so no `--project` flag is needed.

Deploy from an up-to-date `Dev` after the PR that changes them merges. The first deploy needs both #59 (`firestore.rules`) and #60 (this config) merged, because `firebase.json` references both files. Deploying is done by the project owner:

```bash
npm install -g firebase-tools
firebase login
firebase deploy --only firestore
```

## Build

1. Use **JDK 21** or latest **Android Studio**.
2. **Android SDK 37**.
3. Place `google-services.json` inside the `app/` directory.
4. Run `./gradlew assembleDebug` to build.

## Architecture

Strict MVVM with a domain layer, Hilt for DI, and Room mirroring Firestore for offline support. The diagram, package structure and the rules PR review checks against are in [`Docs/architecture.md`](Docs/architecture.md). Decisions are recorded as ADRs under `Docs/adr/` — TODO(M2), tracked under EPIC-004.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branch naming, commit conventions and the Definition of Done.
