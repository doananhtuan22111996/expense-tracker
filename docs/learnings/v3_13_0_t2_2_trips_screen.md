# [ExpenseTracker] Decision — T2.2 TripsScreen + TripsViewModel (v3.13.0)

**Status**: Approved
**Created**: 2026-05-19
**Last Updated**: 2026-05-19

---

## TL;DR

PR [#156](https://github.com/doananhtuan22111996/expense-tracker/pull/156) shipped the v3.13.0 Trips list screen — the first reachable Trips UI in the app. `TripsViewModel` combines three time-relative `TripFilter` streams (Active / Upcoming / Past) into one `TripsUiState`, snapshotting `nowEpochDay` once at init via injected `Clock`. `TripsScreen` renders a sectioned `LazyColumn` with FAB + row-click navigation; not yet user-reachable until T2.8 wires the Settings entry row.

## Background

v3.13.0 (Travel Money Tracking / Trips) is mid-flight. Foundation work is complete (T1.1–T1.7 — `trips` table + Room v8 migration + `TripDao` + `TripRepository` + `TripQueriesDao`) and PR #155 added the four `ModalDestination` route definitions (T2.1). T2.2 is the first reachable Trips UI surface, opening the main feature path of Epic 2 (Trips screens). Downstream work (T2.3 totals overlay, T2.4 add/edit form, T2.6 detail screen, T2.8 Settings entry row) was blocked on this VM/screen pair landing.

## What Shipped

| File | Change |
|---|---|
| `ui/screen/trips/TripsViewModel.kt` | NEW — `@HiltViewModel` (88 LOC) combining 3 trip flows + `TripsUiState` |
| `ui/screen/trips/TripsScreen.kt` | NEW — Composable (256 LOC): `TopAppBar` + FAB + sectioned `LazyColumn` + empty/loading/error |
| `ui/ExpenseTrackerApp.kt` | +`composable(ModalDestination.Trips.route)` block |
| `res/values/strings.xml` | +9 en-US strings (titles, section labels, a11y, empty-state) |
| `test/.../TripsViewModelTest.kt` | NEW — 5 unit tests + local `FakeTripRepository` |
| `CHANGELOG.md` | `[Unreleased]` entry for T2.2 |

**Diff**: +576 / -0 across 6 files. Branch `feat/trips-screen` → `main`, merge commit `266acdcb`.

## How It Works

### State combination
```kotlin
combine(
    repo.observeTrips(TripFilter.Active(nowEpochDay)),
    repo.observeTrips(TripFilter.Upcoming(nowEpochDay)),
    repo.observeTrips(TripFilter.Past(nowEpochDay)),
) { active, upcoming, past -> Triple(active, upcoming, past) }
    .catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = ErrorUtils.getErrorMessage(e)) } }
    .collect { (active, upcoming, past) -> _uiState.update { it.copy(isLoading = false, active = active, upcoming = upcoming, past = past) } }
```

All three sections update in lockstep — no desync window where Active is fresh but Upcoming is stale.

### Sectioned list
`LazyListScope.tripSection(...)` extension fn renders a header `item(key = "header-$titleRes")` only when the bucket is non-empty (no empty headers), then `items(trips, key = { it.id })`.

### Row layout
Name + optional destination + localized date range (`DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)`) + optional foreign-currency code pill (`colorScheme.primary`).

## Decisions Made

- **Decision**: Snapshot `nowEpochDay` once at init, not per recomposition.
  **Reason**: Trip dates change rarely; a midnight rollover during a single screen visit is not worth re-bucketing for. Mirrors `HomeViewModel`'s month-snapshot pattern. Trade-off documented in VM KDoc.

- **Decision**: Inject `java.time.Clock` directly, not the project's `TimeProvider`.
  **Reason**: `TimeProvider` exposes `currentTimeMillis()` only — no epoch-day helper. `Clock` is already provided by `DispatcherModule` and trivially fakeable via `Clock.fixed(...)` in tests.

- **Decision**: Defer per-trip total + transaction count to a later PR (T2.3).
  **Reason**: User-confirmed scope decision. Aggregation primitives exist (`observeTripTotal`, `observeTripTransactionCount` from PR #154) but adding them here would balloon row UX surface; better to land the list shape first.

- **Decision**: Defer Settings → Trips entry row to T2.8.
  **Reason**: User-confirmed. Screen ships dark — reachable only by `composable(ModalDestination.Trips.route)` registration; no user-visible entry yet. Mirrors how T6.4 wired Settings → Widget Categories after the screen's own task landed.

- **Decision**: A11y row description includes trip name + date range, **not** destination or amount.
  **Reason**: Privacy-in-depth; destination ("Tokyo") and amount are higher-sensitivity. Name + date range is the minimum that makes the row screen-reader-usable.

- **Decision**: `.catch` after `combine` accepts that errors terminate the upstream subscription (no auto-retry).
  **Reason**: Matches `RecurringTransactionsViewModel`'s pattern. v3.13.0 spec doesn't require retry; recovery requires recreating the VM (back+forward). Diverging would be inconsistent.

- **Decision**: Keep `TripRow.modifier: Modifier = Modifier` parameter even though no call site passes one.
  **Reason**: Compose convention — composables expose `modifier`. Removing it would be the regression.

## Test Coverage

5 unit tests in `TripsViewModelTest`:
1. `init_emitsLoadingThenSectionedTrips` — happy path
2. `init_emptyRepo_resultsInIsEmptyTrue` — empty state
3. `init_repoThrows_setsErrorMessageAndStopsLoading` — error path via `failingFilter` switch on local `FakeTripRepository`
4. `init_passesSameNowEpochDayToAllThreeFilters` — single-time-snapshot invariant
5. `clearError_resetsErrorMessage` — error clearing

`Clock.fixed(LocalDate.of(2026, 5, 19).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)` makes `today` a deterministic constant. Local fake (private to test file) — promoted to shared `testutil/` only if a second consumer appears.

## Self-Review Outcome

`/review` ran clean — zero 🔴 Must Fix, zero 🟡 Should Fix that warranted action, three 🟢 Nice-to-Haves all explicitly accepted as deliberate trade-offs (locale-stale formatter is project-wide; `.catch` terminates upstream matches precedent; unused `TripRow.modifier` is required by Compose convention).

Fourth consecutive clean `/review` in the v3.13.0 streak.

## Action Items

- [ ] T2.3 — per-trip total + transaction count overlay on `TripRow` — Owner: Solo — Due: TBD
- [ ] T2.4 — Add/Edit Trip screen — unblocks the FAB target — Owner: Solo — Due: TBD
- [ ] T2.6 — Trip detail screen — unblocks row-click target — Owner: Solo — Due: TBD
- [ ] T2.8 — Settings → Trips entry row — first user-visible reach — Owner: Solo — Due: TBD

## Open Questions

None blocking. Both deferred surfaces (totals overlay, Settings entry) are explicit follow-up tasks, not unknowns.

## References

- PR: https://github.com/doananhtuan22111996/expense-tracker/pull/156
- Merge commit: `266acdcb`
- Branch: `feat/trips-screen` (deleted post-merge)
- Predecessors: PR #151 (TripEntity + Room v8), #152 (domain models + TripDao), #153 (TripRepository), #154 (TripQueriesDao), #155 (4 modal routes)
- Pattern reference: `RecurringTransactionsViewModel` + `RecurringTransactionsScreen`
- ADRs: ADR-001 (DeleteTripBehavior), ADR-002 (home-currency aggregation)
