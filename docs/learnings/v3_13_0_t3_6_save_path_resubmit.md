# [ExpenseTracker] Learning — Trips: T3.6 Save Path Re-submission (PR #169)

**Status**: Approved
**Created**: 2026-06-12
**Last Updated**: 2026-06-12
**PR**: https://github.com/doananhtuan22111996/expense-tracker/pull/169 (merged `7287dd5b`)
**Supersedes**: PR #168 (conflicted, not merged)
**Branch**: `feat/trips-save-path-t3.6`

---

## TL;DR

PR #169 is the actual landed PR for Trips T3.6 (save path). It replaces PR #168, which became
conflicted when PR #167 (T3.4 + T3.5) merged into main in the interim. The code change is
identical — this doc captures the re-PR workflow pattern rather than the code details.

For the full code documentation of T3.6, see:
`docs/learnings/v3_13_0_t3_4_t3_5_t3_6_fx_input_save_path.md`

---

## Background

PR #168 was originally created to cover T3.4 + T3.5 + T3.6 together. Part-way through review,
T3.4 + T3.5 were split off and merged as PR #167. This left PR #168's base stale — it now
contained duplicated T3.4/T3.5 code relative to main, causing a merge conflict that would have
required a non-trivial rebase.

Rather than rebasing PR #168, a clean targeted PR (#169) was opened from the already-correct
branch (`feat/trips-save-path-t3.6`) containing only the T3.6 delta on top of the post-#167 base.

---

## What Changed (T3.6 — same as PR #168)

10 files modified, 10 new tests. Full change details in the PR #168 learning doc linked above.

**Summary of files:**

| Layer | Files |
|-------|-------|
| Domain model | `Transaction.kt` (+tripId, +amountForeignMinor) |
| Repository interface | `TransactionRepository.kt` (+2 optional params) |
| Repository impl + mapper | `TransactionRepositoryImpl.kt`, `TransactionMapper.kt` |
| ViewModel | `AddEditTransactionViewModel.kt` (save path, edit load, hasUnsavedChanges) |
| HomeViewModel | `HomeViewModel.kt` (undoDelete preserves FX fields) |
| Tests | `AddEditTransactionViewModelTest.kt` (+7), `AddEditTransactionUiStateTest.kt` (+3), plus 4 fake-extension updates |

---

## Decisions Made

**Decision**: Open a clean re-PR (#169) rather than resolving the conflict in PR #168.
**Reason**: The conflicted PR (#168) carried T3.4+T3.5 code that was already in main via PR #167.
Rebasing would have required manually stripping those sections, with high risk of missing a line
or introducing a regression. A clean PR from the correct base was faster, safer, and produced a
cleaner diff to review.

**Decision**: Close PR #168 without merging.
**Reason**: PR #169 is a strict superset of the T3.6 work in PR #168 and merges cleanly. Keeping
#168 open would create confusion about which PR is the canonical record.

---

## Workflow Pattern: Re-PR on Conflict

When a dependency merges and leaves your PR conflicted:

1. **Assess overlap** — how much of the conflicted PR's content is already in main?
   - If minimal overlap (e.g., a single file touched): rebase.
   - If substantial overlap (e.g., the dependency was a large portion of the PR's diff): re-PR.

2. **Re-PR steps**:
   - Verify the working branch already has the correct state on top of the new main.
   - Open a new PR with the correct base. Title it clearly as a replacement.
   - Note in the new PR body: "Replaces #NNN (conflicted — base included X which merged as #MMM)."
   - Close the old PR with a comment pointing to the new one.

3. **Keep learning docs pointed at the actual merged PR** — PR #169 is the canonical T3.6 merge
   commit (`7287dd5b`). PR #168 is historical context only.

---

## Action Items

- [x] PR #169 merged — T3.6 complete
- [x] PR #168 superseded (conflicted, closed by re-PR)
- [ ] Flip Notion T3.6 task Status → Done, link PR #169

---

## References

- PR #169 (merged): https://github.com/doananhtuan22111996/expense-tracker/pull/169
- PR #168 (conflicted, superseded): https://github.com/doananhtuan22111996/expense-tracker/pull/168
- PR #167 (T3.4 + T3.5, the dependency that caused the conflict): https://github.com/doananhtuan22111996/expense-tracker/pull/167
- Full T3.6 code learning: `docs/learnings/v3_13_0_t3_4_t3_5_t3_6_fx_input_save_path.md`
- ADR-002: Rate override not persisted on transaction entity
