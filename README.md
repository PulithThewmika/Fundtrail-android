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
2. Project settings → *FundTrail Android* → add the **SHA-1** of every teammate's debug keystore. Get yours with `./gradlew signingReport` (the `debug` variant's `SHA1`). The release keystore's SHA-1 and SHA-256 are added too; see [Release build](#release-build).
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

### Release build

The release APK is signed with a key that only the project lead holds. The key and its passwords are never committed (`*.jks`, `*.keystore` and `keystore.properties` are gitignored).

1. **Create the keystore once, outside the repo**, and keep a backup. Losing it means a new key and a new Firebase fingerprint.

   ```
   keytool -genkeypair -v -keystore fundtrail-release.jks -alias fundtrail -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Add `keystore.properties`** to the repository root (not committed):

   ```
   storeFile=C:/path/outside/the/repo/fundtrail-release.jks
   storePassword=...
   keyAlias=fundtrail
   keyPassword=...
   ```

3. **Build and check it:**

   ```
   ./gradlew assembleRelease
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```

   The printed certificate must be the release one, and `aapt2 dump badging` should show `versionName='1.0.0'`.

Without `keystore.properties` (CI, or a fresh clone) the same command still builds, but the APK is left **unsigned** (`app-release-unsigned.apk`), so nothing breaks.

**Register the release fingerprints in Firebase**, or Google Sign-In fails on the release APK: run `keytool -list -v -keystore <your .jks>` (or `./gradlew signingReport` with the properties file present), add the **SHA-1 and SHA-256** under Project settings → *FundTrail Android*, then re-download `google-services.json` for the machine that builds the release.

**Minification is off on purpose** (`isMinifyEnabled = false`). R8 can strip or rename the reflection-based Firestore DTOs and the Hilt entry points in ways that only show up at runtime, and there was no time left to regression-test a shrunk build. Turning it on is future work.

## Architecture

Strict MVVM with a domain layer, Hilt for DI, and Room mirroring Firestore for offline support. The diagram, package structure and the rules PR review checks against are in [`Docs/architecture.md`](Docs/architecture.md). Decisions are recorded as ADRs in [`Docs/adr/`](Docs/adr/README.md).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branch naming, commit conventions and the Definition of Done.
