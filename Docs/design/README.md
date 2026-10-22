# FundTrail — Feature design sections (M3)

One section per M3 feature, written while the feature is fresh so M4 can focus on testing and polish (#158). The lead merges them into the design PDF in M4.

Each section follows the same outline: the problem (`A-xx` rows from [`Docs/Scenario analysis.md`](../Scenario%20analysis.md)), the design decision, the data flow (screen → ViewModel → use case → repository), the edge cases handled, and screenshots on seeded demo data (More → *Seed demo data* in a debug build, #157).

| Section | Feature | Epic | Status at the time of writing |
|---|---|---|---|
| [goal.md](goal.md) | Savings goal that feels real | EPIC-012 | Data, projection, dashboard card and quick-add hint merged; setup/detail screens (#166) in progress |
| [multi-currency.md](multi-currency.md) | Multi-currency income | EPIC-013 | Data model merged in M2 (ADR-0003); entry UI (#174–#180) in progress |
| [income-invoices.md](income-invoices.md) | Expected vs received income, invoices | EPIC-014 | Invoice data layer merged (#183); screen, salary card and reminders (#186–#194) in progress |
| [recurring.md](recurring.md) | Recurring costs | EPIC-015 | Data layer with atomic confirm/skip (#197); due card and screen (#200, #203) in progress |
| [insights.md](insights.md) | Insights trends | EPIC-016 | Use cases and screen (#207, #210) in progress |
| [rules-indexes.md](rules-indexes.md) | Security rules and indexes | EPIC-017 | Merged (#153, #156) |

Sections for features still in progress describe the agreed design from their issues and the M3 contracts (#149). Their owners update the "Status" line and swap the wireframe figure for a screenshot when the feature merges.

Screenshots live in `img/` and are taken on a debug build after *Seed demo data*, at the seeded "today" of late October 2026.

`TODO(M4)`: **no section has its screenshot yet**, so #158's "at least one screenshot" criterion is not met. Goal and Rules & indexes need a seeded debug build; the other four need their feature to land first, and point at the matching wireframe until then.
