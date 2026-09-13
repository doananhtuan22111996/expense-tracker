# [ExpenseTracker] Learning — Conversion Wizard Entry Point + State Machine (T4.1 + T4.2)

**Status**: Approved
**Created**: 2026-06-14
**Last Updated**: 2026-06-14
**PR**: [#171](https://github.com/doananhtuan22111996/expense-tracker/pull/171)
**Merge commit**: `f6c5aff`
**Branch**: `feat/trips-conversion-wizard-t4.1-t4.2`

---

## TL;DR

PR #171 delivers the entry point for the Category → Trip conversion flow (T4.1) and the 3-step wizard state machine (T4.2). EXPENSE categories with ≥1 transaction get a ⋮ overflow menu; tapping "Convert to Trip" opens `ConversionWizardScreen`. The VM drives a `Metadata → RowDecisions → Preview` state machine with zero DB writes until commit — wizard is fully abortable at any step. Step bodies are stubs; T4.3/T4.4/T4.5 replace them.

---

## Background

v3.13.0 (Trips) Epic 4 covers the conversion wizard that lets users migrate an existing EXPENSE category's transactions into a Trip entity. T4.1 adds the discoverable entry point on the Categories screen without cluttering it. T4.2 delivers the core state machine that all subsequent step-body PRs (T4.3/T4.4/T4.5) and the atomic commit (T4.6) depend on. Delivering both together means those follow-up PRs each touch only their own step body — no re-wiring of navigation or state structure.

---

## What Was Delivered

### T4.1 — "Convert to Trip" Entry Point

| Area | Change |
|---|---|
| `CategoriesViewModel` | Gains `convertToTripCategoryId: Long?` one-shot nav flag + `onConvertToTripRequested(id)` + `onConvertToTripConsumed()` — mirrors `pendingDeleteId` pattern |
| `CategoriesScreen` | EXPENSE rows with `transactionCount > 0` get a `MoreVert` overflow menu; single item "Convert to Trip" with `FlightTakeoff` icon |
| Filtering | INCOME categories → no menu; EXPENSE with 0 transactions → no menu |
| Navigation | Tap routes to `modal/convert_category_to_trip/{categoryId}` (route already declared in `ExpenseTrackerDestination.kt`) |

### T4.2 — `ConversionWizardViewModel` + 3-Step State Machine

**Steps**: `Metadata` → `RowDecisions` → `Preview`

| Concern | Detail |
|---|---|
| `onNext()` | Validates Step 1 (blank name, end-before-start date, zero FX rate) before advancing |
| `onBack()` | Steps 2/3 → previous step; Step 1 → `done = true` (pops wizard) |
| `sourceDisposition` | Auto-derives `DELETE` when all rows are `Migrate`; `KEEP` when any row is `Skip` |
| `buildDraft()` | Assembles full `ConversionDraft` payload for T4.6's atomic commit |
| Row seeding | All transactions default to `Migrate` with the first non-source EXPENSE category as `newCategoryId`; falls back to source if no other exists |
| DB writes | **Zero until `commit()`** — all state is in-memory; wizard abortable at any step |
| `ConversionWizardScreen` | Scaffold + `BackHandler` + step-body switching; bodies are stubs until T4.3/T4.4/T4.5 |

**Route wiring**: `ExpenseTrackerApp` gains composable with `navArgument("categoryId", LongType)`. The `ModalDestination.ConvertCategoryToTrip` route and `ModalNavRoutes.convertCategoryToTripRoute(categoryId)` were already declared; this PR only wires the composable.

---

## Test Coverage

**18 tests in `ConversionWizardViewModelTest`:**

| Group | Cases |
|---|---|
| Init / loading | Loads correctly; `tripName` pre-seeded; dates pre-seeded; `done=true` on `id=0`, unknown id, INCOME category; all rows seeded as `Migrate` |
| Step navigation | Metadata → RowDecisions on valid input; blank name stays + `nameError`; end<start stays + `dateError`; zero FX rate stays + `rateError`; RowDecisions → Preview; Preview → done (commit stub) |
| Back navigation | Back from Metadata → `done`; Back from RowDecisions → Metadata; Back from Preview → RowDecisions |
| `sourceDisposition` | All migrate → `DELETE`; one skipped → `KEEP`; skip then re-migrate → `DELETE` |
| `buildDraft` | Returns correct draft; not null after normal load |

---

## Decisions Made

- **Decision**: Filter entry point to EXPENSE categories with ≥1 transaction only.
  **Reason**: INCOME categories can never be converted (trips are EXPENSE-domain). Zero-transaction categories have nothing to migrate — showing the menu would be misleading.

- **Decision**: One-shot nav flag (`convertToTripCategoryId: Long?`) in `CategoriesViewModel`, not a direct navController call from the row.
  **Reason**: Mirrors `pendingDeleteId` — consistent pattern, testable in VM, no NavController dependency in VM.

- **Decision**: Zero DB writes until `commit()`.
  **Reason**: The wizard can be multi-step and long-lived; allowing early writes would create partial state that's hard to roll back. All state lives in-memory; aborting at any step costs nothing.

- **Decision**: `sourceDisposition` auto-derives from row decisions rather than being a user input.
  **Reason**: Asking the user whether to keep or delete the source category is redundant — the answer is fully determined by whether any row is `Skip`. Auto-derivation removes a decision point without reducing expressiveness.

- **Decision**: Step bodies shipped as stubs; T4.3/T4.4/T4.5 replace them.
  **Reason**: Stub-with-parameter pattern (established across v3.12.0 PRs). Delivering the scaffold + VM together means follow-up PRs each have a single, focused concern — no re-wiring of navigation or state.

- **Decision**: Route composable wired in this PR, not route declaration (already existed).
  **Reason**: `ModalDestination.ConvertCategoryToTrip` and `ModalNavRoutes.convertCategoryToTripRoute` were declared ahead of time. Wiring the composable is the only missing piece; keeping them separate avoids a large "route + VM + screen + tests" PR that would be harder to review.

---

## Action Items

- [ ] T4.3 — Implement Metadata step body UI — Owner: Solo
- [ ] T4.4 — Implement RowDecisions step body UI — Owner: Solo
- [ ] T4.5 — Implement Preview step body UI — Owner: Solo
- [ ] T4.6 — Atomic commit (write `ConversionDraft` to DB, delete source if `sourceDisposition=DELETE`) — Owner: Solo
- [ ] Mark T4.1 + T4.2 → Done on the v3.13.0 Notion task board — Owner: Solo

---

## References

- [GitHub PR #171](https://github.com/doananhtuan22111996/expense-tracker/pull/171)
- v3.13.0 Trips sprint docs in Notion
- T3.6 save path learning: `docs/learnings/v3_13_0_t3_6_save_path_resubmit.md`
- T2.2/T2.3 screen + row totals: `docs/learnings/v3_13_0_t2_2_trips_screen.md`, `docs/learnings/v3_13_0_t2_3_trip_row_totals.md`
