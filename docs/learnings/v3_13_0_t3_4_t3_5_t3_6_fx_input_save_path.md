# [ExpenseTracker] Learning — Trips: FX Input Mode + Save Path (T3.4 + T3.5 + T3.6)

**Status**: Approved
**Created**: 2026-06-12
**Last Updated**: 2026-06-12
**PR**: https://github.com/doananhtuan22111996/expense-tracker/pull/168
**Branch**: `feat/trips-foreign-input-t3.4-t3.5`

---

## TL;DR

Completes the Add/Edit integration epic (Epic 3) for Trips. Wires TripChip + FX sub-form into the
save path so `trip_id` and `amount_foreign_minor` are actually persisted on every transaction.
Before this PR the trip chip and FX fields were purely cosmetic — nothing was saved.

---

## Background

Epic 3 spans the Add/Edit Transaction screen integration with Trips. Earlier tasks (T3.1–T3.3)
added the `TripPickerBottomSheet`, `TripChip`, and the `ForeignAmountField`/`RateOverrideField`
composables as visible UI. T3.4 wired the chip into the picker, T3.5 completed the FX sub-form
logic, and T3.6 threaded both through to the DB save path, closing the epic.

---

## What Changed

### Domain layer (T3.6)

| File | Change |
|------|--------|
| `domain/model/Transaction.kt` | `+tripId: Long? = null`, `+amountForeignMinor: Long? = null` |
| `domain/repository/TransactionRepository.kt` | `addTransaction` gains `tripId: Long?` + `amountForeignMinor: Long?` optional params |
| `repository/TransactionRepositoryImpl.kt` | Impl passes both params through to entity insert |
| `repository/mapper/TransactionMapper.kt` | `toDomain` + `toEntity` round-trip both new fields |

### ViewModel (T3.5 + T3.6)

**AddEditTransactionViewModel:**
- `saveTransaction` computes `amountForeignMinor` via `AmountFormatter.parseAmount(amountForeignText)` when `isForeignCurrencyMode`; passes `tripId` from `selectedTrip`
- FX null guard: if `isForeignCurrencyMode && amountForeignMinor == null` → sets error, returns (mirrors existing amount/category guards)
- Edit mode `loadInitialData`: loads `existingTrip` via `getTripById(transaction.tripId)` wrapped in try/catch; seeds `amountForeignText` from `transaction.amountForeignMinor?.toString()`; seeds `rateOverrideText` from `existingTrip.foreignToHomeRate` formatted via `formatRate()`

**hasUnsavedChanges extension (T3.5):**
- Edit mode: `+selectedTrip?.id != original.tripId`, `+AmountFormatter.parseAmount(amountForeignText) != original.amountForeignMinor`
- Add mode: `+rateOverrideText.isNotBlank()`

### HomeViewModel (T3.6)

`undoDelete` re-insert preserves `tripId` and `amountForeignMinor` from the deleted transaction —
without this, undo would silently strip trip associations.

### UI (T3.4)

`TripChip` repositioned to the top of the form (above Amount) so trip context is visible before
the user enters any amounts.

---

## Decisions Made

**Decision**: Rate override is NOT persisted on the transaction entity.
**Reason**: ADR-002. The original exchange rate at time of transaction is unrecoverable after the
trip's rate is updated. Edit mode seeds `rateOverrideText` from the trip's current stored rate,
which is the best available approximation. Storing a per-transaction rate snapshot adds schema
complexity not warranted at this stage.

**Decision**: `getTripById` in edit mode is wrapped in `try/catch`, non-fatal on failure.
**Reason**: A trip may have been deleted between when the transaction was saved and when the edit
screen opens. Treating a missing trip as non-fatal (`selectedTrip = null`) lets the form load
and the user proceed. A fatal error here would permanently brick editing old FX transactions.

**Decision**: `isForeignCurrencyMode` derived from `selectedTrip?.foreignCurrencyCode.isNullOrBlank()`.
**Reason**: Keeps non-trip transactions and home-currency-trip transactions untouched by the FX
path. No flag needed — the trip's own data drives the branching.

**Decision**: `amountForeignMinor` follows the same raw-Long convention as `amount`.
**Reason**: Consistency with the existing amount storage contract (minorUnitDigits=0 for JPY/VND/KRW,
minorUnitDigits=2 for USD/EUR/SGD). No second convention needed.

---

## Tests Added (39 new)

| File | Count | Coverage |
|------|-------|----------|
| `ForeignCurrencyUiStateTest` | 24 | `isForeignCurrencyMode`, `isFormValid` FX paths, `parseRate`, `formatRate` helpers |
| `AddEditTransactionViewModelTest` | +15 | `onTripSelected` FX seeding/clearing, all T3.6 save-path variants |
| `AddEditTransactionUiStateTest` | +3 | `hasUnsavedChanges` for tripId change, foreignAmount change, rate-only add-mode |

**Save-path test matrix (VM tests):**

| Scenario | `tripId` | `amountForeignMinor` |
|---|---|---|
| No trip, add path | null | null |
| Home-currency trip, add path | set | null |
| FX trip, add path | set | set |
| FX trip, edit path | set | set |
| FX trip, missing foreign amount | error, no save | — |
| Edit mode, trip not found | null | loaded from tx |

**5 other test files updated**: fakes extended with `tripId`/`amountForeignMinor` params to
implement the updated `TransactionRepository` interface (`HomeViewModelTest`,
`QuickAddViewModelTest`, `UndoFlowRunnerTest`, `SummaryViewModelTest`,
`AddEditTransactionViewModelTest`).

---

## Patterns Worth Repeating

1. **FX null guard mirrors existing guards** — the `isForeignCurrencyMode && amountForeignMinor == null` check is structurally identical to the existing amount and category guards. Consistency made it easy to review.

2. **Try/catch wrapping non-fatal lookups in edit mode** — `getTripById` failure collapses to `null` rather than crashing the form load. Good pattern for any "related entity" lookup where the entity may have been deleted.

3. **Fake extension over inheritance** — when `TransactionRepository` gains new params, all test fakes just add the params with a no-op body. No shared base class needed; each test file stays self-contained.

4. **`hasUnsavedChanges` covers all new fields** — every new state field that can differ between original and current needs explicit coverage. Tests pin this contract (the `rateOverrideText` add-mode case would have been easy to miss).

---

## Open Questions

- [ ] **Rate snapshot**: Should a future version persist the actual rate at transaction time rather than always reading from the trip? Deferred per ADR-002. Revisit before v4.x if users report incorrect FX history.
- [ ] **vi-VN locale strings** for `ForeignAmountField` / `RateOverrideField` — same deferred-locale pattern as previous PRs. Translate in bulk when locale file exists.

---

## References

- PR #168: https://github.com/doananhtuan22111996/expense-tracker/pull/168
- ADR-002: FX rate-override not persisted on transaction entity
- Previous learning: T2.2 Trips Screen — `docs/learnings/v3_13_0_t2_2_trips_screen.md`
- Previous learning: T2.3 Trip Row Totals — `docs/learnings/v3_13_0_t2_3_trip_row_totals.md`
