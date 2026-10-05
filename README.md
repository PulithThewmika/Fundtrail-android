# Fundtrail-android

![CodeRabbit Pull Request Reviews](https://img.shields.io/coderabbit/prs/github/PulithThewmika/Fundtrail-android?utm_source=oss&utm_medium=github&utm_campaign=PulithThewmika%2FFundtrail-android&labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit+Reviews)

Personal finance tracker for people with irregular, multi-source income. Track salary, freelance, ad revenue and crypto across currencies, log expenses in seconds, and see exactly how daily spending moves your savings goal. Kotlin · Jetpack Compose · MVVM · Hilt · Room · Firebase.

## Overview

FundTrail is built around a single scenario (see [`Doc/Scenario analysis.md`](Doc/Scenario%20analysis.md)): a user with four income channels and no tolerance for high-friction entry or rigid, single-currency budgeting apps. The product promise is that logging an entry costs seconds and the dashboard reflects it immediately.

- **Requirements:** `docs/srs.md` — TODO(M2), tracked under EPIC-001.
- **Architecture:** `docs/architecture.md` and `docs/adr/` — TODO(M2), tracked under EPIC-003 / EPIC-004.
- **UX:** wireframes under `docs/ux/` — TODO(M2), tracked under EPIC-002.

## Firebase setup

TODO(M2) — tracked under [EPIC-006] T2: Firebase project setup (#40).

`google-services.json` is **not** committed to this repo. Once the live Firebase project exists, this section will document:
- Who holds the file and how to request it
- Where it goes in the module tree
- Why it stays gitignored

## Build

TODO(M2) — tracked under [EPIC-006] T1: Android project skeleton (#37).

Once the Gradle version catalog lands (`gradle/libs.versions.toml`), this section will cover the minimum SDK/Android Studio version and the exact build command.

## Architecture

TODO(M2) — tracked under EPIC-003: Architecture & data design (#15).

MVVM with Hilt for DI and Room mirroring Firestore for offline support. Diagram and package structure land with EPIC-003 / EPIC-006; decisions are recorded as ADRs under `docs/adr/`.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branch naming, commit conventions and the Definition of Done.
