# Manual QA Matrix & Pre-PR Self-Review — v3.13.0 (Travel Money Tracking / Trips)

**Version:** 3.13.0
**Scope:** Epics 1–7 (37 Tasks)
**Date:** 2026-09-14

---

## 1. Core Feature Matrix

| # | Flow / Feature Area | Test Scenario | Expected Result | Verified |
|---|---|---|---|:---:|
| 1 | **Trip Creation (Home Currency)** | Create trip with name "Da Nang", destination "Vietnam", dates, no foreign currency | Trip appears in Active/Upcoming/Past list according to dates; totals show "No spending yet" | [x] |
| 2 | **Trip Creation (Foreign Currency)** | Create trip "Tokyo" with JPY foreign currency and exchange rate `0.0067` | Rate is saved; trip card shows foreign currency pill | [x] |
| 3 | **Inline Validation** | Blank name, end date < start date, foreign currency enabled without rate | Form validation fails; save button remains disabled or surfaces inline error | [x] |
| 4 | **Add Transaction Auto-Assign** | Open Add Transaction when single active trip overlaps today | Trip is auto-selected; TripChip displays active trip name | [x] |
| 5 | **Multiple Active Trips Tie-Break** | Two active trips overlap today | Most recently created trip is auto-assigned per FR-19 | [x] |
| 6 | **Foreign Currency Input Mode** | Add transaction with foreign currency trip selected | Primary input in foreign currency, home-currency equivalent line updates dynamically | [x] |
| 7 | **Rate Override** | Override exchange rate on single transaction save | Override applies only to this transaction; Trip entity rate is unchanged | [x] |
| 8 | **Trip Detail Screen** | View trip with mixed transactions | Header KPI tiles (Total, Daily Avg, Count), Donut chart by category, daily bar chart render cleanly | [x] |
| 9 | **Trip Detail Action Menu** | Edit, Delete (Untag vs Revert) | Non-conversion trip defaults to Untag; conversion trip defaults to Revert | [x] |
| 10 | **Legacy Category Conversion** | Long-press category with ≥ 1 transaction -> Convert to Trip | 3-step wizard guides through Metadata -> Row decisions -> Preview | [x] |
| 11 | **Wizard Zero-Write on Abort** | Cancel/back out of wizard at Step 1, 2, or 3 | Zero changes persisted to database; category and transactions untouched | [x] |
| 12 | **Wizard Row Decisions** | Apply default to all, per-row override, per-row Skip | Skip forces source category disposition to KEEP; all-migrated allows DELETE | [x] |
| 13 | **Revert Converted Trip** | Trip detail -> Action menu -> Revert to category | Restores original category (re-creating if deleted), restores transaction categories, deletes trip | [x] |
| 14 | **Exclude Trips Toggle** | Tap flight toggle on Home or Summary | Immediately filters trip transactions from monthly totals, lists, donut charts, and budgets | [x] |
| 15 | **Budget Alerts** | Set budget and toggle Exclude Trips | Background worker evaluates expense totals respecting exclusion preference | [x] |
| 16 | **Backup & Restore Round-Trip** | Export backup with trips -> wipe database -> restore | All trips, FX fields, and converted category snapshots restored byte-identical | [x] |
| 17 | **Pre-v3.13.0 Backup Restore** | Restore pinned v3.11 backup fixture | Restores cleanly without crash; trip fields default to null | [x] |

---

## 2. Accessibility & System Integration

| Feature / Scenario | Requirement | Verification Method | Status |
|---|---|---|:---:|
| **Dark Theme** | Material 3 surface tones, high-contrast text, proper card container colors | Visual audit on dynamic color themes | Pass |
| **TalkBack (Screen Reader)** | TripChip announces `"Trip: <name>, double-tap to change"`; toggle announces state | Accessible role (`Role.Switch`, content descriptions) | Pass |
| **Dynamic Type** | Text scales cleanly at 150% and 200% font scaling without clipping | Scaffold and LazyColumn layouts tested | Pass |
| **RTL Layouts** | AutoMirrored icons, directional padding | Tested with Arabic / RTL preview layout | Pass |
| **Offline Operation** | 100% functional with Airplane Mode enabled; zero network dependencies | End-to-end verified | Pass |

---

## 3. Analytics & Telemetry Contract (FR-A7)

| Event Name | Parameters | Contract Invariant | Status |
|---|---|---|:---:|
| `trip_created` | `foreign_currency` (boolean string) | Never includes trip name, destination, or dates | Pass |
| `trip_converted_from_category` | `transaction_count` (bucket enum), `foreign_currency` (boolean string) | Broad bucket (`one_to_nine`, `ten_to_forty_nine`, `fifty_plus`); no category names | Pass |
| `transaction_added` | `type`, `source`, `trip_attached` (boolean string) | At most 3 params; strictly enum / boolean wire values | Pass |

---

## 4. Pre-PR Self-Review Checklist

- [x] All 7 epics (T1.1 through T7.5) implemented and verified.
- [x] Room schema v7 -> v8 migration passes automated integration tests.
- [x] Database atomicity: forced failures during conversion commit and revert rollback cleanly.
- [x] Backup format v2 backward & forward compatibility verified.
- [x] `privacy-policy.md` updated in lockstep with analytics parameter expansions.
- [x] Fastlane Play Store release notes (`3130000.txt`) <= 500 characters.
- [x] `./gradlew spotlessCheck testDebugUnitTest testReleaseUnitTest` clean.
