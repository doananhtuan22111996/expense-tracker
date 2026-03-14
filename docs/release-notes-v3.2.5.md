# Expense Tracker v3.2.5 Release Notes

## Highlights

Version 3.2.5 completes Phase 6 of the Expense Tracker project, delivering five key
improvements based on tester feedback: full dark mode support, a first-launch onboarding
walkthrough, WCAG AA accessibility enhancements, in-app feedback with privacy-safe crash
reporting, and an optimised Play Store listing in English and Vietnamese.

## New Features

### Dark Mode (v3.2.1)
- Users can choose Light, Dark, or System theme from Settings.
- Theme preference is persisted in DataStore and applied immediately without restart.
- All screens audited to use MaterialTheme.colorScheme tokens instead of hardcoded colors.

### Onboarding Walkthrough (v3.2.2)
- 4-step horizontal pager shown on first launch only.
- Covers: Welcome, Transactions, Budgets, and Backup.
- Skippable from any page; "Get Started" on the final page navigates to Home.
- Backed by a DataStore boolean flag -- never shown again after completion.

### Accessibility -- WCAG AA (v3.2.3)
- DonutChart: semantic content description listing all categories with amounts.
- MonthlyBarChart: per-bar semantics with month name, amount, and tap action.
- Income/Expense indicators: directional arrow icons alongside color for non-color distinction.
- All IconButtons now have content descriptions for TalkBack.
- Minimum 48dp touch targets enforced across all interactive elements.
- AddEditTransactionScreen: logical focus order (Amount, Category, Note, Date, Save).
- Category color swatches labeled with localized color names.

### Feedback & Crash Reporting (v3.2.4)
- In-app review prompt triggered after the 5th transaction (with cooldown and cap).
- Feedback bottom sheet in Settings: positive path triggers review; negative path opens email.
- Firebase Crashlytics abstracted behind CrashReporter interface (no-op by default).
- Analytics consent toggle in Settings -- crash collection disabled until user opts in.
- No amounts, category names, or PII included in any crash report or log.

### ASO & Play Store Listing (v3.2.5)
- ScreenshotSeedActivity (debug only) for generating demo data screenshots.
- Fastlane metadata directory with EN and VI store listings.
- Optimised short descriptions (80 chars), structured long descriptions, and changelogs.

## Bug Fixes

No bug fixes in this release. Phase 6 focused exclusively on new features and improvements.

## Known Limitations

- **Firebase Crashlytics** requires a valid `google-services.json` file. The current
  implementation uses a no-op CrashReporter by default. To enable Crashlytics, add the
  Firebase configuration file and swap the CrashReporter binding.
- **In-App Review API** requires the app to be distributed through the Google Play Store.
  The review flow will not appear in debug builds or sideloaded APKs. Use
  `FakeReviewManager` for testing.
- **ScreenshotSeedActivity** is only available in debug builds and is not included in
  release APKs.
