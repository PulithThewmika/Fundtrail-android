# S2 — Quick-add bottom sheet wireframe

| | |
|---|---|
| **Document** | `Docs/wireframes/S2-quick-add.md` |
| **Issue** | EPIC-002 / T2 · S2 (#13) · Parent: #11 |
| **Screen** | SCR-20 Quick-add expense (`Docs/ux/screens.md`). The issue specifies a modal bottom sheet. |
| **Fidelity** | Low: boxes and labels only. Visual polish is M4. |
| **Failure causes from** | `Docs/Scenario analysis.md` (rows `A-xx`) |
| **Index** | `Docs/wireframes/failure-causes.md` |

**Job of the screen.** This screen decides whether expenses actually get logged. It directly tests NFR-01, "quick add in at most 3 taps".

Legend: `[n]` is a callout that matches the annotation table below. `( )` is a chip. `[ ]` is a button or field.

---

## 1. Wireframe — Frame 1: default state

The sheet opens over the dimmed dashboard. The numeric keypad is already open.

```text
+----------------------------------------------+
|  (dimmed Insights dashboard behind)          |
+----------------------------------------------+
|  ===                                         |
|  Add expense                      [ X ]      |
|                                              |
| [1] Amount                                   |
|  LKR [ 1,250 |                    ]          |
|        ^ focused, numeric keypad             |
|                                              |
| [2] Category                                 |
|  (*Coffee and dining*) (Groceries)           |
|  (Ride-hailing) (Food delivery)              |
|  (Shopping) (Uncategorised) (+ New)  ->      |
|  Suggested from your recent entries.         |
|  Tap to change.                              |
|                                              |
| [3] [ Today, 14:30 ] [ Main bank ]           |
|                        last used             |
|                                              |
| [4] More options  v                          |
|                                              |
| [5] [          SAVE          ]               |
|     [ Save and add another ]                 |
+----------------------------------------------+
|   1  2  3                                    |
|   4  5  6                                    |
|   7  8  9                                    |
|   .  0  <x                                   |
+----------------------------------------------+
```

**Happy path, three taps (NFR-01):**

```
[ Tap FAB ]  ->  [ Type amount ]  ->  [ Tap Save ]   = 3 taps
Category, date and account are already filled in by the defaults.
Choosing a different category adds one tap.
```

---

## 2. Wireframe — Frame 2: after saving a discretionary expense

```text
+----------------------------------------------+
|  (Insights dashboard, sheet closed,          |
|   totals already updated)                    |
|                                              |
|                                              |
+----------------------------------------------+
| Saved. About 4.9% of your MacBook goal       |
| and 60% of this month's required saving.     |
|                               [ Undo ]       |
+----------------------------------------------+
|  Insights | History | Income | Goal | More   |
+----------------------------------------------+
```

The goal-cost message shows only for discretionary expenses, uses neutral wording, and carries an Undo action (FR-37, FR-41).

---

## 3. Wireframe — Frame 3: "More options" expanded

```text
+----------------------------------------------+
|  Add expense                      [ X ]      |
|  LKR [ 3,100 |                    ]          |
|  (Coffee and dining) (*Ride-hailing*)        |
|  [ Today, 14:30 ] [ Main bank ]              |
|                                              |
| [4] More options  ^                          |
|  Note or merchant                            |
|  [ PickMe                         ]          |
|   (typing "PickMe" suggests                  |
|    Ride-hailing; still changeable)           |
|                                              |
|  Type                                        |
|  ( Committed ) (*Discretionary*)             |
|   follows the category by default            |
|                                              |
|  Foreign currency                            |
|  Currency [ USD v ]   Amount [ 10   ]        |
|  LKR received [ 3,100 ]                      |
|  Rate (derived)  310                         |
|                                              |
|  [          SAVE          ]                  |
|  [ Save and add another ]                    |
+----------------------------------------------+
```

---

## 4. Wireframe — Frame 4: validation and offline

```text
+----------------------------------------------+
| Add expense      Offline - will sync later   |
|                                              |
|  LKR [ 0 |                        ]          |
|  Enter an amount greater than 0.             |
|                                              |
|  [       SAVE (disabled)       ]             |
+----------------------------------------------+
| Add expense      Offline - will sync later   |
|                                              |
|  LKR [ 1,250 |                    ]          |
|  [          SAVE          ]                  |
|  Save still works offline.                   |
+----------------------------------------------+
```

Only the amount is required. A missing category saves as *Uncategorised* (FR-32, BR-13). Inline messages replace blocking dialogs (FR-113).

---

## 5. Annotations — the failure each element fixes

| Callout | Wireframe element | Cause | Failure cause (from the analysis) | How the element answers it | FRs |
|:-:|---|---|---|---|---|
| 1 | Amount field is first, focused, with a numeric keypad | A-02 | Data-entry overhead exceeded his willingness | Amount first, so typing starts immediately. | FR-30, FR-32 |
| 2 | Category chips, most-used first, one pre-selected | A-02 | Data-entry overhead exceeded his willingness | A suggested category is pre-selected, so the happy path needs no category tap. | FR-31, FR-33 |
| 2 | Chip suggestion that the user can change | A-11 | Miscategorised a PickMe fare as "online retail" | The app suggests, the user confirms, and corrections are remembered. Nothing is assigned silently. | FR-23, FR-24, FR-25 |
| 3 | Date chip defaults to today | A-02 | Data-entry overhead exceeded his willingness | Date and time default to now. | FR-31 |
| 3 | Date chip allows past dates | A-03 | After 13 days of diligent entry and a 3-week gap, the incomplete entries felt discouraging, so he never returned | Back-dating makes catch-up easy. | FR-35 |
| 3 | Account chip defaults to the last used | A-10 | Captured only card transactions; omitted cash and secondary-account activity | Every channel (bank, cash, Binance) is one tap away in the same form. | FR-31, FR-10 |
| 4 | "More options" collapsed: note or merchant | A-08 | Pasted text became an unreadable block of unlabelled figures | Every record is structured, with optional fields hidden until needed. | FR-34 |
| 4 | Committed / discretionary override | A-18 | No category system; no idea of discretionary vs committed | The type follows the category and can be overridden. | FR-22 |
| 4 | Foreign-currency block with derived rate | A-04 | USD only | Original currency and amount are stored with the LKR value actually received. | FR-38 |
| 5 | "Save and add another" keeps date and account | A-03 | After 13 days of diligent entry and a 3-week gap, the incomplete entries felt discouraging, so he never returned | Fast back-fill after a gap, with no failure language. | FR-36 |
| Frame 2 | Snackbar: goal cost of a discretionary spend | A-23 | Impulsive with discretionary spending, optimistic about income, avoidant about expenses | The goal cost appears at the moment of entry, in a neutral tone. | FR-41 |
| Frame 4 | Save works the same offline; no offline label in the sheet | A-05 | No cloud synchronisation | Entry works fully offline and syncs later. Connectivity is shown only by the app-wide banner. | FR-39 |
| Frame 4 | Only the amount is required | A-02 | Data-entry overhead exceeded his willingness | Optional fields stay optional, so nothing blocks logging. | FR-32, FR-113 |

**Lens 2 shared cause addressed:** *high friction per entry* (callouts 1, 2, 3, 4).

---

## 6. Acceptance criteria (issue #13)

| Criterion | Where it is met |
|---|---|
| Amount field comes first and has focus | Callout 1: first element, focus marker, keypad open |
| Category chips, not a dropdown | Callout 2: scrollable chip row with a "+ New" chip |
| Defaults to today and the last-used account | Callout 3: "Today, 14:30" and "Main bank, last used" |
| A common expense is recordable in ≤ 3 taps | Happy-path strip in section 1: FAB, amount, Save |
