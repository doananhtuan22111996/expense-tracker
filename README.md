# Expense Tracker

A simple, offline-first personal expense tracker Android app for managing your income and expenses locally on your device.

## Features

- **Offline-First**: All data is stored locally on your device
- **No Account Required**: No login, registration, or cloud sync
- **Privacy-Focused**: No data collection, analytics, or ads
- **Simple & Clean**: Material 3 design with intuitive navigation
- **Transaction Management**: Add, edit, delete income and expense transactions
- **Category Organization**: Pre-defined categories for expenses and income
- **Monthly Summary**: View your financial overview with balance and top expense categories
- **Filter & Search**: Filter transactions by type (All, Expenses, Income)

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

**💰 VND-Only Policy**: The app is designed exclusively for Vietnamese Dong (₫)
- **Integer amounts only**: No decimal places (VND doesn't use fractional currency)
- **Thousand separators**: Uses locale-aware comma formatting (1,234,567)
- **Consistent formatting**: Single `AmountFormatter` utility used throughout
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

## Project Structure

```
app/src/main/java/dev/tuandoan/expensetracker/
├── core/                           # Core utilities
│   ├── formatter/                  # Amount formatting utilities
│   └── util/                       # Date/time utilities
├── data/                          # Data layer
│   ├── database/                   # Room database
│   │   ├── dao/                   # Data Access Objects
│   │   └── entity/                # Database entities
│   └── seed/                      # Database seeding
├── di/                            # Dependency injection modules
├── domain/                        # Domain layer
│   ├── model/                     # Domain models
│   └── repository/                # Repository interfaces
├── repository/                    # Repository implementations
│   └── mapper/                    # Entity-Domain mappers
└── ui/                           # UI layer
    ├── component/                 # Reusable UI components
    │   └── CommonComponents.kt    # AmountText, EmptyStateMessage, etc.
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
- `AmountFormatterTest`: Comprehensive VND formatting, parsing, and edge cases
- `DateTimeUtilTest`: Date range calculations, formatting, and time utilities
- Critical business logic for currency handling and date operations

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

- **v1.2.0** - Phase 2.1: Multi-currency data foundation (`currency_code` field, Room migration v1->v2)
- **v1.0.0** - Initial MVP release with core transaction management features
