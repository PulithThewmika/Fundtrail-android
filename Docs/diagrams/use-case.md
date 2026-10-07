# FundTrail — Use case diagram

**Issue:** EPIC-001 / T3 (#8) · Parent: #1 · **Source:** SRS (`Docs/srs.md`) §2.3 and §5

![FundTrail use case diagram](https://github.com/user-attachments/assets/7d2e9167-595a-41b1-a4b2-143ffc6caf4e)

Kavindu is the primary actor. Firebase Authentication, Cloud Firestore and Android OS (notifications, connectivity) are secondary actors. The diagram covers use cases UC-01…UC-15. `<<include>>` marks steps that always run as part of a use case. `<<extend>>` marks a step that runs only under its condition: **Show goal cost** extends quick-add for discretionary expenses (FR-41).

## Coverage: every SRS FR group

| SRS group | Use cases |
|---|---|
| §5.1 Authentication and onboarding | UC-11 |
| §5.2 Accounts | UC-12 |
| §5.3 Categories | UC-13 |
| §5.4 Expense entry | UC-01 |
| §5.5 Income | UC-02…UC-05 |
| §5.6 Transfers | UC-06 |
| §5.7 Recurring costs | UC-09 |
| §5.8 Savings goal | UC-08 |
| §5.9 Insights | UC-07 |
| §5.10 History | UC-14 |
| §5.11 Gap tolerance | UC-10 |
| §5.12 Reliability, errors and settings | UC-15 |
