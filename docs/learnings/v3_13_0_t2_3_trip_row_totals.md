**Title**: [ExpenseTracker] Decision — v3.13.0 T2.3: Per-trip totals + count overlay on TripRow

**Status**: Approved
**Created**: 2026-05-19
**Last Updated**: 2026-05-19

---

## TL;DR

Shipped T2.3 (PR #157, merge `813e5895`): every `TripRow` on the Trips list now ends with a `"count · total"` line, where the home-currency total is pre-formatted in the ViewModel and reactively reformats when the user changes default currency. Empty-trips lists no longer hang on `isLoading`, and the a11y row description deliberately omits the total amount (privacy-in-depth, mirrors the Home/Summary precedent). This is the first part of the Trips screen that turns the list from "navigation menu" into "useful overview".

## Background

PR #156 (T2.2) shipped the Trips screen scaffold with name + date range + optional foreign-currency pill — but every row was visually identical and gave the user zero signal about where their money went. T2.3 closes that gap by surfacing the same metrics the (upcoming) trip detail screen will show, using the per-trip `observeTripTotal` / `observeTripTransactionCount` flows from PR #154.

The technical heart of the change is a `flatMapLatest` enrichment pipeline that has to handle three distinct edge cases: (1) empty trip lists must not hang on `combine(emptyList())`, (2) currency changes must reformat every row in one pass, and (3) trips with no transactions yet must render distinctly from "0 spent".

## What shipped

| Layer | File | Change |
|---|---|---|
| ViewModel | `ui/screen/trips/TripsViewModel.kt` | Inject `CurrencyPreferenceRepository` + `CurrencyFormatter`. New `flatMapLatest` enrichment pipeline. New `TripCardUi` data class + private nested `Sections` / `TripAggregates`. |
| Screen | `ui/screen/trips/TripsScreen.kt` | `TripRow` rebuilt to consume `TripCardUi`. New `"$count · $total"` line. `tripSection(trips=…)` → `tripSection(cards=…)` rename. Updated 3-arg a11y row description. |
| Resources | `res/values/strings.xml` | New `<plurals name="trip_row_count">` (first plurals resource in the project, en-US only). New `trip_row_no_spending`. `a11y_trip_row` extended to 3 args. |
| Tests | `test/.../TripsViewModelTest.kt` | 5 existing updated to use extended `FakeTripRepository` (added `totalsByTrip` + `countsByTrip` maps) + new `FakeCurrencyPreferenceRepository` + `FakeCurrencyFormatter`. 2 new tests. |
| Changelog | `CHANGELOG.md` | `[Unreleased]` entry. |

Diff stat: 5 files / +225 / −40 LOC.

## Architectural pattern

The enrichment pipeline composes three layered Flow operators:

1. **Outer combine** over the three `TripFilter` streams produces a `Sections(active, upcoming, past)` triple. (Inherited from T2.2.)
2. **Outer `flatMapLatest`** over the sections selects between two enrichment branches:
   - `allTrips.isEmpty()` → `flowOf(sections to emptyMap())` — short-circuits the inner combine which would otherwise never emit.
   - Otherwise → `enrichmentFlow(allTrips).map { aggMap -> sections to aggMap }`.
3. **Inner enrichment flow** in `enrichmentFlow(trips)`:
   - `currencyPreferenceRepository.observeDefaultCurrency().distinctUntilChanged()` is the OUTER flow.
   - Inside its `flatMapLatest`, `combine(totalsFlow, countsFlow)` produces the per-trip aggregate map with the current currency code baked into `totalLabel`.
   - When the user changes default currency, `flatMapLatest` cancels the inner `combine` and rebuilds it under the new code — every visible row reformats atomically without re-querying totals.

The currency-on-the-outside ordering (currency → combine, NOT combine → currency-map) is deliberate: it keeps the formatting decision and the data fetch in the same emission, so there's no transient frame where totals are formatted in the OLD currency code.

## Decisions Made

- **Decision**: Empty trips list returns `flowOf(sections to emptyMap())` instead of letting the inner combine attempt to emit.
  **Reason**: `combine(emptyList<Flow<X>>())` never emits. Without the short-circuit, the screen would hang on `isLoading=true` whenever the user has zero trips — the most common state for a fresh install.

- **Decision**: `totalLabel: String?` (nullable), with `null` semantically meaning "this trip has no transactions yet".
  **Reason**: Distinct from `"0 spent"`. The repository's `observeTripTotal` returns `Flow<Long?>` for the same reason (PR #154), so the null propagates end-to-end. UI falls back to a localized `trip_row_no_spending` ("No spending yet").

- **Decision**: Use Android `<plurals>` for the count label — first plurals resource in the project.
  **Reason**: `"%1$d transactions"` would render `"1 transactions"`, which reads as a UI bug. en-US ships now; vi-VN deferred per the v3.12.0 locale-bulk-translate convention. Future locales drop in cleanly via the same resource.

- **Decision**: a11y row description excludes the total amount.
  **Reason**: Privacy-in-depth — mirrors the Home/Summary row precedent. Screen-reader users still get name + date range + transaction count, which is enough context to navigate; the amount stays visual-only.

- **Decision**: `CurrencyPreferenceRepository.observeDefaultCurrency().distinctUntilChanged()` wraps the inner combine.
  **Reason**: Currency rarely changes; wrapping it as the OUTER `flatMapLatest` source means the (more frequent) totals/counts emissions don't re-trigger formatter work. `.distinctUntilChanged()` defends against any chatty preferences emitter.

- **Decision**: `Trip.toUi(aggMap)` falls back to `transactionCount = 0` / `totalLabel = null` when `aggMap[id]` is missing — instead of `getValue(id)` crashing.
  **Reason**: The fallback is unreachable in production (the map is always built from the same `ids` we're mapping over), but defensive against any future caller that breaks the invariant. The `/review` flagged this as a "pick your poison" 🟢 nice-to-have; the safer side won.

- **Decision**: `Sections` and `TripAggregates` are private nested data classes inside `TripsViewModel`, NOT top-level.
  **Reason**: Internal pipeline implementation detail. Tests assert end-state via `vm.uiState.value`, not via these intermediate types — leaking them would just be API surface noise.

## Test coverage

| Test | What it pins |
|---|---|
| `init_emitsLoadingThenSectionedTripsWithAggregates` | Active/Upcoming/Past sections all enriched correctly with totals + counts |
| `init_emptyRepo_resultsInIsEmptyTrueWithoutHangingOnInnerCombine` | The empty-trips short-circuit invariant |
| `init_repoThrows_setsErrorMessageAndStopsLoading` | Error path: `catch` after `flatMapLatest` covers both outer + inner errors |
| `init_passesSameNowEpochDayToAllThreeFilters` | Single-`nowEpochDay` snapshot invariant |
| `clearError_resetsErrorMessage` | Error reset |
| **`currencyChange_reformatsAllTotals`** (new) | Flips `currencyFlow.value` mid-stream, asserts every visible row's `totalLabel` reformats |
| **`nullTotal_yieldsNullLabelAndZeroCountFallback`** (new) | Unseeded totals/counts → `totalLabel = null`, `transactionCount = 0` |

7/7 pass via `runTest(mainDispatcherRule.testDispatcher)` + `advanceUntilIdle()`. Full unit suite green.

## Self-review outcome

**Fourth consecutive clean `/review`** in the v3.13.0 sequence (PRs #154/#155/#156/#157). Zero actionable findings. The single 🟢 Nice-to-Have was the `getValue` vs defensive-fallback stylistic note in `Trip.toUi`, where the current code is already the safer choice.

The streak is starting to look like signal, not luck — the v3.13.0 epic is hitting on a well-established codebase template (Hilt VM + Flow combine + `MainDispatcherRule` tests + `<plurals>` strings + a11y privacy-in-depth) where the first PR pays the design cost and every subsequent PR slots into the pattern.

## Action Items

- [ ] Pick next v3.13.0 task — T2.4 (Add/Edit Trip), T2.6 (Trip Detail), or T2.8 (Settings → Trips entry, first user-visible reach) — Owner: Solo — Due: 2026-05-20
- [ ] If trip list ever grows large (~100+ trips), revisit the `flatMapLatest` resubscribe-on-list-mutation cost — Owner: Solo — Due: deferred (no current scaling pressure)

## Open Questions

- [ ] Should T2.8 (Settings → Trips entry row) land before T2.4/T2.6, so the screen becomes user-reachable for manual smoke-testing without test-only seeding?
  Status: Open — leaning yes, since T2.8 is small (Settings row + nav wiring) and unblocks visual verification of T2.2/T2.3 on the v3.12.0 release APK.

## References

- PR #157 — https://github.com/doananhtuan22111996/expense-tracker/pull/157 (merge `813e5895`, closed 2026-05-19T16:49:54Z)
- PR #156 (T2.2) — Trips screen scaffold (merge `266acdcb`)
- PR #154 (T1.4) — `TripQueriesDao` + `observeTripTotal`/`observeTripTransactionCount` repository passthroughs
- v3.13.0 Trips epic — Notion v3.13.0 container page
- ADR-002 — Home-currency aggregation invariant (`transactions.amount` carries home equivalent)
- Related learning: [v3_13_0_t2_2_trips_screen.md](v3_13_0_t2_2_trips_screen.md)
