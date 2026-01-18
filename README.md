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
    ├── navigation/                # Navigation setup
    ├── screen/                    # Compose screens
    │   ├── home/                  # Home screen (transaction list)
    │   ├── summary/               # Monthly summary screen
    │   ├── addedit/              # Add/edit transaction screen
    │   └── settings/              # Settings screen
    └── theme/                     # App theme and styling
```

## Database Schema

### Transactions
- `id`: Primary key (auto-generated)
- `type`: Transaction type (0=EXPENSE, 1=INCOME)
- `amount`: Amount in VND format (no decimals)
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
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

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

- **v1.0.0** - Initial MVP release with core transaction management features
