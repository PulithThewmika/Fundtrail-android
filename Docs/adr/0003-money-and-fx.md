# 0003. Money as `Long` minor units, with the LKR value frozen at entry

- **Status:** Accepted
- **Date:** 2026-10-08
- **Deciders:** @PulithThewmika

## Context

Kavindu is paid in several currencies: AdSense in USD at a variable rate, crypto results in USDT, and freelance work that can be invoiced in USD (A-04, A-14, A-15). His previous tool only handled USD. What matters to him is the LKR that actually reached his account, and his goal progress and month totals are built from those LKR figures (A-16, A-19). Totals must add exactly (NFR-06), and past months must not change when an exchange rate moves later.

## Decision

We will store money as `Long` minor units. Every entry stores the LKR amount actually received or spent (`amountLkrMinor`) as the truth. Foreign-currency entries also store the original currency, amount and the rate used at entry (`original.fxRateMicros`), for display only. The LKR value is never recomputed (`Docs/schema.md` §4).

## Options considered

| Option | Pros | Cons |
|--------|------|------|
| A (chosen): `Long` minor units, LKR value frozen at entry | Exact integer sums, cheap to store and compare, maps directly to Firestore int64 and Room `INTEGER`. History is stable. | Each currency's decimal places come from one code table. Raw coin quantities don't fit (ETH has 18 decimals), so crypto is recorded in USDT. |
| B: `Double` | Native to Kotlin and Firestore, no conversion. | Most decimal amounts can't be represented exactly (0.1 + 0.2 ≠ 0.3), so month totals drift by cents and equality checks fail. |
| C: `BigDecimal` | Exact, with arbitrary precision. | Firestore has no decimal type, so it must be stored as a string and parsed on every read. It can't be summed in a Room query, and it is slower and heavier than needed for amounts that fit in a `Long`. |
| D: Store the original amount only and convert at today's rate (live FX re-evaluation) | Always reflects the current rate. | Last month's totals, net saving and goal progress would change whenever the rate moves, which is untrue to what he received. It also needs a live rate feed and network for every total (SRS D-03). |

## Consequences

- **Positive:** Historical totals never move when rates move. Sums are exact, so the scenario checks (39,900, 4.9%) are reproducible in unit tests. Totals need no network.
- **Negative / trade-offs:** The user enters the LKR received, so a wrong entry stays wrong until edited. Display formatting (two decimals, NFR-20) is a separate step.
- **Follow-ups:** The money formatting utilities and their unit tests land in #46.
