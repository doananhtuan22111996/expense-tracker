# Expense Tracker

A simple, offline-first personal expense tracker Android app for managing your income and expenses locally on your device.

## Features

- **Offline-First**: All data is stored locally on your device
- **No Account Required**: No login, registration, or cloud sync
- **Privacy-Focused**: No data collection, analytics, or ads
- **Simple & Clean**: Material 3 design with intuitive navigation
- **Transaction Management**: Add, edit, delete income and expense transactions
- **Category Organization**: Pre-defined categories for expenses and income
- **Monthly & Yearly Summary**: View your financial overview with balance and top expense categories, toggle between month and year view
- **Filter & Search**: Filter transactions by type (All, Expenses, Income) and search by note text

## Architecture

This app follows modern Android development best practices:

- **UI Layer**: Jetpack Compose with Material 3 design
- **Architecture**: MVVM with Repository pattern (UI → ViewModel → Repository → Data)
- **Database**: Room for local SQLite database
- **Dependency Injection**: Hilt for clean dependency management
- **Navigation**: Navigation Compose with bottom navigation
- **State Management**: StateFlow for reactive UI updates
- **Coroutines**: For asynchronous operations

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Database**: Room (SQLite)
- **Dependency Injection**: Hilt
- **Navigation**: Navigation Compose
- **Architecture Components**: ViewModel, LiveData, StateFlow
- **Coroutines**: For background operations
- **Serialization**: kotlinx-serialization-json for backup format
- **Material Design**: Material 3

## Phase 2.1 - Multi-Currency Foundation (v1.2)

### Data Model Upgrade (Backward Compatible)

Added a `currency_code` field to the transactions table to prepare for future multi-currency support:

- **New column**: `currency_code TEXT NOT NULL DEFAULT 'VND'` (ISO 4217 currency code)
- **Default value**: All existing and new transactions default to `VND` (Vietnamese Dong)
- **Room migration**: Automatic migration from database version 1 to 2 via `MIGRATION_1_2`
- **Backward compatible**: Existing data is fully preserved; no user-visible changes
- **Amount unchanged**: The `amount` field remains `Long` and existing amounts are not modified

This is a data-layer-only change. No UI, settings, or display logic is affected. Future phases will
add a currency picker and currency-aware formatting.

### Currency Definition (Offline – Static)

Added a static, offline-only registry of supported currencies with helper APIs:

**Supported Currencies:**

| Code | Name | Symbol | Minor Unit Digits |
|------|------|--------|-------------------|
| VND | Vietnamese Dong | ₫ | 0 |
| USD | US Dollar | $ | 2 |
| EUR | Euro | € | 2 |
| JPY | Japanese Yen | ¥ | 0 |
| KRW | South Korean Won | ₩ | 0 |
| SGD | Singapore Dollar | S$ | 2 |

**Helper APIs (`SupportedCurrencies` object):**
- `all()` — returns all 6 currencies in deterministic order (VND first)
- `byCode(code)` — lookup by ISO 4217 code, returns null if unsupported
- `requireByCode(code)` — lookup by code, throws `IllegalArgumentException` if unsupported
- `default()` — returns VND (the app's default currency)

**Design rules:**
- Offline only: no API calls, no online updates, no `java.util.Currency` dependency
- Static data: currency list is compile-time constant, defined in `domain/model/CurrencyDefinition.kt`
- Thread-safe: immutable data class + Kotlin object singleton
- Domain model defaults (`Transaction.currencyCode`, `TransactionRepository.addTransaction`) reference `SupportedCurrencies.default().code` instead of hardcoded `"VND"`

### Currency Formatter Layer (Locale-Independent)

Added a deterministic, locale-independent currency formatting layer that renders monetary amounts
correctly for all 6 supported currencies. Formatting is pure string manipulation with no dependency
on `java.util.Locale`, `java.text.NumberFormat`, or any Android framework API.

**Interface & Implementation:**
- `CurrencyFormatter` interface (`core/formatter/CurrencyFormatter.kt`) — three methods: `format`, `formatWithSign`, `formatBareAmount`
- `DefaultCurrencyFormatter` (`core/formatter/DefaultCurrencyFormatter.kt`) — `@Singleton`, `@Inject`-constructable production implementation
- `AmountFormatter` object — static facade delegating to `DefaultCurrencyFormatter`, used where Hilt injection is unavailable (Composables, data-class computed properties)
- Hilt binding: `@Binds` in `RepositoryModule` maps `DefaultCurrencyFormatter` to `CurrencyFormatter`

**Formatting Rules by Currency:**

| Currency | Thousands Sep | Decimal Sep | Decimal Places | Symbol Position | Example |
|----------|---------------|-------------|----------------|-----------------|---------|
| VND | `.` (dot) | N/A | 0 | Suffix with space | `120.000 ₫` |
| USD | `,` (comma) | `.` (dot) | 2 | Prefix | `$120.00` |
| EUR | `,` (comma) | `.` (dot) | 2 | Prefix | `€2,500.75` |
| JPY | `,` (comma) | N/A | 0 | Prefix | `¥1,500` |
| KRW | `,` (comma) | N/A | 0 | Prefix | `₩5,000,000` |
| SGD | `,` (comma) | `.` (dot) | 2 | Prefix | `S$120.00` |
| Unknown | `,` (comma) | N/A | 0 | Suffix code | `1,500 GBP` |

**Amount Representation:**
- Currencies with `minorUnitDigits=0` (VND, JPY, KRW): amount Long is the whole-unit value
- Currencies with `minorUnitDigits=2` (USD, EUR, SGD): amount Long is in minor units (cents)

**Design Constraints:**
- Deterministic: output is identical on every device regardless of system locale
- Stateless and thread-safe: no mutable shared state in `DefaultCurrencyFormatter`
- VND remains the default currency throughout the app


## Phase 2.2 - Default Currency Setting (v1.3)

### App-Level Default Currency Preference

Added a persistent, app-level default currency setting accessible from Settings. When the user selects
a default currency, all **new** transactions automatically use that currency. Existing transactions are
never modified.

**Behavior:**
- Navigate to Settings and tap "Default Currency" to open a currency selection dialog
- The selected currency is persisted via Jetpack DataStore (`currency_preferences`)
- When creating a new transaction, the Add/Edit screen reads the current default currency preference
- When editing an existing transaction, the screen uses that transaction's stored `currency_code` (not the preference)
- If no preference has been set, the app defaults to VND (Vietnamese Dong)

**Important: No automatic conversion.** Changing the default currency does not convert amounts on existing
transactions. Each transaction retains the currency it was created with. This is a display/entry default only.

**Offline-only:** The preference is stored locally in DataStore. There are no network calls, no cloud sync,
and no external dependencies. The feature works entirely offline, consistent with the app's privacy-first design.

### Per-Transaction Currency Picker

Added an inline currency picker on the Add/Edit Transaction screen, allowing users to override the default
currency on a per-transaction basis.

- Per-transaction currency can be selected on Add/Edit screen via an inline dropdown
- Changing the currency reformats the current amount text for the target currency
- No conversion / no exchange rates -- the numeric amount is preserved as-is
- In edit mode, changing the currency is tracked as an unsaved change

### Home List Currency Visibility

Each transaction in the Home screen list displays its amount with the correct currency symbol
derived from the transaction's own `currency_code`, not the app-level default currency preference.

- **Per-transaction formatting**: `TransactionItem` passes `transaction.currencyCode` to `AmountText`
- **Symbol rendering**: Supported currencies show their native symbol (₫, $, €, ¥, ₩, S$); unknown currencies fall back to the raw ISO code suffix (e.g., `1,500 GBP`)
- **Accessibility**: Screen reader content descriptions include the currency-formatted amount
- **No conversion**: Amounts are displayed as-is with their stored currency; no exchange rate logic
- **No merging**: Each row uses its transaction's stored `currencyCode` independently

**New Files:**

| File | Layer | Purpose |
|------|-------|---------|
| `domain/repository/CurrencyPreferenceRepository.kt` | Domain | Repository interface (`observeDefaultCurrency`, `setDefaultCurrency`, `getDefaultCurrency`) |
| `data/preferences/CurrencyPreferenceRepositoryImpl.kt` | Data | DataStore-backed implementation with `@Singleton` scope and `@IoDispatcher` |
| `ui/screen/settings/SettingsViewModel.kt` | UI | `@HiltViewModel` exposing `SettingsUiState` with selected currency and available currencies |
| `ui/screen/settings/SettingsScreen.kt` | UI | Settings screen with currency selector dialog, app info, and privacy sections |
| `testutil/FakeCurrencyPreferenceRepository.kt` | Test | In-memory fake with verification flags (`setDefaultCurrencyCalled`, `lastSetCurrencyCode`) |
| `data/preferences/FakeCurrencyPreferenceRepositoryTest.kt` | Test | Validates fake repository behavior (default, set, observe, all supported codes) |
| `ui/screen/settings/SettingsViewModelTest.kt` | Test | ViewModel tests (init load, currency selection, error handling) |

**Modified Files:**

| File | Change |
|------|--------|
| `di/RepositoryModule.kt` | Added `@Binds` for `CurrencyPreferenceRepository` to `CurrencyPreferenceRepositoryImpl` |
| `ui/screen/addedit/AddEditTransactionViewModel.kt` | Injects `CurrencyPreferenceRepository`; new transactions use preference, edits use transaction's code; `onCurrencyChanged()` with amount reformatting |
| `ui/screen/addedit/AddEditTransactionScreen.kt` | Added `CurrencyDropdown` composable between type selector and amount field |
| `ui/screen/addedit/AddEditTransactionViewModelTest.kt` | Added 10 currency-related tests (preference, edit mode, save, currency change, reformatting, unsupported code) |
| `ui/screen/addedit/AddEditTransactionUiStateTest.kt` | Added 2 tests for `hasUnsavedChanges` with currency code changes |

## Phase 2.3 - Monthly Summary Per Currency (v1.4)

### Monthly Summary -- Per Currency

The Summary screen now groups totals by currency instead of showing a single aggregate total.
Each currency section shows its own Income, Expenses, and Balance cards, along with that currency's
top expense categories.

**Behavior:**
- Each currency used in the current month gets its own section with Income/Expenses/Balance cards
- Top 5 expense categories are shown per currency; remaining categories are aggregated into an "Other" row
- Currency sections are ordered by the SupportedCurrencies registry order (VND, USD, EUR, JPY, KRW, SGD), with unknown currencies sorted alphabetically at the end
- A policy-safe disclaimer reads: "Totals are shown per currency. No currency conversion is applied." -- visible whenever the month has at least one transaction, clarifying that totals are per currency with no conversion
- If no transactions exist for the month, the empty state message is shown
- Balance card correctly displays +/- sign with income/expense coloring for positive/negative balances

**Important: No conversion, no combined totals.** Amounts in different currencies are never summed
together. There are no exchange rates, no cross-currency aggregation, and no "total across all
currencies" figure. Each currency section is independent.

**New Files:**

| File | Layer | Purpose |
|------|-------|---------|
| `data/database/entity/CurrencySumRow.kt` | Data | Room projection for per-currency sum queries |
| `data/database/entity/CurrencyCategorySumRow.kt` | Data | Room projection for per-currency-and-category sum queries |

**Modified Files:**

| File | Change |
|------|--------|
| `data/database/dao/TransactionDao.kt` | Added `sumExpenseByCurrency`, `sumIncomeByCurrency`, `sumByCurrencyAndCategory` queries; removed dead `sumExpense`, `sumIncome`, `sumByCategory` methods |
| `domain/model/MonthlySummary.kt` | Restructured to `MonthlySummary(currencySummaries: List<CurrencyMonthlySummary>)` with `isEmpty` property |
| `repository/TransactionRepositoryImpl.kt` | Rewrote `observeMonthlySummary` to use per-currency flows, top-5 + Other aggregation, registry-ordered currency sorting, and orphaned category fallback |
| `ui/screen/summary/SummaryScreen.kt` | Per-currency sections with `CurrencySectionHeader`, `DisclaimerText`, dividers, balance sign fix, and "Other" muted styling |
| `ui/screen/summary/SummaryViewModel.kt` | Empty state check updated for new `isEmpty` property; refresh race condition fix with Job cancellation |

## Phase 3.1 - Backup Schema v1 (v1.5)

### Backup Data Format Foundation

Added an offline-only, versioned backup schema for exporting and importing app data. This is a
data-layer-only change with no UI surface. The backup format is designed for forward compatibility
and safe round-trip serialization of all transaction and category data.

**Backup Document Structure (`BackupDocumentV1`):**

| Field | Type | Description |
|-------|------|-------------|
| `schema_version` | Int | Always `1` for this format version |
| `app_version_name` | String | App version that created the backup (e.g., `"1.5.0"`) |
| `created_at_epoch_ms` | Long | Timestamp when the backup was created |
| `categories` | List | All category records as `BackupCategoryDto` |
| `transactions` | List | All transaction records as `BackupTransactionDto` |

**DTOs mirror entity fields exactly** (Int types, Long IDs) for lossless round-trip fidelity.
JSON field names use `@SerialName` with snake_case for stable, readable output.

**Serialization Configuration:**
- `kotlinx-serialization-json` 1.8.1 (newly added dependency)
- `ignoreUnknownKeys = true` for forward compatibility with future schema versions
- `encodeDefaults = true` for self-documenting JSON (null and default values written explicitly)
- `prettyPrint = true` for human-readable backup files
- `decode()` returns `null` on malformed/invalid input (no exceptions thrown to caller)

**Validation (`BackupValidator`):**
- Non-fail-fast: collects all errors in a single pass
- 9 validation error types: `UnsupportedSchemaVersion`, `DuplicateCategoryId`,
  `DuplicateTransactionId`, `BlankCategoryName`, `InvalidCategoryType`,
  `InvalidTransactionType`, `NegativeAmount`, `UnsupportedCurrencyCode`, `OrphanedTransaction`
- Validates referential integrity (transaction → category foreign key)
- Validates currency codes against `SupportedCurrencies` registry

**Bidirectional Mappers:**
- `CategoryEntity.toBackupDto()` / `BackupCategoryDto.toEntity()`
- `TransactionEntity.toBackupDto()` / `BackupTransactionDto.toEntity()`
- Extension function pattern matching existing `CategoryMapper` / `TransactionMapper`

**ProGuard/R8 Rules:**
- Keep rules for `$$serializer` generated classes, `Companion` objects, and `KSerializer` methods
- Scoped to both `kotlinx.serialization.json` internals and app package

**New Files:**

| File | Layer | Purpose |
|------|-------|---------|
| `data/backup/model/BackupDocumentV1.kt` | Data | `@Serializable` DTOs: `BackupDocumentV1`, `BackupCategoryDto`, `BackupTransactionDto` |
| `data/backup/BackupSerializer.kt` | Data | JSON encode/decode with `kotlinx.serialization` |
| `data/backup/BackupValidator.kt` | Data | Structural validation with sealed error types |
| `data/backup/mapper/BackupCategoryMapper.kt` | Data | `CategoryEntity <-> BackupCategoryDto` bidirectional mapping |
| `data/backup/mapper/BackupTransactionMapper.kt` | Data | `TransactionEntity <-> BackupTransactionDto` bidirectional mapping |

**Test Coverage (48 tests):**

| Test Suite | Tests | Coverage |
|------------|-------|---------|
| `BackupSerializerTest` | 18 | Round-trip, snake_case, pretty print, unknown keys, malformed input, emoji, Long.MAX_VALUE, missing fields, type mismatch |
| `BackupValidatorTest` | 25 | All 9 error types, boundary values (schema 0/-1, Long.MIN_VALUE, empty/lowercase currency, categoryId=0), multi-error collection |
| `BackupCategoryMapperTest` | 5 | Forward, reverse, round-trip, null optional fields |
| `BackupTransactionMapperTest` | 5 | Forward, reverse, round-trip, null note |

## Phase 2 - Feature Enhancements

### Edit Transaction - UX Polish

Enhanced the edit transaction experience with improved user interaction and data safety:

**✅ Unsaved Changes Detection**
- Automatically tracks modifications to transaction fields (type, amount, category, date, note)
- Compares current form state against original transaction data
- Visual feedback through save button state changes

**✅ Back Navigation Confirmation**
- Smart confirmation dialog when attempting to leave with unsaved changes
- Handles both system back button and top app bar back navigation
- Immediate navigation when no changes are detected
- Dialog options: "Discard" (lose changes) or "Cancel" (stay on screen)

**✅ Save Button Intelligence**
- Disabled when form is invalid OR when no changes have been made
- Only enabled when input is valid AND at least one field has been modified
- Prevents unnecessary database operations and user confusion

**✅ Safe Edit Workflow**
- Original transaction data preserved as baseline for comparison
- Clean state management prevents data corruption
- Smooth navigation flow with proper state cleanup

## UX/UI Design Standards

### Material 3 Design System Implementation

The app follows strict Material 3 design guidelines with a custom design system layer for consistency:

**🎨 Design System Location**
- **Design constants**: `ui/theme/DesignSystem.kt`
- **Reusable components**: `ui/component/CommonComponents.kt`
- **Financial semantic colors**: `FinancialColors` object for income/expense/balance styling

**📏 Spacing System**
```kotlin
object DesignSystemSpacing {
    val xs: Dp = 4.dp        // Micro spacing
    val small: Dp = 8.dp     // Component spacing
    val medium: Dp = 12.dp   // Section spacing
    val large: Dp = 16.dp    // Screen padding
    val xl: Dp = 24.dp       // Large sections
    val xxl: Dp = 32.dp      // Major sections
}
```

**📱 Screen Structure Standards**
- **Screen padding**: 16dp consistent across all screens
- **Section spacing**: 12-16dp between major sections
- **Component spacing**: 8dp between related components
- **List item spacing**: 8dp vertical spacing between cards

**✏️ Typography Usage**
- **Screen titles**: `MaterialTheme.typography.headlineMedium`
- **Section headers**: `MaterialTheme.typography.titleMedium`
- **Primary content**: `MaterialTheme.typography.bodyLarge`
- **Secondary content**: `MaterialTheme.typography.bodyMedium`
- **Tertiary content**: `MaterialTheme.typography.bodySmall`

**🎯 Touch Targets**
- **Minimum size**: 48dp for all interactive elements
- **Icon buttons**: Properly sized touch areas
- **Cards**: Full-width clickable areas with visual feedback

**🏷️ Card Elevations**
```kotlin
object DesignSystemElevation {
    val none: Dp = 0.dp      // Form components
    val low: Dp = 2.dp       // List items
    val medium: Dp = 4.dp    // Summary cards
    val high: Dp = 8.dp      // Modal dialogs
}
```

### VND Currency Standards

**💰 VND-Default Policy**: The app defaults to Vietnamese Dong (₫) with a multi-currency foundation (see Phase 2.1)
- **Integer amounts only**: No decimal places (VND doesn't use fractional currency)
- **Thousand separators**: Deterministic dot separators for VND (1.234.567), comma for others
- **Consistent formatting**: Single `CurrencyFormatter` implementation used throughout via `AmountFormatter` facade
- **Color coding**: Green for income, red for expenses, contextual for balance

**🚫 No Charts Constraint**: Deliberately excludes charts/graphs to maintain simplicity
- **Text-based summaries**: Clear numerical displays instead of visual charts
- **Top categories**: Simple list format for expense breakdowns
- **Focus on numbers**: Emphasizes precise amounts over visual representations

### Reusable Components

**`AmountText`**: Consistent VND formatting with semantic colors
```kotlin
AmountText(
    amount = 1234567L,
    transactionType = TransactionType.INCOME,
    showSign = true,
    fontWeight = FontWeight.Bold
)
```

**`EmptyStateMessage`**: Standardized empty states
```kotlin
EmptyStateMessage(
    title = "No transactions yet",
    subtitle = "Tap the + button to add your first transaction"
)
```

**`SectionHeader`**: Consistent screen/section titles
```kotlin
SectionHeader(title = "This Month")
```

### Accessibility Standards

**🔍 Content Descriptions**: All interactive icons include descriptive labels
**🎨 Color Contrast**: Uses Material 3 semantic colors for sufficient contrast
**👆 Touch Targets**: Minimum 48dp touch areas for all interactive elements
**📖 Text Hierarchy**: Clear visual hierarchy with appropriate font sizes and weights

### Development Guidelines

**🔧 Adding New UI Components**
1. Use design system spacing constants (`DesignSystemSpacing.*`)
2. Follow Material 3 typography scale consistently
3. Apply semantic colors from `FinancialColors` for money amounts
4. Include content descriptions for accessibility
5. Maintain consistent card elevations using `DesignSystemElevation`

**✅ UI Validation Checklist**
```bash
# Run before committing UI changes
./gradlew spotlessCheck        # Code formatting
./gradlew test                # Unit tests including AmountFormatter
./gradlew assembleDebug       # Compilation check
```

**⚡ Quick UI Testing**
- Test on multiple screen sizes (phone, tablet)
- Verify touch targets are >= 48dp
- Check VND formatting in all contexts
- Confirm empty states display properly
- Test dark/light mode consistency

## Phase 3.2 - Export Backup (Offline) (v1.6)

### Export Backup via Settings

Added user-facing backup export functionality to the Settings screen, building on the Phase 3.1
schema foundation. Uses Android's Storage Access Framework (SAF) for secure, offline file access
without requiring storage permissions. This phase implements **export only**; import will follow
in a future phase.

**How to Export:**
1. Open Settings > "Backup & Restore"
2. Tap "Export Backup"
3. Choose a save location in the system file picker
4. The app writes a JSON backup file containing all your data

**File Format:**
- Format: JSON (`application/json`)
- Schema version: 1 (`BackupDocumentV1`)
- Default filename: `expense-tracker-backup_YYYY-MM-DD.json`
- Encoding: UTF-8, pretty-printed

**Backup Contents:**
- All categories (sorted by ID)
- All transactions (sorted by ID)
- Default currency code preference
- Device locale at export time
- App version and schema version
- Export timestamp

**Offline-Only:** The export is entirely offline. The app does not upload your data. You choose
where to save the backup file (internal storage, SD card, or a cloud-synced folder via the system
file picker). The app itself makes no network calls.

**Export Flow (Technical):**
1. User taps "Export Backup" in Settings > Backup & Restore
2. SAF file picker opens with suggested filename `expense-tracker-backup_YYYY-MM-DD.json`
3. `BackupAssembler` deterministically builds `BackupDocumentV1` from DAOs + preferences
4. `BackupSerializer.encode()` converts to pretty-printed JSON
5. JSON is written to the selected URI via `ContentResolver` on IO dispatcher
6. Success/error feedback shown via Snackbar

**Key Components:**

| Component | File | Role |
|-----------|------|------|
| `BackupAssembler` | `data/backup/BackupAssembler.kt` | Pure, deterministic document assembly (sorts by ID) |
| `BackupRepositoryImpl` | `data/backup/BackupRepositoryImpl.kt` | Reads DAOs + preferences, delegates to assembler + serializer |
| `TransactionRunner` | `data/database/TransactionRunner.kt` | Abstraction over `RoomDatabase.withTransaction` for testability |
| `SettingsViewModel` | `ui/screen/settings/SettingsViewModel.kt` | Orchestrates export with `BackupOperation` state, uses `@IoDispatcher` |
| `SettingsScreen` | `ui/screen/settings/SettingsScreen.kt` | Backup & Restore section with SAF launcher, loading state, Snackbar |

**BackupDocumentV1 Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `schema_version` | Int | Always `1` |
| `app_version_name` | String | App version that created the backup |
| `created_at_epoch_ms` | Long | Timestamp when the backup was created |
| `default_currency_code` | String | User's default currency preference |
| `device_locale` | String | Device locale at export time (BCP 47) |
| `categories` | List | All categories sorted by ID ascending |
| `transactions` | List | All transactions sorted by ID ascending |

**Test Coverage:**

| Test Class | Tests | Coverage |
|------------|-------|----------|
| `BackupAssemblerTest` | 7 | Sorting, metadata, determinism, empty lists, fixture stability |
| `BackupRepositoryImplTest` | 15 | Export mapping, sorting, new fields, import atomicity, validation |
| `BackupSerializerTest` | 22 | Round-trip, new fields, backward compat, snake_case, edge cases |
| `SettingsViewModelTest` | 12 | Currency selection, export lifecycle, error handling |

## Phase 3.3 - Import / Restore (Replace All) (v2.1)

### Import Backup (Restore) via Settings

Added user-facing backup import functionality to the Settings screen, completing the backup/restore
lifecycle. Uses Android's Storage Access Framework (SAF) for secure, offline file selection
without requiring storage permissions.

**How to Import (Restore):**
1. Open Settings > "Backup & Restore"
2. Tap "Import Backup"
3. Select a `.json` backup file from the system file picker
4. Confirm the "Replace All" action in the confirmation dialog
5. All existing data is replaced with the backup data

**WARNING: Import will replace all existing app data on this device.** This includes all transactions
and categories. This action cannot be undone. We recommend exporting a backup before importing.

**Replace All Behavior:**
- All existing transactions are deleted
- All existing categories are deleted
- Categories from the backup file are inserted
- Transactions from the backup file are inserted
- Default currency preference is restored from the backup (if supported)
- All operations run in a single database transaction (atomic)
- If any error occurs, the entire operation is rolled back and existing data is preserved

**Supported Backup Format:**
- Schema version: 1 (`BackupDocumentV1`)
- File format: JSON (`application/json`)
- Encoding: UTF-8

**Error Handling:**
- Invalid or corrupted backup files show a user-friendly error message
- Unsupported schema versions are rejected
- Validation errors (orphaned transactions, invalid data) are caught before any data modification
- Original data remains intact on any error

**Offline-Only:** The import is entirely offline. The app does not upload your data. You choose
the backup file from local storage or a cloud-synced folder via the system file picker. The app
itself makes no network calls.

**Key Components:**

| Component | File | Role |
|-----------|------|------|
| `SettingsViewModel` | `ui/screen/settings/SettingsViewModel.kt` | Import orchestration with confirmation flow, `BackupOperation.Importing` state |
| `SettingsScreen` | `ui/screen/settings/SettingsScreen.kt` | Import row, SAF `OpenDocument` launcher, confirmation dialog |
| `BackupRepositoryImpl` | `data/backup/BackupRepositoryImpl.kt` | Atomic replace-all with currency preference restore |

**Test Coverage:**

| Test Class | New Tests | Coverage |
|------------|-----------|----------|
| `BackupRepositoryImplTest` | 3 | Currency preference restore (supported, unsupported, blank) |
| `SettingsViewModelTest` | 7 | Restore confirmation flow, success, errors, null stream, no-op on missing URI |

## Phase 4.1 – Data Retention Guardrails (v2.2)

### Root Cause: "Data Disappears"

Investigation confirmed that **no data is actually lost**. The database has no destructive migrations,
no `clearAllTables()` on startup, and no scheduled cleanup jobs. The perceived "data disappearance"
was caused by the UI being hardcoded to query only the current calendar month — when a new month
begins, all previous transactions become invisible even though they remain in the database.

### Guardrails Added

- **No `fallbackToDestructiveMigration()`** — confirmed absent; Room uses explicit migrations only.
- **Database version 3** — adds performance indices on `transactions.timestamp` and
  `transactions.category_id`. No data changes; all existing rows preserved.
- **Data retention regression tests** (`DataRetentionTest`) — verify that:
  - Consecutive months have no gaps and no overlaps
  - A full year of months covers exactly 365/366 days
  - Transactions at exact month boundaries belong to the correct month
  - Year-boundary navigation (Dec→Jan) is seamless

## Phase 4.2 – Time Range System + Month Navigation (v2.3)

### Multi-Month Tracking

Users can now navigate between months on both the Home and Summary screens using
prev/next arrow buttons. Transactions from all months remain in the database and
are retrievable by selecting the desired month.

**How to Use:**
1. Open Home or Summary
2. The current month is shown by default (e.g., "Mar 2026")
3. Tap the left arrow to go to the previous month
4. Tap the right arrow to go to the next month
5. Both the transaction list and summary update to show the selected month's data

**Key Components:**

| Component | File | Role |
|-----------|------|------|
| `DateRange` | `domain/model/DateRange.kt` | Half-open time range `[start, end)` |
| `DateRangeCalculator` | `core/util/DateRangeCalculator.kt` | Month boundary computation with injectable `Clock`/`ZoneId` |
| `MonthSelector` | `ui/component/MonthSelector.kt` | Reusable prev/label/next composable |
| `HomeViewModel` | `ui/screen/home/HomeViewModel.kt` | Month navigation with `goToPreviousMonth()` / `goToNextMonth()` |
| `SummaryViewModel` | `ui/screen/summary/SummaryViewModel.kt` | Same month navigation pattern |

**Database Migration (v2→v3):**
- Adds index on `transactions.timestamp` for date-range query performance
- Adds index on `transactions.category_id` for foreign key cascade performance
- No data changes; existing rows fully preserved

**Test Coverage:**

| Test Class | Tests | Coverage |
|------------|-------|----------|
| `DateRangeCalculatorTest` | 17 | Month boundaries, leap years, year crossings, timezones, round-trips |
| `DataRetentionTest` | 8 | No-gap coverage, boundary correctness, year totals |
| `HomeViewModelTest` | 16 | Month navigation, year boundary crossings, label display |
| `SummaryViewModelTest` | 12 | Month navigation, year boundary crossings, label display |

## Phase 4.3 – Shared Month/Year Navigation + Picker (v2.4)

### Synchronized Month Selection

Home and Summary now share a single selected-month state. Changing the month on
one screen automatically updates the other — no more independent navigation.

**New: Month/Year Picker Dialog**
Tapping the month label (e.g., "Mar 2026") opens a picker dialog with a 4x3
month grid and year stepper, allowing users to jump directly to any month/year.

**How to Use:**
1. Tap the left/right arrows to step one month at a time (same as Phase 4.2)
2. **New:** Tap the month label to open the month/year picker
3. In the picker, use the year arrows to change year, then tap a month to select
4. Switch tabs — the selected month is shared between Home and Summary

**Data Retention:** Changing months is purely a view filter. No data is created
or deleted. Multi-currency summaries remain per-currency (Phase 2.3 preserved).

**Key Components:**

| Component | File | Role |
|-----------|------|------|
| `SelectedMonthRepository` | `domain/repository/SelectedMonthRepository.kt` | Shared month state interface |
| `SelectedMonthRepositoryImpl` | `data/preferences/SelectedMonthRepositoryImpl.kt` | `@Singleton` in-memory `StateFlow<YearMonth>` |
| `MonthYearPickerDialog` | `ui/component/MonthYearPickerDialog.kt` | Material 3 month/year picker dialog |
| `MonthSelector` | `ui/component/MonthSelector.kt` | Updated with `onMonthLabelClick` callback |

**Architecture:**
- `SelectedMonthRepository` is a `@Singleton` scoped in-memory `StateFlow<YearMonth>`
- Both ViewModels inject and observe the same instance via Hilt
- On cold start, the month resets to "current month" (no DataStore persistence needed)
- The picker dialog uses `rememberSaveable` for year state across config changes

**Test Coverage:**

| Test Class | Tests | Coverage |
|------------|-------|----------|
| `SelectedMonthRepositoryImplTest` | 8 | Default month, set/nav, year boundaries, accumulation |
| `HomeViewModelTest` | 19 | Shared month, setMonth, external change, picker integration |
| `SummaryViewModelTest` | 16 | Shared month, setMonth, external change, cross-VM consistency |

## Phase 4.4 – Year View + Search (v2.5)

### Year Summary

The Summary screen now supports a Year mode in addition to the existing Month mode.
A toggle (Month / Year FilterChips) lets the user switch between per-month and per-year
aggregation. Year mode reuses the same per-currency summary pipeline — it simply passes
full-year boundaries (Jan 1 to Jan 1 next year) to `observeMonthlySummary`.

**How to Use:**
1. Open Summary
2. Tap the "Year" chip to switch to yearly view
3. Use left/right arrows to navigate between years
4. Tap "Month" to return to per-month view

**Behavior:**
- Year mode shows per-currency Income/Expenses/Balance cards and Top 5+Other categories
  for the entire selected year
- No currency conversion or cross-currency aggregation (Phase 2.3 rules preserved)
- Policy-safe disclaimer remains visible
- Switching from Year back to Month restores the previously selected month
- When entering Year mode, the year is initialized from the current selected month's year

### Search & Filters

The Home screen now includes a search bar for finding transactions by note text.

**How to Use:**
1. Open Home
2. Type in the "Search by note" field above the filter chips
3. Results update after 300ms debounce
4. Tap the clear (×) button to reset the search
5. Search combines with the existing type filter and month selection

**Behavior:**
- Case-insensitive substring match on the transaction `note` field (SQL `LIKE`)
- Works alongside the All/Expenses/Income filter chips
- Scoped to the currently selected month
- Empty search query shows all transactions (normal mode)
- "No results found" empty state when search yields zero matches

**Key Components:**

| Component | File | Role |
|-----------|------|------|
| `DateRangeCalculator` | `core/util/DateRangeCalculator.kt` | Added `currentYear()` and `rangeOfYear(year)` |
| `TransactionDao` | `data/database/dao/TransactionDao.kt` | Added `searchTransactions()` query |
| `TransactionRepository` | `domain/repository/TransactionRepository.kt` | Added `searchTransactions()` interface method |
| `TransactionRepositoryImpl` | `repository/TransactionRepositoryImpl.kt` | Added `searchTransactions()` implementation |
| `SummaryViewModel` | `ui/screen/summary/SummaryViewModel.kt` | `SummaryMode` enum, year navigation, mode switching |
| `SummaryScreen` | `ui/screen/summary/SummaryScreen.kt` | Month/Year toggle chips, period selector |
| `HomeViewModel` | `ui/screen/home/HomeViewModel.kt` | Debounced search query, `searchTransactions` integration |
| `HomeScreen` | `ui/screen/home/HomeScreen.kt` | `SearchBar` composable, "No results found" empty state |

**Test Coverage:**

| Test Class | Tests Added | Coverage |
|------------|-------------|----------|
| `DateRangeCalculatorTest` | 5 | Year range boundaries, leap year, timezone |
| `HomeViewModelTest` | 4 | Search query, debounce, clear, empty results |
| `SummaryViewModelTest` | 7 | Year mode, year navigation, mode switching, external month isolation |

## Phase 4.5 – Safe Upgrade Guarantee (v2.6)

### No Destructive Migration Policy

The app guarantees that upgrading from any previously shipped version **never wipes user data**.
Room uses explicit, additive migrations only. There is no `fallbackToDestructiveMigration()`,
no `clearAllTables()` on startup, and no automatic deletion of old transactions.

**Policy Rules:**
- Every schema change MUST have a corresponding `Migration` object
- Every migration MUST have an instrumented test
- `fallbackToDestructiveMigration()` is NEVER used in release builds
- Category seeding only runs when the categories table is empty (dual-guard)

### DB Versioning

| DB Version | App Version | Migration | Schema Change |
|:---:|:---:|---|---|
| 1 | v1.0.0 | — (initial) | `transactions` + `categories` tables |
| 2 | v1.2.0 | `MIGRATION_1_2` | `ALTER TABLE transactions ADD COLUMN currency_code TEXT NOT NULL DEFAULT 'VND'` |
| 3 | v2.3.0 | `MIGRATION_2_3` | `CREATE INDEX` on `transactions(timestamp)` and `transactions(category_id)` |

**Migration Graph:**
```
v1 ──MIGRATION_1_2──▶ v2 ──MIGRATION_2_3──▶ v3 (current)
│                                            │
└────────────── chained migration ───────────┘
```

All migrations are additive (column additions, index creation). No data is deleted,
moved, or transformed during any migration step.

### Schema Export

Starting with v3, Room schema JSON files are exported to `app/schemas/` and committed
to version control. This enables:
- Compile-time schema validation (Room verifies entity annotations match the expected schema)
- `MigrationTestHelper` support for future migrations (v3+)
- Schema change visibility in pull request diffs

### Category Seeding Safety

Default categories are seeded on first launch using a dual-guard mechanism:
1. **Fast path**: DataStore `seed_complete` flag (avoids DB query on normal launches)
2. **Safety net**: `SELECT COUNT(*) FROM categories` (catches DataStore/DB desync)

Categories are only inserted when the table is **actually empty**. This prevents:
- Duplicate seeding after backup restore
- Re-seeding after DataStore reset on upgrade
- Category wipe if seeding runs unexpectedly

### Migration Tests

Instrumented migration tests verify all upgrade paths:

| Test | Start → End | Validates |
|------|:-----------:|-----------|
| `migration1To2_addsColumnWithDefaultVND` | v1 → v2 | currency_code column added with 'VND' default |
| `migration1To2_preservesExistingData` | v1 → v2 | All rows survive intact |
| `migration1To2_preservesCategoryData` | v1 → v2 | Category data untouched |
| `migration2To3_addsIndices` | v2 → v3 | Both indices created |
| `migration2To3_preservesTransactionData` | v2 → v3 | Transaction data preserved |
| `migration2To3_preservesCategoryData` | v2 → v3 | Category data preserved |
| `migration2To3_canQueryWithRoomAfterMigration` | v2 → v3 | Room DAO works post-migration |
| `migration1To3_chained_preservesAllTransactionData` | v1 → v3 | Full chain: transaction data preserved |
| `migration1To3_chained_preservesAllCategoryData` | v1 → v3 | Full chain: category data preserved |
| `migration1To3_chained_hasIndices` | v1 → v3 | Full chain: indices exist |
| `migration1To3_chained_hasCurrencyCodeColumn` | v1 → v3 | Full chain: currency_code column exists |
| `migration1To3_chained_canQueryWithRoomDao` | v1 → v3 | Full chain: Room DAOs functional |

**Running migration tests:**
```bash
# Instrumented tests (requires emulator or device)
./gradlew connectedAndroidTest

# Unit tests (data retention + seed safety)
./gradlew test
```

### Release Safety Checklist

For every release that changes the database schema:
- [ ] Increment `version` in `@Database` annotation
- [ ] Add a `Migration` object for the new version step
- [ ] Register the migration in `DatabaseModule.provideAppDatabase()`
- [ ] Add instrumented migration test(s) for the new version step
- [ ] Run `./gradlew connectedAndroidTest` and verify all migration tests pass
- [ ] Verify exported schema JSON is updated in `app/schemas/`
- [ ] **Never** add `fallbackToDestructiveMigration()` in release builds

**Modified Files:**

| File | Change |
|------|--------|
| `data/database/AppDatabase.kt` | `exportSchema = true` for schema validation |
| `data/database/dao/CategoryDao.kt` | Added `count()` query for seeding safety |
| `data/seed/SeedRepository.kt` | Dual-guard seeding (DataStore flag + table emptiness) |
| `data/database/migration/MigrationTest.kt` | Expanded from 3 to 12 instrumented tests |
| `data/DataRetentionTest.kt` | Added `migrationChain_coversAllVersions` test |
| `app/build.gradle.kts` | Added `room-testing` dependency, KSP schema export arg |
| `gradle/libs.versions.toml` | Added `room-testing` library entry |

**New Files:**

| File | Purpose |
|------|---------|
| `data/seed/SeedRepositoryTest.kt` | Unit tests for seeding safety logic |
| `app/schemas/` | Room schema JSON exports (v3+) |

**Test Coverage:**

| Test Class | Tests | Coverage |
|------------|-------|----------|
| `MigrationTest` (androidTest) | 12 | All migration paths: v1→v2, v2→v3, v1→v3 chain |
| `DataRetentionTest` | 9 | Date ranges, migration chain completeness |
| `SeedRepositoryTest` | 4 | Seeding safety, category type stability |

## Project Structure

```
app/src/main/java/dev/tuandoan/expensetracker/
├── core/                           # Core utilities
│   ├── formatter/                  # Amount formatting utilities
│   └── util/                       # Date/time utilities
├── data/                          # Data layer
│   ├── backup/                    # Backup/restore data format
│   │   ├── model/                 # BackupDocumentV1, DTOs
│   │   ├── mapper/                # Entity <-> DTO mappers
│   │   ├── BackupRepositoryImpl.kt # Export/import with atomic transactions
│   │   ├── BackupSerializer.kt    # JSON encode/decode
│   │   └── BackupValidator.kt     # Structural validation
│   ├── database/                   # Room database
│   │   ├── dao/                   # Data Access Objects
│   │   ├── entity/                # Database entities
│   │   └── TransactionRunner.kt   # Room transaction abstraction
│   ├── preferences/                # DataStore preferences
│   │   ├── CurrencyPreferenceRepositoryImpl.kt
│   │   └── SelectedMonthRepositoryImpl.kt
│   └── seed/                      # Database seeding
├── di/                            # Dependency injection modules
├── domain/                        # Domain layer
│   ├── model/                     # Domain models
│   └── repository/                # Repository interfaces
├── repository/                    # Repository implementations
│   └── mapper/                    # Entity-Domain mappers
└── ui/                           # UI layer
    ├── component/                 # Reusable UI components
    │   ├── CommonComponents.kt    # AmountText, EmptyStateMessage, etc.
    │   ├── MonthSelector.kt       # Month prev/next selector with label tap
    │   └── MonthYearPickerDialog.kt # Month/year picker dialog
    ├── navigation/                # Navigation setup
    ├── screen/                    # Compose screens
    │   ├── home/                  # Home screen (transaction list)
    │   ├── summary/               # Monthly summary screen
    │   ├── addedit/              # Add/edit transaction screen
    │   └── settings/              # Settings screen
    └── theme/                     # App theme and styling
        ├── DesignSystem.kt        # Design system constants & semantic colors
        ├── Theme.kt               # Material 3 theme configuration
        ├── Color.kt               # Color schemes
        └── Type.kt                # Typography definitions
```

## Database Schema

### Transactions
- `id`: Primary key (auto-generated)
- `type`: Transaction type (0=EXPENSE, 1=INCOME)
- `amount`: Amount stored as Long (no decimals)
- `currency_code`: ISO 4217 currency code (default: `VND`)
- `category_id`: Foreign key to categories table
- `note`: Optional note
- `timestamp`: Transaction date (epoch millis)
- `created_at`, `updated_at`: Tracking timestamps

### Categories
- `id`: Primary key (auto-generated)
- `name`: Category name
- `type`: Category type (0=EXPENSE, 1=INCOME)
- `icon_key`, `color_key`: Optional styling (for future use)
- `is_default`: Whether it's a default seeded category

## Setup & Installation

### Prerequisites
- Android Studio Arctic Fox (2020.3.1) or later
- Android SDK 26 or later
- Kotlin 1.9.0 or later

### Building the App

1. **Clone the repository**
   ```bash
   git clone <your-repository-url>
   cd expense-tracker
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the project directory and select it

3. **Sync dependencies**
   - Android Studio will automatically prompt to sync Gradle
   - Or manually sync by clicking "Sync Now" or using `./gradlew build`

4. **Run the app**
   - Connect an Android device or start an emulator (API 26+)
   - Click the "Run" button or use `Ctrl+F9` (Windows/Linux) / `Cmd+R` (macOS)

### Building APK

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (unsigned)
./gradlew assembleRelease
```

The generated APK will be in `app/build/outputs/apk/`

### Code Formatting

This project uses [Spotless](https://github.com/diffplug/spotless) with ktlint for consistent code formatting.

#### Check code formatting
```bash
./gradlew spotlessCheck
```

#### Apply automatic formatting
```bash
./gradlew spotlessApply
```

#### Configuration
- **ktlint**: Version 1.4.0 for Kotlin code formatting
- **EditorConfig**: Located at `.editorconfig` with project-specific rules
- **Rules**: Configured for Android/Compose development with disabled function-naming and no-wildcard-imports rules
- **CI Integration**: `spotlessCheck` is automatically run with `./gradlew check`

**Note**: Always run `spotlessApply` before committing to ensure consistent formatting across the codebase.

### Running Tests

```bash
# Unit tests (including core business logic tests)
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

**Core Unit Tests Coverage:**
- `CurrencyFormatterTest`: Comprehensive multi-currency formatting for all 6 currencies + unknown fallback + edge cases
- `AmountFormatterTest`: VND-default facade tests, parsing, and backward compatibility
- `DateTimeUtilTest`: Date range calculations, formatting, and time utilities
- `FakeCurrencyPreferenceRepositoryTest`: Default currency preference storage, validation, and observation
- `SettingsViewModelTest`: Settings screen state management, currency selection, and error handling
- `AddEditTransactionViewModelTest`: Transaction CRUD operations including currency preference integration (3 new currency tests)
- Critical business logic for currency handling, preferences, and date operations

## Usage

### First Launch
- The app will automatically seed default categories on first run
- **Expense categories**: Food, Transport, Shopping, Bills, Health, Entertainment, Other
- **Income categories**: Salary, Bonus, Gift, Other

### Adding Transactions
1. Tap the "+" floating action button on the Home screen
2. Select transaction type (Expense or Income)
3. Enter the amount (numbers only, no currency symbol)
4. Choose a category from the dropdown
5. Select date (defaults to today)
6. Add an optional note
7. Tap "Save Transaction"

### Viewing Summary
- Navigate to the "Summary" tab to see monthly overview
- View total income, expenses, and balance
- See top expense categories with amounts

### Managing Transactions
- Tap any transaction in the list to edit it
- Tap the delete button to remove a transaction
- Use filter chips to view All, Expenses, or Income only

## Privacy & Data

- **Local Storage**: All data is stored locally in a SQLite database
- **No Internet Access**: The app does not require or request internet permissions
- **No Data Collection**: No personal information is collected or transmitted
- **No Analytics**: No usage tracking or analytics
- **No Ads**: Clean, ad-free experience
- **Data Retention**: Data persists until the app is uninstalled

## Permissions

This app requires minimal permissions:
- **No sensitive permissions**: No access to SMS, contacts, location, camera, etc.
- **Storage**: Only internal app storage for the local database

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push to the branch (`git push origin feature/new-feature`)
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support or questions, please contact: support@expensetracker.com

## Version History

- **v2.6.0** - Phase 4.5: Safe Upgrade Guarantee (exportSchema=true, room-testing dependency, 12 instrumented migration tests covering v1→v2→v3 chain, dual-guard category seeding, no destructive migration policy, release safety checklist)
- **v2.5.0** - Phase 4.4: Year View + Search (Month/Year toggle on Summary, year-range aggregation, search bar on Home with 300ms debounce + SQL LIKE, combined with type filter + month scope, 16 new unit tests)
- **v2.4.0** - Phase 4.3: Shared Month/Year Navigation + Picker (SelectedMonthRepository singleton for synchronized Home + Summary month state, MonthYearPickerDialog with 4x3 month grid + year stepper, tap-on-label to pick, 43 unit tests including cross-VM consistency)
- **v2.3.0** - Phase 4.2: Time Range System + Month Navigation (DateRange model, DateRangeCalculator with injectable Clock/ZoneId, MonthSelector composable, prev/next month navigation on Home + Summary, timestamp + category_id indices, Room migration v2→v3, 43 new/updated unit tests)
- **v2.2.0** - Phase 4.1: Data Retention Guardrails (root cause analysis – no data loss, only current-month visibility issue; 8 regression tests proving no gaps/overlaps in date ranges; confirmed no destructive migration)
- **v2.1.0** - Phase 3.3: Import / Restore (Replace All) -- SAF OpenDocument file picker, confirmation dialog for destructive replace-all, atomic import with currency preference restore, 10 new unit tests
- **v1.6.0** - Phase 3.2: Export Backup (Offline) -- BackupAssembler for deterministic export, defaultCurrencyCode + deviceLocale in BackupDocumentV1, Settings "Backup & Restore" section with SAF export, @IoDispatcher threading, 56 unit tests
- **v1.5.0** - Phase 3.1: Backup Schema v1 (BackupDocumentV1 DTOs, kotlinx-serialization, BackupValidator, entity-DTO mappers, BackupRepository, ProGuard rules, 48 unit tests)
- **v1.4.0** - Phase 2.3: Monthly Summary per currency (per-currency sections on Summary screen, top-5 + Other aggregation, registry-ordered currency sorting, policy-safe disclaimer for all non-empty months)
- **v1.3.0** - Phase 2.2: App-level default currency setting, per-transaction currency picker, home list currency visibility (Settings → Currency selector, DataStore persistence, inline currency override on Add/Edit screen, per-transaction symbol display on Home list)
- **v1.2.0** - Phase 2.1: Multi-currency data foundation (`currency_code` field, Room migration v1->v2, static currency definitions)
- **v1.0.0** - Initial MVP release with core transaction management features
