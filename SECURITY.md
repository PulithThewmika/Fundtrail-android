# Security Policy

FundTrail holds people's income, balances, invoices and savings goals, so we take security reports seriously. Thank you for helping keep it and its users safe.

## Supported versions

| Version | Supported |
|---|---|
| 1.0.x (latest release) | Yes |
| Earlier milestone builds (v0.x, v1.0.0-m1) | No |

Only the latest release on `main` receives security fixes.

## Reporting a vulnerability

**Please do not open a public issue, discussion or pull request for a security problem.**

Report it privately through GitHub instead: go to the **Security** tab and choose **Report a vulnerability**, or open [a new private advisory](https://github.com/PulithThewmika/Fundtrail-android/security/advisories/new) directly. Only the maintainers can see it.

A helpful report includes:

| What | Example |
|---|---|
| Where | Screen, file or Firestore collection affected |
| What happens | What an attacker can read, change or delete |
| How to reproduce | Steps, a proof of concept, or a crafted request |
| Build | App version and Android version |
| Impact | Whose data is exposed, and how |

Please don't access, change or delete other people's data while testing. Use your own test account.

## What happens next

| Step | Target |
|---|---|
| We acknowledge your report | Within 3 working days |
| We confirm the issue and its severity | Within 7 working days |
| We fix it and publish an advisory | As soon as possible, depending on complexity |

FundTrail is maintained by a small team, so these timings are best effort. We will keep you updated, and we are happy to credit you in the advisory unless you prefer to stay anonymous.

## Scope

| In scope | Out of scope |
|---|---|
| The Android app in this repository | Vulnerabilities in Firebase, Google Play services or Android itself |
| `firestore.rules`: reading or writing another user's data, or bypassing field validation | Attacks that need a rooted or already-compromised device |
| Data left on the device after sign-out or account deletion | Social engineering or phishing |
| Secrets or credentials committed to the repository | Denial of service or volume testing |
| The CI workflow in `.github/workflows` | Missing best-practice headers or settings with no demonstrated impact |

## How FundTrail protects data

Every Firestore read and write is owner-only and validated field by field on the server. The app's local data is cleared on sign-out, and an optional app lock uses the phone's own biometrics or screen lock. The full design is on the [Security wiki page](https://github.com/PulithThewmika/Fundtrail-android/wiki/Security).
