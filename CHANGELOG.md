# Changelog

## [Unreleased]

### Added
- `BackupCrypto` primitive for AES-256-GCM encrypted backups with PBKDF2-HMAC-SHA256 (200k iterations) key derivation
- `.etbackup` file format: 4-byte magic (`ETBK`) + version + salt + IV + GCM-authenticated ciphertext
- `BackupCryptoException` sealed type: `WrongPassword`, `MalformedHeader`, `UnsupportedVersion`, `DecryptionFailed`
- `docs/backup-format.md` — specification for the `.etbackup` container (byte layout, crypto params, version policy, threat model)
- `BackupRepository.exportBackup` / `importBackup` accept optional `EncryptOptions(password)` — when present, export writes an `.etbackup` and import transparently decrypts; import auto-detects the `ETBK` magic and falls back to the existing plain/gzip path otherwise

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
