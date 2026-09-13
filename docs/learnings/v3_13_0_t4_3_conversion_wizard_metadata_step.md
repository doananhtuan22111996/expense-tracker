# [ExpenseTracker] Learning — Conversion Wizard Step 1 Metadata Form (T4.3)

**Status**: Approved
**Created**: 2026-06-14
**Last Updated**: 2026-06-14
**PR**: [#172](https://github.com/doananhtuan22111996/expense-tracker/pull/172)
**Merge commit**: `78ed8ef` (pending — PR open at time of writing)
**Branch**: `feat/trips-conversion-wizard-t4.3-metadata-step`

---

## TL;DR

PR #172 replaces the stub `WizardStep1MetadataBody` with a real trip-details form (name, destination, date range, FX toggle + currency + rate). Six composables that were private to `CreateEditTripScreen` were extracted into a new `TripFormComponents.kt` so both screens share them without duplication. Self-review caught a missing `remember {}` around `FocusRequester` before merge.

---

## Background

T4.2 (PR #171) delivered the 3-step wizard VM + scaffold with stub bodies. T4.3 replaces Step 1's placeholder text with a real form. Since `CreateEditTripScreen` already had all the required form composables (`NameField`, `DestinationField`, `DateRangeRow`, `ForeignCurrencyToggle`, `RateField`, `TripDateRangeSheet`), the key decision was whether to copy, re-implement, or share them.

---

## What Was Delivered

### New file: `TripFormComponents.kt`

Extracts 6 composables + date-format helpers from `CreateEditTripScreen` into a shared `internal`-visibility file (same `trips` package):

| Component | Purpose |
|---|---|
| `TripNameField` | Name input with label, error, IME Next |
| `TripDestinationField` | Optional destination input |
| `TripDateRangeRow` | Tap-to-open date range display card |
| `TripForeignCurrencyToggle` | Toggle switch with subtitle |
| `TripRateField` | Rate input with digit/dot/comma filter + helper text |
| `TripDateRangeSheet` | M3 `DateRangePicker` in `ModalBottomSheet` |
| `TripForeignCurrencySection` | Convenience wrapper: dropdown + rate field, early-returns when off |
| `tripFormatDateRange` (internal) | Shared date formatter helper |

### `CreateEditTripScreen.kt` changes

- 6 private functions removed, call sites updated to `Trip*` names
- Net: -299 LOC (deletion of duplicated code)
- Zero behaviour change — compilation + all tests confirm

### `ConversionWizardScreen.kt` — `WizardStep1MetadataBody`

Real form replaces the 2-line stub:
- `remember { FocusRequester() }` + `LaunchedEffect(Unit)` → auto-focus on name field
- `rememberSaveable` for `showDatePicker` → survives rotation
- `verticalScroll(rememberScrollState())` on the Column → scrollable on small screens
- `TripDateRangeSheet` placed **outside** the scrollable Column (ModalBottomSheet must not live inside a scroll container)
- All validation (blank name, bad dates, zero rate) wired in VM (T4.2); UI surfaces errors via `nameError`, `dateError`, `rateError`

---

## Decisions Made

- **Decision**: Extract to `TripFormComponents.kt` rather than copy or re-implement.
  **Reason**: ~200 LOC of identical composables; duplication would drift. Same-package `internal` visibility is the right Kotlin scope — accessible to both screens, invisible outside the package.

- **Decision**: `TRIP_DATE_FORMATTER` and `tripFormatDate` are `private` in `TripFormComponents.kt`; only `tripFormatDateRange` is `internal`.
  **Reason**: `TripsScreen.kt` has its own `private val TRIP_DATE_FORMATTER` at the top level of the same package. Kotlin `internal` top-level vals in the same package collide with `private` ones — making the formatter `private` avoids the name clash. Only `tripFormatDateRange` needs to be `internal` since it's called from `ConversionWizardScreen` (T4.5 preview step also uses it).

- **Decision**: `clearFocus()` instead of `moveFocus(Down)` in the wizard's IME actions.
  **Reason**: The wizard Step 1 is a short form in a single scroll container; sequential IME navigation (`moveFocus(Down)`) can land on the wrong field after the date picker card. `clearFocus()` is simpler and correct here. `CreateEditTripScreen` keeps `moveFocus(Down)` since it has a longer sequential form.

- **Decision**: `CreateEditTripScreen` does NOT use `TripForeignCurrencySection`.
  **Reason**: `CreateEditTripScreen` has additional `isFxCurrencyLocked` state — currency picker disabled + a lock-explanation text. The wizard doesn't have this (T3.7 only applies to edit-mode). Keeping the FX section inline in `CreateEditTripScreen` avoids adding a `locked` parameter to the shared component for a single caller's edge case.

---

## Self-Review Finding (caught before merge)

**`FocusRequester` not wrapped in `remember`** — `val nameFocusRequester = FocusRequester()` creates a new instance on every recomposition (e.g., every keystroke in the name field). The `LaunchedEffect(Unit)` only fires against the original instance; after first recomposition the `.focusRequester()` modifier attaches to a new instance and the field loses focus mid-typing.

**Fix**: `val nameFocusRequester = remember { FocusRequester() }` — matching `CreateEditTripScreen`'s pattern.

**Lesson**: Any object used as a Compose modifier target (focus, scroll) must be `remember`-ed. `@Composable` functions run on every recomposition; plain `val x = SomeObject()` is NOT stable.

---

## Action Items

- [ ] Mark T4.3 → Done on v3.13.0 Notion task board — Owner: Solo
- [ ] Merge PR #172, then rebase PR #173 (T4.4) onto updated main — Owner: Solo

---

## References

- [GitHub PR #172](https://github.com/doananhtuan22111996/expense-tracker/pull/172)
- [T4.1 + T4.2 Learning](v3_13_0_t4_1_t4_2_conversion_wizard_entry_state_machine.md)
- T4.4 Row Decisions: PR #173 (open)
- T4.5 Preview: PR #174 (open)
