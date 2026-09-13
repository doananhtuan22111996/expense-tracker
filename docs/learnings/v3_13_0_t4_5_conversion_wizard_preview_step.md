# [ExpenseTracker] Learning — Conversion Wizard Step 3 Preview Screen (T4.5)

**Status**: Approved
**Created**: 2026-06-14
**Last Updated**: 2026-06-14
**PR**: [#174](https://github.com/doananhtuan22111996/expense-tracker/pull/174)
**Merge commit**: `81933aa` (rebased onto main post-#173 merge)
**Branch**: `feat/trips-conversion-wizard-t4.5-preview-step`

---

## TL;DR

PR #174 replaces the stub `WizardStep3PreviewBody` with a structured read-only review screen showing the user exactly what will happen when they tap Commit. Two `PreviewSection` summary cards (Trip + Transactions) plus a semantically colored source-fate card (red for DELETE, blue for KEEP). Zero VM changes — pure display from existing `ConversionWizardUiState`. Clean self-review: no issues found.

---

## Background

T4.3/T4.4 delivered the first two wizard steps. T4.5 closes the wizard UI work before T4.6's atomic commit. The user needs a confirmation summary so they can verify the trip name, date range, and which transactions will migrate vs stay, before the irreversible commit fires. This is especially important for the source-category fate (delete vs keep), which is visually highlighted.

---

## What Was Delivered

### `WizardStep3PreviewBody`

`Column(.fillMaxSize().verticalScroll(rememberScrollState()))` containing three sections:

### Trip summary — `PreviewSection("Trip")`

| Condition | Row shown |
|---|---|
| Always | Name |
| `tripDestination.isNotBlank()` | Destination |
| `startEpochDay != null && endEpochDay != null` | Dates (formatted via `tripFormatDateRange`) |
| `isForeignCurrency && foreignCurrencyCode.isNotBlank()` | Currency — format: `"CODE @ rate HOME_CODE"` |

### Transaction decisions — `PreviewSection("Transactions")`

| Condition | Row shown |
|---|---|
| `migratedCount > 0` | "X migrating to → [trip name]" |
| `skippedCount > 0` | "Y staying in → [source category name]" |

### Source fate card

Full-width `Card` with semantic container color:
- `errorContainer` (red) → `"CategoryName" will be deleted after commit.`
- `secondaryContainer` (blue) → `"CategoryName" will be kept (some transactions were skipped).`

Category name is embedded in the message (T4.2's generic stub strings kept for backward compat).

### Reusable private composables

| Composable | Purpose |
|---|---|
| `PreviewSection(title, content)` | Section header (primary color) + elevated Card wrapper |
| `PreviewRow(label, value)` | Two-column label/value row with `weight(0.4f)` / `weight(0.6f)` |

---

## Decisions Made

- **Decision**: `verticalScroll` but NOT `rememberSaveable` for scroll state.
  **Reason**: Preview screen is read-only summary — the user can trivially re-scroll to any position. Preserving scroll position across rotation adds complexity for near-zero UX value. Same tradeoff as the T4.4 `expanded` dropdown state.

- **Decision**: `Arrangement.SpaceBetween` with weighted text in `PreviewRow`.
  **Reason**: For a 2-child row, `SpaceBetween` pushes label to the left edge and value toward the right with remaining weight(0.6f). This creates a clean key-value pair layout where the label column is always 40% wide and value 60% — values wrap within their column rather than pushing the label. This is the same pattern used in the trip detail screen's KPI tile layout.

- **Decision**: `errorContainer` / `secondaryContainer` for source fate card colors.
  **Reason**: Matches M3 semantic color conventions already used in the app — budget-over-budget indicator uses `errorContainer`, info/neutral states use `secondaryContainer`. Avoids introducing new color tokens for a one-off card.

- **Decision**: FX rate string format is `"CODE @ rate HOME_CODE"` (e.g., `"JPY @ 165 VND"`).
  **Reason**: Compact and self-explanatory in a read-only context. The full "1 JPY = 165 VND" helper text is already shown on the rate field in Step 1 — the preview just needs to confirm the rate was recorded.

- **Decision**: Empty `PreviewSection("Transactions")` card when both counts are 0 (theoretical only — VM always seeds ≥1 row).
  **Reason**: Defensive — rather than adding an `if (migratedCount > 0 || skippedCount > 0)` guard on the section, the composable safely renders an empty card. The VM invariant (every loaded category has ≥1 transaction) makes this unreachable, but no crash results.

---

## Self-Review Result

Clean pass — zero issues found. This is the third consecutive clean self-review (T4.3 and T4.4 each caught one real bug). Pattern: read-only composables with no new VM logic or interaction surfaces have near-zero bug surface.

---

## Action Items

- [ ] Mark T4.5 → Done on v3.13.0 Notion task board — Owner: Solo
- [ ] Proceed to T4.6 — atomic commit (`TripRepository.commitConversion`) — Owner: Solo

---

## References

- [GitHub PR #174](https://github.com/doananhtuan22111996/expense-tracker/pull/174)
- [T4.4 Learning](v3_13_0_t4_4_conversion_wizard_row_decisions.md)
- [T4.3 Learning](v3_13_0_t4_3_conversion_wizard_metadata_step.md)
- T4.6 atomic commit: next task
