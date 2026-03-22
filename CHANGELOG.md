# Changelog

## [Unreleased]

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
