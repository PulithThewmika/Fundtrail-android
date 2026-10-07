# FundTrail — Use case diagram

**Issue:** EPIC-001 / T3 (#8) · Parent: #1
**Source:** SRS (`Docs/srs.md`, PR #63) §2.3 use cases and §5 FR groups, including the review changes requested on #63.

Diagram source: [`use-case.puml`](use-case.puml) (PlantUML, standard UML notation). It isn't committed as an image; render it with the PlantUML extension in VS Code or IntelliJ, or run:

```bash
java -jar plantuml.jar -tpng Docs/diagrams/use-case.puml
```

**Reading the diagram**
- Kavindu is the primary actor (stick figure). Firebase Authentication (cloud), Cloud Firestore (database) and Android OS (node) are secondary actors.
- Solid lines are associations between an actor and a use case. Dashed arrows are `«include»` (always runs) and `«extend»` (runs only when its condition holds).
- **Sync data offline-first** is included by every use case that reads or writes data. Those edges are left out so the diagram stays readable. Writes go to Firestore's local cache first and sync later; Room mirrors Firestore for reads (`Docs/architecture.md` §4).
- `«extend»` **Show goal cost** runs only when the saved expense is discretionary (FR-41).
- A goal links to a savings account, and its progress is that account's balance (BR-07). Adding to the goal is a transfer into that account (UC-06, FR-80).
- Income is saved as `EXPECTED` and counts only once confirmed as `ACTUAL` (salary, milestones, AdSense estimate). Overdue is derived, not stored.
- Android OS connectivity drives the single app-wide offline banner (FR-110).

## Actors

| Actor | Kind | Role |
|---|---|---|
| User | Primary | The single user class: an individual tracking their own money (SRS §2.1). |
| Firebase Authentication | Secondary | Registers, signs in and signs out the user; deletes the account (CON-08). |
| Cloud Firestore | Secondary | Primary persistence and real-time sync (CON-09). |
| Android OS | Secondary | Delivers local notifications (reminders, overdue milestones, subscription reviews) and reports connectivity for the offline banner. |

## Coverage: every SRS FR group

| SRS group | Use cases | FRs |
|---|---|---|
| §5.1 Authentication and onboarding | UC-11 | FR-01…FR-08 |
| §5.2 Accounts | UC-12 | FR-10…FR-12 |
| §5.3 Categories | UC-13, Suggest category | FR-20…FR-25 |
| §5.4 Expense entry | UC-01, Show goal cost | FR-30…FR-41 |
| §5.5 Income | UC-02…UC-05, Confirm income EXPECTED → ACTUAL (UC-05 crypto is recorded as ACTUAL directly) | FR-45…FR-59 |
| §5.6 Transfers | UC-06 | FR-60…FR-62 |
| §5.7 Recurring costs | UC-09 | FR-65…FR-70 |
| §5.8 Savings goal | UC-08, Check goal feasibility | FR-75…FR-84 |
| §5.9 Insights | UC-07 | FR-90…FR-99 |
| §5.10 History | UC-14 | FR-100…FR-102 |
| §5.11 Gap tolerance | UC-10 | FR-105, FR-106 |
| §5.12 Reliability, errors and settings | UC-15, Sync data offline-first (offline banner) | FR-110…FR-118 |

UC-12…UC-15 are new. They cover the four FR groups that had no use case in SRS §2.3, and they need adding to the SRS use case table.
