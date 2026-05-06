# Changelog

## [Unreleased]

### Added
- `AnalyticsPreferences.analyticsEventsConsent` + `analyticsEventsPromptShown` flags (both default `false`) — second consent channel for Firebase Analytics event collection, fully independent of the existing Crashlytics `analyticsConsent` per ADR-011's two-toggle decision. Historical-naming note added in interface KDoc: the original `analyticsConsent` key predates v3.11.0's Analytics work and remains Crashlytics-only despite the generic-sounding name; renaming would reset beta-tester consent. Four keys now live in the existing `analytics_preferences` DataStore (no new store, no migration). `FakeAnalyticsPreferences` in `testutil/` extended to match.
- `AnalyticsPreferencesImplTest` extended with 9 additional fake-based contract tests for the new Analytics-events keys (18 total): default-false for both new flows, round-trip on the consent setter, and — critically — pairwise independence in every direction (flipping any one of the four keys must not flip any of the other three). The independence tests pin ADR-011's privacy-critical contract that opting into Crashlytics must not imply opting into Analytics, and vice versa — important for beta testers upgrading from pre-bundle v3.11.0 (Crashlytics-only) to the bundled release.
- `ConsentPromptDialogV2` composable in `ui/component/` — dual-checkbox post-onboarding dialog per ADR-011 + Design doc. Two rows (Crashlytics + Analytics) each defaulting unchecked (privacy-safe), shared "We never send…" footer, full-width "Done" button that commits current checkbox state. Back-gesture and outside-tap both route through `onResolved(shareCrashes, shareAnalytics)` — ADR-008's "ambiguous dismissal = privacy-safe default" holds because unchecked IS the privacy-safe default. Checkbox state is `rememberSaveable` so rotation preserves in-progress choices; process death resets to unchecked. Row touch target is a full 56dp band with `Role.Checkbox` semantics for TalkBack.
- `AnalyticsOnlyConsentDialog` composable — upgrade-path variant for beta testers who already resolved the Crashlytics prompt (`consentPromptShown = true`) and have not yet seen the new Analytics question (`analyticsEventsPromptShown = false`). Single checkbox, Analytics-specific title + intro that explicitly states the Crashlytics choice is unchanged. Shares `ConsentCheckboxRow` with the main dialog.
- 10 new consent strings (`consent_prompt_v2_*`, `consent_prompt_crash_*`, `consent_prompt_analytics_*`, `consent_prompt_footer`, `consent_prompt_done`, `consent_prompt_analytics_only_*`) per Design doc's en-US copy draft.
- `ConsentPromptVariant` sealed interface in `ui/screen/home/` (`None` / `Main` / `AnalyticsOnly`) — drives the new [consent dialog selection] state on `HomeViewModel`. Computed by `combine(consentPromptShown, analyticsEventsPromptShown, isOnboardingComplete)` with the ADR-011 branching: fresh install gets `Main`, pre-bundle beta testers who already resolved Crashlytics get `AnalyticsOnly`, everyone else gets `None`.
- `HomeViewModel.consentPromptVariant: StateFlow<ConsentPromptVariant>` + `onConsentsResolved(crashAccepted, analyticsAccepted)` for the main dialog (writes all four keys) + `onAnalyticsOnlyResolved(analyticsAccepted)` for the upgrade dialog (writes only the Analytics pair — preserves prior Crashlytics choice per ADR-011 driver #1).
- 10 new `HomeViewModelTest` cases pinning the four-state variant matrix + three resolver-handler scenarios (accept both, decline both, crash-only split) + the Analytics-only upgrade independence invariant in both directions (prior crash `true` and prior crash `false`). Replaces the 7 v1 tests around `shouldShowConsentPrompt` / `onConsentAccepted` / `onConsentDeclined`.
- Settings Privacy section now has a second toggle row — **"Share anonymous usage data"** — sibling to the existing Crashlytics toggle. Wired to `analyticsEventsConsent` via new `SettingsViewModel.analyticsEventsConsent: StateFlow<Boolean>` + `setAnalyticsEventsConsent(Boolean)` handler. Independent switch per ADR-011: flipping one toggle does not touch the other.
- 5 new `SettingsViewModelTest` cases — defaults, round-trip, and pairwise-independence invariants in both directions (flipping Analytics must not flip Crashlytics, and vice versa).
- Developer-only debug panel reachable via a 7-tap Easter-egg gesture on the Settings version text (v3.11.0 / ADR-010 / PRD FR-13). Contains a single "Trigger test crash" button that throws a `RuntimeException` — release builds forward to Firebase Crashlytics, debug builds forward to `NoOpCrashReporter` (the crash still happens, no data leaves). Panel is intentionally unmarked and unlocalized so normal users don't find it.
- `TapCounter` helper in `core/util/` — pure-Kotlin threshold tracker for the Easter-egg gesture (N taps within a rolling window). 6 unit tests cover threshold-hit, window expiry, post-success reset, and the `>` vs `>=` boundary semantics.
- Firebase BoM (`34.5.0`) + Crashlytics SDK declared in the version catalog and applied as a `releaseImplementation` in the `:app` module — foundation for v3.11.0's opt-in crash reporting. Wired behind the `google-services` (`4.4.3`) + `firebase-crashlytics` (`3.0.6`) Gradle plugins.
- Manifest flag `firebase_crashlytics_collection_enabled=false` as the SDK-level default — guards the ~100ms init race before runtime consent is wired, so pre-consent crashes cannot leak.
- `FirebaseCrashlyticsWrapper` interface + `FirebaseCrashlyticsWrapperImpl` in the release source set — thin pass-through around `FirebaseCrashlytics.getInstance()` that serves as a test seam so `FirebaseCrashReporterImpl` stays unit-testable without Robolectric.
- `FirebaseCrashReporterImpl` in the release source set — real `CrashReporter` implementation that forwards `recordException` and `setCollectionEnabled` to the wrapper. No custom keys, no `setUserId`, no user data leakage (PRD FR-10 / FR-11). Consent observation already lives in `ExpenseTrackerApplication.onCreate` (collects `AnalyticsPreferences.analyticsConsent` and calls `setCollectionEnabled` on each emission); the impl stays policy-free.
- Source-set DI split for `CrashReporter` (ADR-010): `src/debug/.../di/CrashReporterModule.kt` binds `NoOpCrashReporter`; `src/release/.../di/CrashReporterModule.kt` binds `FirebaseCrashReporterImpl` + `FirebaseCrashlyticsWrapperImpl`. Debug builds have zero Firebase classes on classpath — collection suppression is a physical rather than a logical guarantee.
- `FirebaseCrashReporterImplTest` under `src/testRelease/` — 4 Mockito-kotlin tests pin the pass-through contract: `recordException` forwards unchanged, `setCollectionEnabled` forwards both `true` and `false`, and toggle ordering is preserved.
- `AnalyticsPreferences.consentPromptShown` flag (default `false`) — persists whether the post-onboarding consent dialog has been shown, so a declined prompt is not re-displayed (ADR-008). Added to the existing `analytics_preferences` DataStore (not a new store); independent from `analyticsConsent` — a user who declines has `consentPromptShown = true` + `analyticsConsent = false`.
- Consent dialog strings (`consent_prompt_title`, `consent_prompt_body`, `consent_prompt_yes`, `consent_prompt_no`) — en-only per ADR-008 + v3.10.0 locale-buckets learning. Copy matches the Design doc: 3-paragraph body explaining what's collected, what's NOT, and Settings-changeable.
- `AnalyticsPreferencesImplTest` — 9 fake-based contract tests covering both default-false states, round-trip, independence of the two keys (flipping one must not flip the other), and the two user-facing scenarios (accept → both true; decline → consent false + promptShown true).
- `ConsentPromptDialog` composable in `ui/component/` — Material 3 `AlertDialog` with full-width "Yes, share crash reports" confirming action, dismissive "No thanks" text button, and back-gesture = decline semantics per ADR-008.
- `HomeViewModel.shouldShowConsentPrompt: StateFlow<Boolean>` — drives the one-shot dialog trigger. True only when onboarding is complete AND `consentPromptShown` is false. Cold-start safe: `combine` naturally resolves both flows before emitting true, so the dialog never flashes over the skeleton state.
- `HomeViewModel.onConsentAccepted()` / `onConsentDeclined()` handlers — accept sets both `analyticsConsent` and `consentPromptShown` to true; decline only sets `consentPromptShown=true` leaving consent at its privacy-safe `false` default (ADR-008).
- 7 new `HomeViewModelTest` cases covering the full consent flow: defaults, onboarding-gated trigger (4 state combinations), accept/decline handlers with independence pinned, and the post-decline dismissal guarantee.
- `FakeOnboardingRepository` extracted to `testutil/` — mirrors the v3.11.0 pattern established by `FakeAnalyticsPreferences`. Exposes a test-only `setOnboardingComplete(Boolean)` for scenarios that need to pin either direction without the real impl's one-way semantics.

### Changed
- CI workflow (`build.yml`) now synthesises `app/google-services.json` from a base64 `GOOGLE_SERVICE_FILE` secret at build time. The file remains `.gitignore`d locally (same pattern as `keystore.jks`). Supersedes ADR-009's initial "commit to repo" recommendation — same outcome (public identifier, not secret), cleaner repo.
- `ReviewModule` no longer binds `CrashReporter` — moved to source-set-specific modules per ADR-010.
- `privacy-policy.md` updated to disclose optional anonymous Firebase Crashlytics crash reporting — off by default, toggleable in Settings, 90-day retention, no PII. Removes the `do not use analytics or tracking SDKs` claim that v3.11.0 renders inaccurate. Fastlane store descriptions were already worded correctly ("Anonymous crash reports are opt-in only") and need no change.
- `HomeScreen` now branches on `consentPromptVariant` to render the new v2 dual-checkbox dialog (fresh installs) or the Analytics-only upgrade dialog — replaces the single-question v1 `ConsentPromptDialog` mount point.
- `settings_analytics_subtitle` tightened from "Help improve the app by sharing crash data. No personal information is collected." to "Stack traces when the app crashes" — parallels the new Analytics row's subtitle pattern per Design doc.
- `privacy-policy.md` Section 2 extended with a new "Optional anonymous usage analytics" subsection mirroring the Crashlytics disclosure's 6-bullet structure. Explicit FR-A6 event list (`app_open`, `onboarding_completed`, `widget_added/removed`, `transaction_added`, `backup_exported/imported`, `insight_shown` with their minimal parameters), FR-A7 "never collected" list (amounts, category names, note text, dates, region-identifying currency codes), GA4 14-month retention, Google LLC processor link. Intro paragraph spells out the two-toggle independence contract up front. Sections 3/4/6 updated so cross-references read "two opt-in Firebase features" instead of "crash reports" singular. Section 7 Change log entry consolidated into a single v3.11.0 line per FR-A14.
- Fastlane `full_description.txt` Privacy-First section updated in both locales (en-US + vi-VN) from "Anonymous crash reports are opt-in only" to "Anonymous crash reports and usage data are opt-in only" (vi-VN: "Báo cáo lỗi và dữ liệu sử dụng ẩn danh chỉ khi bạn đồng ý") per FR-A16. Single-sentence change on line 29 of each file.
- `Analytics` domain interface + `AnalyticsEvent` sealed hierarchy + `NoOpAnalytics` in `domain/analytics/` (Tasks 4.2 + 4.3 / PRD FR-A6, FR-A7, FR-A9). Interface carries only two methods (`logEvent`, `setCollectionEnabled`) mirroring `CrashReporter`. Sealed hierarchy enumerates the exact 8 FR-A6 events (`AppOpen`, `OnboardingCompleted`, `WidgetAdded`, `WidgetRemoved`, `TransactionAdded`, `BackupExported`, `BackupImported`, `InsightShown`) with typed enum-only parameters (`BuildType`, `WidgetSize`, `TransactionKind`, `BackupFormat`, `InsightRowType`) — no `Map<String, Any>` catch-all means FR-A7 compliance is a compile-time property, not a runtime discipline. 11 domain-layer tests pin wire-value shape (`[a-z][a-z0-9_]*`), per-enum uniqueness, exact membership matching the privacy-policy disclosure, and the `data object` shape of parameterless events (so any future additive change forces a policy update). Firebase Analytics SDK (`firebase-analytics`, BoM-managed) added as `releaseImplementation` — mirrors Crashlytics ADR-010 scoping so debug builds have zero Firebase classes on classpath. `NoOpAnalytics` binding + `FirebaseAnalyticsImpl` wiring to follow in Task 4.4 / 4.5.
- `FirebaseAnalyticsWrapper` + `FirebaseAnalyticsImpl` in the release source set (Task 4.4 / PRD FR-A8, FR-A9, FR-A10, FR-A12). Wrapper mirrors the `FirebaseCrashlyticsWrapper` seam from PR #103 — SDK-shaped `(name, params)` signature so sealed-hierarchy serialization stays in the impl, not the seam. Impl maps all 8 `AnalyticsEvent` subtypes to their snake_case event names (`app_open`, `widget_added`, `transaction_added`, etc.) and parameter names (`build_type`, `widget_size`, `type`, `format`, `row_type`) verbatim from the PRD FR-A6 table and the privacy-policy disclosure. The `when` over the sealed hierarchy is compiler-checked exhaustive — adding a new event (in a PR that also updates the policy + PRD) fails compile here until the mapping is added. 15 new `testRelease` cases pin the event-name + param-shape mapping end-to-end. `AndroidManifest.xml` gains four Firebase Analytics meta-data flags: `firebase_analytics_collection_enabled=false` (master gate, covers the ~100ms init race pre-consent), `google_analytics_automatic_screen_reporting_enabled=false` (suppresses SDK-default `screen_view`), plus `google_analytics_adid_collection_enabled=false` and `google_analytics_ssaid_collection_enabled=false` as FR-A7 defense-in-depth against future re-enable accidents. DI binding to follow in Task 4.5.
- Source-set DI split for `Analytics` (Task 4.5 / ADR-010 / PRD FR-A10). `src/debug/.../di/AnalyticsModule.kt` binds `NoOpAnalytics` → `Analytics`; `src/release/.../di/AnalyticsModule.kt` binds `FirebaseAnalyticsImpl` + `FirebaseAnalyticsWrapperImpl`. Mirrors the `CrashReporterModule` pattern from PR #103 — debug builds resolve `Analytics` without Firebase SDK on classpath, release builds forward through the wrapper seam. Observer wiring (Task 5.4) can now consume `Analytics` from Hilt.
- `ExpenseTrackerApplication` now observes `analyticsEventsConsent` in a dedicated second `applicationScope.launch` and forwards every emission to `analytics.setCollectionEnabled(consent)` (Task 5.4 / PRD FR-A5). Independent observer loop — decoupled from the existing Crashlytics consent observer so a pause on one DataStore flow can't starve the other. Runtime gate; the manifest master flag `firebase_analytics_collection_enabled=false` from PR #116 keeps SDK collection off until this observer flips it after user opt-in, so the ~100ms init race pre-consent remains covered.

### Removed
- `ConsentPromptDialog` composable (PR #105) + its v1 strings (`consent_prompt_title/body/yes/no`) — superseded by `ConsentPromptDialogV2` + `AnalyticsOnlyConsentDialog`. Originally planned for v3.12 cleanup per PR #110's description, brought forward into Task 5.6 since the HomeScreen swap is the natural clean-up point. Removes dead code in the same review cycle.
- `HomeViewModel.shouldShowConsentPrompt` + `onConsentAccepted` / `onConsentDeclined` handlers — replaced by `consentPromptVariant` + `onConsentsResolved` / `onAnalyticsOnlyResolved`. Seven corresponding test cases removed; ten new cases cover the expanded variant matrix.

### Security
- Audited all 5 existing `CrashReporter.recordException` call sites (`SettingsViewModel.observeDefaultCurrency`, `BackupRepositoryImpl.importBackup` + `exportCsv`, `RecurringTransactionWorker.doWork`, `WidgetRefreshWorker.doWork`, `BudgetAlertWorker.doWork`): all pass caught `Exception` objects directly with no user data concatenated into messages. PRD FR-10 invariant holds.

## [3.10.0] - 2026-05-02

### Added
- Glance + Compose widget dependencies in the version catalog (`androidx.glance:glance-appwidget` 1.1.1, `androidx.glance:glance-material3` 1.1.1) — declaration only; no widget code or manifest wiring yet
- `ExpenseWidgetState` + `BudgetDisplay` data classes and pure-Kotlin `mapExpenseWidgetState()` — foundation for the v3.10.0 home-screen widget. Takes pre-filtered month transactions, default currency, optional budget, clock inputs, and a `CurrencyFormatter` and emits a fully-formatted widget state. Filters income and non-default-currency transactions silently per v1 scope.
- `ExpenseWidget` GlanceAppWidget shell + `ExpenseWidgetContent` small (2×1) layout — renders today's expense total and a circular "+" affordance. State fetching, click actions, receiver registration, and Material You theming land in subsequent Widget epic tasks; this PR is layout-only and consumes `ExpenseWidgetState.LOADING` as placeholder input.
- `androidx.glance:glance-appwidget` and `androidx.glance:glance-material3` now consumed by the `:app` module (declaration landed in an earlier PR).
- Medium (4×2) widget layout variant — renders Today + This Month totals + a budget progress bar when a monthly budget is set. `ExpenseWidget.sizeMode = SizeMode.Responsive(setOf(SMALL_SIZE, MEDIUM_SIZE))` dispatches by widget size on Android 12+, with width-threshold fallback for older versions. Over-budget progress renders in Material `error` color with a `↑` glyph in the percent label for colorblind accessibility.
- `ExpenseWidgetReceiver` + `AndroidManifest` `<receiver>` entry + `res/xml/expense_widget_info.xml` provider metadata — makes the widget appear in the launcher picker. Sized for a 2×1 default (`targetCellWidth/Height = 2/1`) with resize up to 4×2; `updatePeriodMillis = 0` since refresh is handled by WorkManager (Task 1.8) and repository hooks (Task 1.7). Placeholder preview uses `@mipmap/ic_launcher` until Task 1.11.
- Widget click actions — tapping the "+" button opens `AddEditTransactionScreen` (new transaction); tapping elsewhere on the widget opens the app's Home tab. Both routes launch `MainActivity` with `launchMode="singleTop"` + `FLAG_ACTIVITY_CLEAR_TOP` so re-tapping while the app is open reuses the existing task.
- `MainActivity.EXTRA_LAUNCH_ADD_TRANSACTION` intent extra + `onNewIntent` handling — widget "+" action sets the extra, MainActivity forwards a monotonic tick to `ExpenseTrackerApp` which navigates to the add-transaction modal via `LaunchedEffect`. Repeated widget taps re-fire navigation (the tick changes per tap).
- Widget wrapped in `GlanceTheme { }` — picks up Material You dynamic color on Android 12+, falls back to Glance's built-in neutral scheme on Android 8–11. Widget is intentionally palette-neutral to blend with the user's launcher theme.
- `WidgetUpdater` domain interface + `GlanceWidgetUpdater` implementation — refresh hook invoked after each transaction write, default-currency change, and budget set/clear. `TransactionRepositoryImpl`, `CurrencyPreferenceRepositoryImpl`, and `BudgetPreferencesImpl` now request a widget update (`NonCancellable + @IoDispatcher`, best-effort with logged failures) so the home-screen widget reflects changes without waiting for the periodic WorkManager refresh (Task 1.8).
- `WidgetEntryPoint` Hilt `@EntryPoint` — provides Singleton-scoped dependencies (transaction/budget/currency repos, `CurrencyFormatter`, `TimeProvider`) to `ExpenseWidget.provideGlance`, which now fetches real current-month data on every refresh and hands the mapped `ExpenseWidgetState` to the Glance composable tree instead of `LOADING`.
- `WidgetRefreshWorker` periodic WorkManager job (30-minute interval, `KEEP` policy) — backstops the repository-write refresh hooks so the widget reflects day rollovers and external state changes when the app isn't foregrounded; scheduled from `ExpenseTrackerApplication.onCreate` alongside the existing recurring-transaction and budget-alert workers.
- Widget accessibility pass — each layout now carries a single coherent `contentDescription` so TalkBack announces one sentence per widget focus instead of stepping through each label/amount text node. Over-budget state spells out "Over budget by X" (backed by a new pre-formatted `BudgetDisplay.overByFormatted` field) so screen readers don't have to parse the raw `↑` glyph. Add-transaction button gets its own focused description; loading state announces "Loading widget data".
- Widget launcher-picker preview — `android:previewLayout` (`@layout/widget_preview`) shows a static mock of the medium layout on Android 12+ (today + month + "+" button + budget progress bar), with `android:previewImage` fallback drawable for Android 8–11. Replaces the placeholder `@mipmap/ic_launcher` so users see what the widget actually looks like before placing it.
- Four new `ExpenseWidgetStateMapperTest` cases — today-dated non-default currency must not inflate the today bucket (guards filter ordering), strict `>` vs `>=` boundary for `isOverBudget`, and two time-zone cases (`America/New_York` day partitioning, DST spring-forward boundary) that verify the mapper uses the injected `ZoneId` rather than `ZoneId.systemDefault()`. Total coverage: 17/17 green.
- `domain/insights/` package — `InsightsEngine` pure-Kotlin orchestration with `InsightsResult` + `InsightRow` sealed hierarchy (BiggestMover / DailyPace / NoBudgetFallback / DayOfMonth / Empty / Error). Filters income + non-default-currency transactions upfront and defines the slot-promotion rules (BiggestMover slot 1, else promote DailyPace/NoBudgetFallback; DayOfMonth in slot 3). Individual algorithms remain stubbed until Tasks 2.2 / 2.3 / 2.4.
- `InsightsCollapsePreferences` DataStore toggle — persists whether the Summary-tab Insights section is collapsed. Key `insights_collapsed` (default `false`) in a new `insights_preferences` DataStore; interface + impl in `data/preferences/`, Hilt binding in `ReviewModule`. Consumed by the Summary ViewModel + Insights composable in follow-up Tasks 2.9 / 2.11.
- Biggest month-over-month category mover algorithm in `InsightsEngine` (replaces the Task 2.1 stub) — ranks categories by `|percentChange|` with a ≥2-transaction threshold in both months and a delta floor (`|delta| ≥ 5% of current month total` OR `|delta| ≥ 100k minor units` for zero-decimal currencies / `≥ 10k minor units` for two-decimal currencies). Returns `null` when no category clears both filters, which collapses slot 1 and lets the orchestrator promote slot 2 per PRD Open Question 1. 8 new unit tests pin the happy paths, filters, ranking, tie-break, OR semantics of the floor, and currency-aware absolute threshold.
- Daily-pace-vs-budget algorithm in `InsightsEngine` (replaces the Task 2.3 stub) — projects `spend * daysInMonth / daysElapsed` (integer arithmetic, no float rounding at boundaries) and branches into ON_PACE / OVER / UNDER against a ±5% slack around the user's budget. `InsightRow.DailyPace` carries pre-formatted projected + budget strings; `differenceFormatted` is `null` on ON_PACE and the absolute overage/shortfall on OVER/UNDER. 6 new tests pin the three status branches, both ±5% slack boundaries, and the zero-spend edge case.
- Day-of-month comparison algorithm in `InsightsEngine` (replaces the Task 2.4 stub) — compares current-month spend from day 1 through *today* against prior-month spend through the same day number. Prior-month day cap clamps to `min(today.dayOfMonth, prevYearMonth.lengthOfMonth())` so a Mar 31 "today" compares against Feb 1–28 (fair calendar positioning). When the prior window sums to zero, `percentChange` and `direction` are `null` so the UI can render FR-15's "you've spent X so far this month" fallback copy; when both windows are zero the row is suppressed. Integer-only percent math matches the biggest-mover convention.
- No-budget informational fallback in `InsightsEngine` (replaces the Task 2.7 stub) — emits `InsightRow.NoBudgetFallback` with current-month spend and an implied daily average (`spend / daysElapsed`, integer-truncated) when no budget is set for the default currency. Uses the same day-elapsed clamp as `computeDailyPace` so the two insights stay consistent when a user toggles a budget on and off. Suppresses the row when the current month has zero spend. Orchestrator already routed `budgetAmount <= 0` to this path; covered by a regression test.
- `SummaryViewModel.insightsState: StateFlow<InsightsUiState>` — wires `InsightsEngine` into the Summary tab. Combines the current + previous month expense flows, default currency, its budget, and the `InsightsCollapsePreferences` toggle into a single pipeline, computes through `computeInsights` on `@IoDispatcher`, and stabilizes via `debounce(200ms)` + `stateIn(WhileSubscribed(5000L))`. Year view short-circuits to `InsightsUiState.Hidden` (PRD FR-20); data-layer throws surface as `InsightsUiState.Error` (PRD FR-19). `setInsightsCollapsed(Boolean)` persists the section's collapsed state. 5 Turbine-based unit tests cover happy path, collapse flip, year-mode hide, error path, and the 200ms debounce coalescing a bulk-restore storm.
- Summary tab Insights section — card-style section rendered above the first currency summary card on the Summary tab, in MONTH mode only (PRD FR-01, FR-20). Dispatches on `InsightsUiState`: `Loading` renders a 3-row shimmer (FR-18), `Error` renders an "Insights unavailable" row (FR-19), `Populated` renders up to three `InsightRowItem` rows (biggest mover / daily pace / no-budget fallback / day-of-month) with a collapse toggle on the header (FR-21). Rows carry a coherent `contentDescription` so TalkBack announces one sentence per focus; delta chips use ↑/↓ glyphs alongside color (colorblind-safe per FR-03). EN-only until a dedicated full-app localization milestone lands.

### Changed
- Insights string rendering is now locale-correct: percent values (delta chip, biggest-mover headline, day-of-month headline) render via `NumberFormat.getPercentInstance(locale)` instead of hardcoded `%%`, and the empty-row "title — subtitle" line is a single `insights_empty_combined` format string rather than in-Kotlin concatenation. No user-visible change at en-US, but the code is now ready to drop a full translation bucket into `values-*/` without further Kotlin edits. 4 unit tests pin the `formatPercent` contract at en-US, vi-VN, zero, and 100% edges.

## [3.9.0] - 2026-04-25

### Added
- `BackupCrypto` primitive for AES-256-GCM encrypted backups with PBKDF2-HMAC-SHA256 (200k iterations) key derivation
- `.etbackup` file format: 4-byte magic (`ETBK`) + version + salt + IV + GCM-authenticated ciphertext
- `BackupCryptoException` sealed type: `WrongPassword`, `MalformedHeader`, `UnsupportedVersion`, `DecryptionFailed`
- `docs/backup-format.md` — specification for the `.etbackup` container (byte layout, crypto params, version policy, threat model)
- `BackupRepository.exportBackup` / `importBackup` accept optional `EncryptOptions(password)` — when present, export writes an `.etbackup` and import transparently decrypts; import auto-detects the `ETBK` magic and falls back to the existing plain/gzip path otherwise
- Settings: encrypt-backup toggle and password dialog wired into the export flow — when enabled, export writes a `.etbackup` protected by a user-supplied password (`PasswordDialog` composable with show/hide toggle, 8-char minimum, confirm-password match)
- Settings: import-side password prompt — picking a `.etbackup` file auto-detects the `ETBK` magic and prompts for the decrypt password before any DB writes; wrong passwords re-surface the dialog with an inline error and preserve the picked URI so users can retry without re-picking
- `error_import_file_corrupted` string — dedicated, placeholder-free copy for non-password crypto failures (`MalformedHeader` / `UnsupportedVersion` / `DecryptionFailed`), avoids the dangling "`: `" produced by `error_import_failed`'s `%1$s` when no detail message is safe to surface
- Settings: one-time forgotten-password warning dialog — first time a user enables the "Encrypt backup with password" toggle, an `AlertDialog` explains that lost passwords can't be recovered; the toggle only persists after the user taps "I understand"
- `BackupEncryptionPreferences.hasAcknowledgedPasswordWarning` DataStore flag (key `has_acknowledged_password_warning`) — gates the first-run warning; once set, subsequent toggle changes bypass the dialog

### Fixed
- Encrypt-backup Switch no longer visually bounces back to OFF under the forgotten-password warning dialog — the Switch now renders `checked = true` optimistically while the warning is pending and only returns to OFF if the user cancels

### Changed
- Lift `ETBK` magic detection into `BackupCrypto.isEtbkHeader()` so `BackupRepositoryImpl` no longer duplicates the magic-byte constant
- `EncryptOptions` implements `AutoCloseable`; `close()` zeroes the password array (best-effort — JVM may have copied it already)
- `PasswordDialog` gains a `requireConfirm` parameter (defaults to `true` for export); import decrypt mode renders a single password field with optional inline error text instead of the confirm field

## [3.8.0] - 2026-04-19

### Added
- Privacy Policy and Terms of Service links in Settings (opens in browser)
- Share App button in Settings with Play Store link via Android share sheet
- Graceful fallback toast when no browser is installed for external links
- Terms of Service document (`terms-of-service.md`) for GitHub Pages hosting

### Changed
- Home screen: FilterChips and ActiveFilterBar moved into `topBar` slot — pinned below TopAppBar with scroll tint
- Home screen: MonthSelector scrolls away with content inside the LazyColumn
- Home screen: replaced outer Column + nested TransactionsList LazyColumn with single unified LazyColumn
- Enable dynamic color (Material You) on Android 12+ devices
- Add `pinnedScrollBehavior()` + `nestedScroll` to TopAppBar on Home, Summary, Settings, and Gold Portfolio screens
- Replace hardcoded chart colors with M3 `colorScheme` tokens in `ChartColors`
- Remove 25 redundant `fontWeight = FontWeight.Medium` overrides on `titleMedium`/`titleSmall` Text (M3 default is already Medium)
- Extract `80.dp` FAB clearance magic number into `DesignSystemSpacing.fabClearance` constant

### Fixed
- Edge-to-edge: content now scrolls behind NavigationBar instead of being clipped above it
- Last list items on Home, Summary, Gold, and Settings screens are fully visible when scrolled to bottom
- Status bar color now matches TopAppBar surface tint on scroll (inner Scaffold handles insets instead of outer)
- FAB and Snackbar no longer hidden behind NavigationBar on Home, Gold Portfolio, and Settings screens
- FAB no longer overlaps last list item — added FAB clearance to LazyColumn contentPadding on Home and Gold screens
- FAB position no longer double-counts system insets (restored `contentWindowInsets` on inner Scaffolds)
- Gold Portfolio: removed redundant `bottom_spacer` item (now handled by `contentPadding`)

### Removed
- Inline Privacy Statement, Data Privacy, and Data Storage text from Settings (replaced by Privacy Policy link)

## [3.6.0] - 2026-04-16

### Added
- Room migration v6→v7: `buy_back_price_per_unit` nullable column on `gold_prices` table
- `buyBackPricePerUnit` field on `GoldPriceEntity`, `GoldPrice` domain model, `BackupGoldPriceDto`, and mappers
- String resources for dealer sell/buy-back price labels, market/liquidation value, and accessibility descriptions
- Instrumented migration tests for v6→v7 (column existence, null default, data preservation)
- `GoldHoldingWithPnL`: `liquidationValue`, `liquidationPnL`, `liquidationPnLPercent` computed properties based on buy-back price
- `GoldPortfolioSummary`: `totalLiquidationValue`, `liquidationPnL`, `liquidationPnLPercent` for portfolio-level liquidation P&L
- `PriceInput` data class for dual sell/buy-back price input in `savePrices()`
- Unit tests for dual P&L (market + liquidation) on `GoldPortfolioSummaryTest` (12 tests) and `GoldPortfolioViewModelTest` (18 tests)
- Dual-input `UpdatePricesBottomSheet`: separate sell price and buy-back price fields per gold type/unit
- Inline cross-field validation: buy-back price cannot exceed sell price, with real-time error feedback
- Pre-populates existing buy-back prices from current price data
- Portfolio summary card shows Market Value + Liquidation Value (when buy-back prices exist) with dual P&L display
- Holding cards show liquidation-based P&L when buy-back price is set, market-based P&L as fallback
- Estimated P&L indicator (`~` prefix + muted color) on holding cards when buy-back price is missing
- Current prices section shows buy-back prices alongside sell prices
- CSV gold P&L summary: Buy-Back Price, Market Value, and Liquidation Value columns
- Unit tests for backup import backward compat: old JSON without `buy_back_price_per_unit` defaults to null
- Unit test for round-trip gold price serialization with `buyBackPricePerUnit`

### Changed
- `GoldPrice.pricePerUnit` renamed to `sellPricePerUnit` for clarity (entity column name unchanged)
- `BackupGoldPriceMapper` maps new `buyBackPricePerUnit` field bidirectionally
- Backup import backward compatible: old JSON without `buy_back_price_per_unit` defaults to null
- `GoldHoldingWithPnL.currentPricePerUnit` renamed to `currentSellPricePerUnit`, added `currentBuyBackPricePerUnit`
- `GoldHoldingWithPnL`: `currentValue`/`pnL`/`pnLPercent` renamed to `marketValue`/`marketPnL`/`marketPnLPercent`
- `GoldPortfolioSummary.totalCurrentValue` renamed to `totalMarketValue`, `totalPnL`/`pnLPercent` renamed to `marketPnL`/`marketPnLPercent`
- `GoldPortfolioViewModel.savePrices()` accepts `Map<Pair, PriceInput>` instead of `Map<Pair, Long>` for dual-price support
- `buildPortfolioState()` passes buy-back prices through to `GoldHoldingWithPnL` and computes `totalLiquidationValue`
- `UpdatePricesBottomSheet` refactored from single-field to dual-field layout with `sellInputs` and `buyBackInputs` state maps
- `PortfolioSummaryCard` "Current Value" label changed to "Market Value"; shows liquidation P&L as primary when available with market P&L as secondary
- `HoldingCard` P&L display uses liquidation value when buy-back price exists, falls back to market value with `~` estimated indicator
- CSV gold summary header: `Value` → `Market Value`, added `Buy-Back Price` and `Liquidation Value` columns; P&L uses liquidation when available

## [3.5.0] - 2026-04-09

### Added
- Budget alert notifications at 80% (warning) and 100% (over budget) thresholds via WorkManager
- Budget alert preferences (enable/disable, warning threshold) with Settings UI toggle
- POST_NOTIFICATIONS runtime permission for Android 13+ with rationale
- Daily budget check worker with per-month dedup and immediate check on transaction save
- Notification tap opens Home screen
- Collapsible search bar in AppBar with badge dot when query is active
- All Months toggle chip for cross-month transaction search
- CategoryFilterBottomSheet with grouped expense/income categories and radio selection
- DateRangePicker dialog in ModalBottomSheet for custom date range filtering
- Active filter bar with dismissible InputChips and clear-all action
- Persist all search filter preferences (scope, type, category, date range) across app restarts via DataStore
- Advanced search DAO query with nullable date range, category, and type filters (NULL bypass pattern)
- SearchScope enum (CURRENT_MONTH / ALL_MONTHS) for cross-month search
- searchTransactionsAdvanced() in TransactionRepository interface and impl
- Stale category auto-cleanup when persisted category filter no longer exists
- Instrumented Room in-memory DB tests for searchTransactionsAdvanced DAO query (12 tests)
- Unit tests for BudgetAlertPreferences (defaults, read/write, overwrite) with shared fake
- Unit tests for HomeViewModel filter persistence (18 tests)

### Changed
- Search bar moved from standalone field to collapsible TopAppBar — SearchBar composable replaced by SearchTopBar
- MonthSelector shows visual disabled state (alpha + disabled buttons) when All Months is active
- Date range chip shows formatted date range when active instead of generic "Date range" label
- CategoryFilterBottomSheet uses heightIn(max=400dp) instead of fixed height for better adaptiveness
- Empty state now shows "no results" message when filters are active with no matches

### Fixed
- MigrationTest failures — all 13 tests now pass by providing full migration chain (v1→v6) to Room.databaseBuilder
- Date range re-selection when scope is already All Months now triggers re-query (was silently deduped by MutableStateFlow)
- DateRangePicker prevents selecting future dates via SelectableDates constraint
- Clearing "All months" scope badge now also clears date range to prevent inconsistent filter state

## [3.4.0] - 2026-04-05

### Added
- Auto-focus amount/weight field on form open in add mode for faster input
- Inline validation with error states on Amount field (Recurring) and Weight/Buy Price fields (Gold)
- Loading state with spinner on Add/Edit Recurring Transaction screen
- Edit mode for recurring transactions with tap-to-edit, DAO getById, and SavedStateHandle-based ViewModel
- Full-screen ErrorStateMessage with retry on Home and Gold Portfolio screens
- BackHandler + discard confirmation dialog on Add/Edit Recurring Transaction screen
- Categories empty state uses EmptyStateMessage component
- Loading spinner contentDescription for Summary, Gold Portfolio, and Add/Edit Gold Holding screens
- Onboarding page indicator semantics ("Page X of Y")
- Retry methods on HomeViewModel and GoldPortfolioViewModel
- UiText sealed class for ViewModel string resource references without Android Context dependency
- All user-facing strings extracted to strings.xml across all screens, ViewModels, and components (~150+ strings)

### Changed
- Form field order: Amount/Weight now first on all form screens for faster data entry
- Discard dialog now shows in add mode (all 3 forms) when user has entered data, preventing accidental data loss
- ErrorUtils.getErrorMessage() returns UiText instead of String for i18n support
- All ViewModel errorMessage/backupMessage fields migrated from String? to UiText?
- Month label constants in MonthYearPickerDialog and MonthlyBarChart converted to composable functions using stringResource()
- Unit tests updated to assert UiText types instead of raw strings (ErrorUtilsTest, AddEditTransactionViewModelTest, SettingsViewModelTest)

### Fixed
- Large gap between Note field and keyboard on form screens — removed imePadding from bottomBar so keyboard naturally covers the save button
- Budget error message uses string resource instead of hardcoded string
- HomeViewModel retry() was a no-op due to MutableStateFlow same-value dedup; now uses counter-based retryTrigger
- HomeViewModel catch moved inside flatMapLatest so errors don't kill the combine flow and retry works
- AddEditRecurringTransactionViewModel loadExisting() could hang on infinite category flow; now uses first() for initial load
- AddEditRecurringTransactionViewModel save() preserves isActive state instead of hardcoding true
- Edit mode hasUnsavedChanges compares against original loaded state instead of just checking non-empty fields
- Error state messages on Home and Gold Portfolio screens use string resources instead of hardcoded strings

### Removed
- Dead DateSelector composable from AddEditTransactionScreen
- Unused AVAILABLE_ICONS list from CategoriesScreen
- Unused recurringId variable extraction in ExpenseTrackerApp navigation

## [3.3.0] - 2026-03-22

### Added
- **Add/Edit Gold Holding Screen**: Full form with gold type selector, weight/unit inputs, buy price with currency formatting, date picker, and note field
- **AddEditGoldHoldingViewModel**: SavedStateHandle-based ViewModel supporting add and edit modes with form validation
- **Swipe-to-delete on gold holdings**: SwipeToDismissBox with undo snackbar on portfolio screen
- **Tap to edit gold holdings**: HoldingCard now navigates to edit screen on tap
- **Navigation wiring**: Modal route for AddEditGoldHolding with holdingId parameter
- **Gold holdings backup/restore**: Gold holdings included in JSON backup export and import with backward-compatible schema
- **Gold holdings CSV export**: Gold holdings section appended to CSV export with Date, Type, Weight, Unit, Buy Price, Currency, Note columns
- **BackupGoldHoldingDto**: Serializable DTO for gold holdings in backup documents
- **BackupGoldHoldingMapper**: Entity-to-DTO and DTO-to-entity mapping extensions
- **Backup validation for gold**: 6 new validation rules (duplicate ID, invalid type/unit, non-positive weight, negative price, unsupported currency)
- **Gold prices backup/restore**: Current gold prices included in JSON backup export and import with backward-compatible schema
- **BackupGoldPriceDto**: Serializable DTO for gold prices in backup documents
- **BackupGoldPriceMapper**: Entity-to-DTO and DTO-to-entity mapping extensions
- **Gold P&L summary CSV export**: New section in CSV with Type, Unit, Weight, Buy Price, Current Price, Currency, Cost, Value, P&L columns
- 14 unit tests for AddEditGoldHoldingViewModel covering add/edit/validation/error flows

### Fixed
- **CsvExporter**: Reuse single `BufferedWriter` across transaction and gold CSV sections instead of creating multiple writers on the same `OutputStream`

### Changed
- **BackupDocumentV1**: Added optional `gold_holdings` field with empty default (backward compatible, no version bump)
- **BackupRepositoryImpl**: Export includes gold holdings; import conditionally replaces gold data only when backup contains holdings
- **SettingsViewModel**: Import success message now includes gold holding count when applicable

## [3.2.5] - 2026-03-14

### Added
- **ScreenshotSeedActivity**: Debug-only activity that seeds 30 transactions across 6 categories and 3 recurring transactions for screenshot generation.
- **Fastlane metadata**: Play Store listing files for EN and VI locales (short/full descriptions, changelogs).
- **Release notes**: `docs/release-notes-v3.2.5.md` documenting all Phase 6 features.

### Changed
- Bumped `versionName` to `3.2.5`.

## [3.2.4] - 2026-03-14

### Added
- **In-App Review**: Review preferences DataStore for tracking prompt eligibility and cooldown.
- **InAppReviewManager**: Abstraction for in-app review flow with eligibility checks (install age, cooldown, max prompts). No-op implementation ready for Play Review library integration.
- **Feedback Bottom Sheet**: New feedback UI in Settings with two paths -- positive feedback (review trigger) and issue reporting (email intent with device info, no PII).
- **CrashReporter interface**: Abstraction for crash reporting with `NoOpCrashReporter` default implementation. Ready for Firebase Crashlytics when credentials are available.
- **Analytics consent**: DataStore-backed preference for anonymous crash reporting consent, disabled by default.
- **Settings UI**: "Send Feedback" section and "Share anonymous crash reports" toggle in Privacy section.
- **Privacy-safe crash reporting**: RecurringTransactionWorker, BackupRepositoryImpl, and CsvExporter now report non-fatal exceptions via CrashReporter without logging any user data.
- Play Review KTX dependency added to version catalog.

### Changed
- `SettingsViewModel` now accepts `AnalyticsPreferences` and exposes `analyticsConsent` StateFlow.
- `ExpenseTrackerApplication` observes analytics consent to enable/disable crash reporting.
- `BackupRepositoryImpl` now accepts `CrashReporter` for non-fatal exception reporting.
- `RecurringTransactionWorker` now accepts `CrashReporter` for non-fatal exception reporting.

### Fixed
- N/A

## [3.2.3] - 2026-03-14

### Added
- Accessibility enhancements (WCAG AA): DonutChart semantics, MonthlyBarChart per-bar semantics, income/expense indicators, icon button content descriptions, minimum 48dp touch targets, focus order in AddEditTransactionScreen, category color swatch labels.

## [3.2.1] - 2026-03-14

### Added
- Dark mode and theme support (Light/Dark/System) with DataStore persistence.

## [3.1.5] - Previous

### Added
- Empty states and chart interactivity improvements.
