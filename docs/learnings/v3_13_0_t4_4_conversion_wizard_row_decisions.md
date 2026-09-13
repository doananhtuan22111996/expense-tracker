# [ExpenseTracker] Learning — Conversion Wizard Step 2 Row Decisions (T4.4)

**Status**: Approved
**Created**: 2026-06-14
**Last Updated**: 2026-06-14
**PR**: [#173](https://github.com/doananhtuan22111996/expense-tracker/pull/173)
**Merge commit**: `a9f63df` (rebased onto main post-#172 merge)
**Branch**: `feat/trips-conversion-wizard-t4.4-row-decisions`

---

## TL;DR

PR #173 replaces the stub `WizardStep2RowDecisionsBody` with a `LazyColumn` of per-transaction `DecisionRow` composables. Each row has a Migrate/Skip `FilterChip` pair and an inline `RowCategoryPicker` when Migrate is selected. Self-review caught a `DropdownMenu` anchor issue (popup positioning) before merge.

---

## Background

T4.2 (PR #171) seeded all row decisions as `Migrate` in-memory. T4.4 gives the user a real UI to inspect and change each decision before T4.6's commit. All VM handlers (`onDecisionChanged`) and state (`rowDecisions`, `availableCategories`, `transactions`) were already in place — this was a pure UI task.

---

## What Was Delivered

### `WizardStep2RowDecisionsBody`

`Column(.fillMaxSize())` containing:
1. Subtitle text (transaction count)
2. `LazyColumn(.weight(1f))` — scrollable list of `DecisionRow` composables, stable `key = { it.id }`

### `DecisionRow`

One per transaction. Receives pre-computed `isMigrate: Boolean` and `currentCategoryId: Long?` rather than the raw `RowDecision` sealed type — keeps the leaf composable clean.

| Area | Implementation |
|---|---|
| Amount + currency | `AmountText` with `homeCurrencyCode` |
| Date | `remember(tx.timestamp) { LocalDate.ofInstant(...).format(dateFormatter) }` |
| Note | Optional single-line text (shown only if non-blank) |
| Migrate/Skip | `FilterChip` pair with guard conditions |
| Category picker | `RowCategoryPicker` — only shown when `isMigrate && availableCategories.isNotEmpty()` |

**Chip guard conditions** — prevent no-op state writes:
- Migrate chip `onClick`: `if (!isMigrate) onMigrate(...)`
- Skip chip `onClick`: `if (isMigrate) onSkip()`

### `RowCategoryPicker`

Inline card showing current category (color swatch + name) that opens a `DropdownMenu` on tap.

- Color swatch: `ChartColors.categoryColor(selectedCategory?.colorKey, colorScheme)`
- Accessibility: `a11y_selected_tap_to_change` / `a11y_select_category_generic`
- Haptic feedback on tap (matches `CurrencyDropdown` pattern)
- `DropdownMenu` placed inside a `Box` wrapper alongside the trigger `Card` — see decisions below

---

## Decisions Made

- **Decision**: `DecisionRow` receives `isMigrate: Boolean` + `currentCategoryId: Long?` instead of `ConversionDraft.RowDecision`.
  **Reason**: Leaf composables shouldn't inspect sealed types. Pre-computing the booleans at the `WizardStep2RowDecisionsBody` level keeps `DecisionRow` as a pure display component. Easier to test visually and reason about.

- **Decision**: `DateTimeFormatter` created per `DecisionRow` via `remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }`, not a shared top-level `private val`.
  **Reason**: A shared `private val` is created once at class-load time and doesn't respond to locale changes at runtime. Per-`remember` instances are created at composition time — if locale changes, new compositions get a new formatter. Same reason `TripFormComponents.kt` uses `private val` for its formatter (a deliberate tradeoff there: that formatter is used for date *display* after user picks a date, not during composition of a list).

- **Decision**: `DropdownMenu` wrapped in `Box(fillMaxWidth)` alongside the trigger `Card`.
  **Reason**: Caught in self-review. Without an anchor container, Compose positions the popup relative to the nearest `Popup` ancestor, which in a `LazyColumn` row can be the top of the row instead of below the trigger card. Wrapping in `Box` at the picker level gives the popup a local anchor — same pattern used by `CurrencyDropdown` and `CategoryDropdown` throughout the app.

- **Decision**: `RowCategoryPicker` hidden (`if (isMigrate && availableCategories.isNotEmpty())`) rather than shown-but-disabled when there are no alternatives.
  **Reason**: If the source is the only EXPENSE category, showing a picker with no choices is misleading. The VM already handles this edge case in `buildDraft()` by falling back to the source category id — the UI simply omits the control.

- **Decision**: `expanded` for `DropdownMenu` uses `remember { mutableStateOf(false) }`, not `rememberSaveable`.
  **Reason**: Dropdowns should close on configuration change (rotation). All other dropdowns in the codebase (`CurrencyDropdown`, `CategoryDropdown`) follow this convention — open state is intentionally transient.

---

## Self-Review Finding (caught before merge)

**`DropdownMenu` without anchor container** — `Card` and `DropdownMenu` were bare siblings at the composable root. `DropdownMenu` in Compose positions its popup relative to its nearest `Popup` ancestor in the layout tree; inside a `LazyColumn`, this can resolve to the top of the visible viewport instead of below the trigger. Fix: wrap both in `Box(modifier.fillMaxWidth())`.

**Lesson**: Every `DropdownMenu` must be wrapped with its trigger in a `Box` or `Column` to act as the popup anchor. This is the pattern used by `CurrencyDropdown`, `CategoryDropdown`, and `TripDateRangeRow` throughout the codebase — always check the anchor when introducing a new `DropdownMenu`.

---

## Action Items

- [ ] Mark T4.4 → Done on v3.13.0 Notion task board — Owner: Solo
- [ ] After #173 merges, rebase PR #174 (T4.5) onto `main` — Owner: Solo

---

## References

- [GitHub PR #173](https://github.com/doananhtuan22111996/expense-tracker/pull/173)
- [T4.3 Learning](v3_13_0_t4_3_conversion_wizard_metadata_step.md)
- T4.5 Preview: PR #174 (open, chained on T4.4)
- `CurrencyDropdown.kt` — canonical `DropdownMenu` anchor pattern
