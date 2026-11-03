# Fundtrail-android

Personal finance tracker for people with irregular, multi-source income. Track salary, freelance, ad revenue and crypto across currencies, log expenses in seconds, and see exactly how daily spending moves your savings goal. Kotlin · Jetpack Compose · MVVM · Hilt · Room · Firebase.

## Overview

FundTrail is built around a single scenario (see [`Docs/Scenario analysis.md`](Docs/Scenario%20analysis.md)): a user with four income channels and no tolerance for high-friction entry or rigid, single-currency budgeting apps. The product promise is that logging an entry costs seconds and the dashboard reflects it immediately.

- **Requirements:** [`Docs/srs.md`](Docs/srs.md) (SRS: functional and non-functional requirements, constraints, business rules).
- **Architecture:** [`Docs/architecture.md`](Docs/architecture.md). ADRs: [`Docs/adr/`](Docs/adr/README.md).
- **UX:** [`Docs/ux/screens.md`](Docs/ux/screens.md) (screen inventory), wireframes under [`Docs/wireframes/`](Docs/wireframes/).

## Firebase setup

- **Project:** `FundTrail` (ID `fundtrail-5632e`), Spark (free) plan.
- **Services:** Authentication (Email/Password) and Cloud Firestore.
- **Access:** least privilege. The repo owner (`PulithThewmika`) is the project Owner and the only one who deploys. Teammates get the **Firebase Viewer** role, which is enough to open the console and download `google-services.json` but can't change or delete resources. Ask the owner for access if you can't open the console.

### `google-services.json`

The file is **not** committed to this repo.

- **Who has it:** anyone with project access downloads it themselves. Firebase console → Project settings → General → Your apps → *FundTrail Android* (`com.fundtrail`) → `google-services.json`.
- **Where it goes:** `app/google-services.json`, next to `app/build.gradle.kts`.
- **Why it's gitignored:** it ties a build to our Firebase project. Keeping it out of a public repo stops other people's builds pointing at our project and lets each machine use its own copy. Data is protected by the Firestore rules, not by hiding this file.

### Google sign-in

"Continue with Google" needs three things in Firebase. The owner does the first and the third; each teammate supplies the second.

1. Authentication → Sign-in method → enable **Google**.
2. Project settings → *FundTrail Android* → add the **SHA-1** of every teammate's debug keystore. Get yours with `./gradlew signingReport` (the `debug` variant's `SHA1`). The release keystore's SHA-1 is added in M4.
3. Re-download `google-services.json`. It now includes the web OAuth client that the app needs, which becomes `default_web_client_id`.

Without the new `google-services.json` the app still builds. The Google button then says it isn't set up yet, and email sign-in works as before. On an emulator, use a **Google Play** system image (not "Google APIs") with a Google account added; API 26 has Play images too.

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

Strict MVVM with a domain layer, Hilt for DI, and Room mirroring Firestore for offline support. The diagram, package structure and the rules PR review checks against are in [`Docs/architecture.md`](Docs/architecture.md). Decisions are recorded as ADRs in [`Docs/adr/`](Docs/adr/README.md).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branch naming, commit conventions and the Definition of Done.
