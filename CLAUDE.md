# Expense Tracker – Phase 6: Tester Feedback & Improvements

## Project Context

- **Package:** `dev.tuandoan.expensetracker`
- **Language:** Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, WorkManager
- **Current version:** `3.1.5` (set in `app/build.gradle.kts`)
- **Architecture:** MVVM — UI → ViewModel → Repository → DAO/DataStore
- **DB version:** 5 (Room, schema exports in `app/schemas/`)
- **Test framework:** JUnit4, Mockito-Kotlin, Turbine, kotlinx-coroutines-test
- **Notion Task Board:** collection `402a4990-8e31-4562-a551-58c728291c53`
- **Notion Sprint Spec:** https://www.notion.so/323b17725413816d8e54e9c1cc61a44a

## Sprint Goal

Implement all 6 improvements derived from tester feedback: Dark Mode, Onboarding,
Accessibility (WCAG AA), In-App Feedback + Crashlytics, and ASO / Store Listing.
Deliver a polished, store-ready build at v3.2.5. Work session by session, in order.

## Execution Rules

1. **Compile-check after every session:** `./gradlew :app:compileDebugKotlin`
2. **Unit tests after every session:** `./gradlew :app:testDebugUnitTest`
3. **Formatting before each commit:** `./gradlew spotlessApply`
4. **Do not break existing tests.** If a test fails that you didn't write, fix it first.
5. **Follow existing patterns exactly.** Match StateFlow patterns from `HomeViewModel`,
   `SettingsViewModel`, and `BackupRepositoryImpl`.
6. **No hardcoded strings** — add all new copy to `res/values/strings.xml`.
7. **No hardcoded colors** — use `MaterialTheme.colorScheme.*` tokens only.
8. **Privacy first** — never log transaction amounts, category names, or PII anywhere.

---

## Workflow Execution Protocol

Every session MUST follow this full workflow before marking it complete.
The **orchestrator** subagent drives each session end-to-end. Do not skip any step.

### Step 0 — Branch Creation (before touching any code)

Branch naming: `phase-6/session-<N>-<short-slug>`

```
phase-6/session-1-dark-mode
phase-6/session-2-onboarding
phase-6/session-3-accessibility
phase-6/session-4-feedback-crashlytics
phase-6/session-5-aso-screenshots
git push -u origin <branch-name>
```

### Step 1 — Pre-Session Architecture Review

Invoke **mobile-architect · opus** to:
- Review session features against MVVM architecture.
- Flag deviations from `HomeViewModel`, `SettingsViewModel`, `BackupRepositoryImpl` patterns.
- Approve or suggest alternatives before implementation begins.
- Use `engineering:system-design` skill if session introduces new service boundaries.

### Step 2 — UX/Design Sign-Off (all sessions)

Invoke **ui-ux-designer · sonnet** to:
- Run `design:critique` skill on proposed UX changes.
- Run `design:ux-copy` skill before adding any string to `strings.xml`.
- Run `design:accessibility` skill after any new Composable is written (WCAG AA).
- Run `design:design-system` skill when adding new color/token usage.

Invoke **ux-researcher · sonnet** (Sessions 2 & 3 only) to:
- Session 2: Validate onboarding flow friction against user mental models.
- Session 3: Validate TalkBack navigation paths for chart components.
- Use `design:research-synthesis` skill to document findings.

### Step 3 — Implementation

Invoke **android-engineer · opus** to implement all features for the session.
The android-engineer MUST:
- Run `engineering:code-review` skill (self-review) before committing each file.
- Run `engineering:documentation` skill to add KDoc to all new public APIs.
- Follow all Execution Rules (no hardcoded strings/colors, match existing patterns).
- Run `./gradlew spotlessApply && ./gradlew :app:compileDebugKotlin` and fix errors.

### Step 4 — Security & Privacy Review

Invoke **security-privacy · opus** to:
- Session 1: Verify ThemePreference DataStore does not leak system-level info.
- Session 4: Verify no PII (amounts, categories) in Crashlytics or email feedback body.
- Session 4: Verify Firebase Crashlytics collection gated behind consent flag.
- All sessions: Check for data leakage in new WorkManager inputData / logs.
- Use `engineering:code-review` skill focused on security findings.
Flag issues back to android-engineer for immediate resolution before proceeding.

### Step 5 — QA & Test Engineering

Invoke **qa-test-engineer · sonnet** to:
- Run `engineering:testing-strategy` skill to verify coverage meets the session spec.
- Write or complete any missing unit tests listed in the session's "Unit Tests" section.
- Run `./gradlew :app:testDebugUnitTest` — all tests MUST pass.
- Document test results summary (pass count, skipped tests with reason).

### Step 6 — Code Review

Invoke **code-reviewer · inherit** to:
- Run `engineering:code-review` skill on full session diff (`git diff main`).
- Check: correctness, performance, error handling, Kotlin idioms, Compose best practices.
- Run `engineering:tech-debt` skill to flag any shortcuts as future tech debt.
- All blocking issues must be resolved before the PR is opened.

### Step 7 — Backlog & Release Tracking

Invoke **backlog-analyst · sonnet** to:
- Mark session tasks as **Done** in Notion Task Board (collection `402a4990-8e31-4562-a551-58c728291c53`).
- Cross-check every feature is accounted for — no partial completions.

Invoke **release-manager · sonnet** to:
- Run `engineering:deploy-checklist` skill before the PR.
- Bump `versionName` in `app/build.gradle.kts` (v3.2.1 → v3.2.2 → ... → v3.2.5).
- Confirm `app/schemas/` contains updated Room schema JSON (sessions with migrations).

### Step 8 — Pull Request (GitHub MCP)

**release-manager** creates the PR via GitHub MCP tool.

PR title format: `[Phase 6] Session N — <Short Description> (v3.2.X)`

PR body MUST include:
1. **Summary** — one paragraph describing what the session adds.
2. **Changes** — table: Feature number, description, primary files changed.
3. **Test Results** — paste unit test summary from Step 5.
4. **Security Review** — one-line clearance from security-privacy subagent.
5. **Architecture Notes** — deviations from standard patterns approved in Step 1.
6. **Checklist** — deploy-checklist output from Step 7.
7. **Breaking Changes** — DB migrations, new dependencies, consent flags.
8. **Screenshots** — required for all sessions (before/after UI changes).

Base branch: `main`

### Step 9 — PRD Writer Update (Sessions 1 & 5 only)

Invoke **prd-writer · sonnet** using `engineering:documentation` skill:
- **Session 1 start:** produce Technical Implementation Summary → `docs/phase-6-technical-summary.md`
- **Session 5 end:** produce Release Notes for v3.2.5 → `docs/release-notes-v3.2.5.md`
  (highlights, new features, accessibility improvements, known limitations)

---

## Subagent Reference

| Subagent | Model | Responsibility |
|---|---|---|
| `orchestrator` | sonnet | Drives each session; coordinates all subagents |
| `android-engineer` | sonnet | Writes all Kotlin/Compose code; KDoc; spotless |
| `mobile-architect` | sonnet | Architecture review; pattern compliance sign-off |
| `ui-ux-designer` | sonnet | UX review; design system; accessibility; copy |
| `ux-researcher` | sonnet | User mental model validation (Sessions 2 & 3) |
| `security-privacy` | sonnet | Security + privacy audit of every session diff |
| `qa-test-engineer` | sonnet | Test strategy; unit test completeness; test runner |
| `code-reviewer` | sonnet | Final diff review; tech debt flags |
| `backlog-analyst` | sonnet | Notion board updates; task status tracking |
| `release-manager` | sonnet | Version bump; deploy checklist; PR creation |
| `prd-writer` | sonnet | Technical summary (Session 1); release notes (Session 5) |

## Skills Reference

| Skill | Invoked By | When |
|---|---|---|
| `engineering:code-review` | android-engineer, code-reviewer, security-privacy | Self-review per file; final PR diff; security audit |
| `engineering:documentation` | android-engineer, prd-writer | KDoc on public APIs; tech summary; release notes |
| `engineering:testing-strategy` | qa-test-engineer | Before writing tests each session |
| `engineering:tech-debt` | code-reviewer | Final review pass — flag shortcuts |
| `engineering:deploy-checklist` | release-manager | Before every PR |
| `engineering:system-design` | mobile-architect | If session introduces new service boundaries |
| `design:ux-copy` | ui-ux-designer | Before adding any string to strings.xml |
| `design:accessibility` | ui-ux-designer | After any new Composable is written |
| `design:design-system` | ui-ux-designer | When adding new color/token usage |
| `design:critique` | ui-ux-designer | All sessions — review final UI output |
| `design:research-synthesis` | ux-researcher | Sessions 2 & 3 — validate UX decisions |

---

## SESSION 1 — Dark Mode & Theme Support (v3.2.1)

**Scope:** Let users choose Light / Dark / System theme. Persist preference in DataStore.
Wire it through `MainActivity` so the entire app responds immediately.

### Feature 1 — ThemePreference DataStore

New files:
- `data/preferences/ThemePreference.kt`: `enum class ThemePreference { LIGHT, DARK, SYSTEM }`
- `data/preferences/ThemePreferencesRepository.kt`: interface — `themePreference: Flow<ThemePreference>`, `suspend fun setTheme(pref: ThemePreference)`
- `data/preferences/ThemePreferencesRepositoryImpl.kt`: DataStore-backed impl using `intPreferencesKey("theme_preference")`.
- `di/ThemeModule.kt`: `@Binds` `ThemePreferencesRepository` → `ThemePreferencesRepositoryImpl`.

### Feature 2 — MainActivity theme wiring

- `MainActivity.kt`: inject `ThemePreferencesRepository`; collect `themePreference` as `State` via `collectAsStateWithLifecycle()`.
- Pass collected value to `ExpenseTrackerTheme(darkTheme = ...)` call:
  - `LIGHT` → `darkTheme = false`
  - `DARK` → `darkTheme = true`
  - `SYSTEM` → `darkTheme = isSystemInDarkTheme()`

### Feature 3 — Theme picker in Settings

- `SettingsViewModel.kt`: inject `ThemePreferencesRepository`; expose `themePreference: StateFlow<ThemePreference>` and `fun setTheme(pref: ThemePreference)`.
- `SettingsScreen.kt`: add Theme section with `SingleChoiceSegmentedButtonRow` — three segments: "Light", "Dark", "System". Selecting a segment calls `viewModel.setTheme(pref)`.
- Strings: `settings_theme_title`, `settings_theme_light`, `settings_theme_dark`, `settings_theme_system`.

### Feature 4 — Dark mode audit

- Audit all existing Composables: replace any hardcoded `Color(0xFF...)` with `MaterialTheme.colorScheme.*` tokens.
- `DonutChart` and `MonthlyBarChart`: confirm all colors resolve via `DesignSystem.categoryColor()` or passed-in colorScheme values — no hardcoded colors inside Canvas lambdas.
- Test on Pixel emulator: toggle Light ↔ Dark in Settings — all screens must re-render without requiring restart.

### Session 1 Unit Tests

- `ThemePreferencesRepositoryImplTest`: `setTheme(DARK)` persists; `themePreference` emits `DARK` on collect.
- `SettingsViewModelTest`: `setTheme(LIGHT)` delegates to repository; `themePreference` StateFlow reflects update.

**Quality gate:** `./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest`

---

## SESSION 2 — Onboarding Walkthrough (v3.2.2)

**Scope:** 4-step onboarding shown only on first launch. Skippable, backed by DataStore flag.

### Feature 5 — OnboardingPreferences DataStore

- `data/preferences/OnboardingPreferences.kt`: `booleanPreferencesKey("onboarding_complete")`.
- `data/preferences/OnboardingRepository.kt` + `OnboardingRepositoryImpl.kt`: `isOnboardingComplete: Flow<Boolean>`, `suspend fun markOnboardingComplete()`.
- `di/OnboardingModule.kt`: `@Binds` binding.

### Feature 6 — OnboardingScreen composable (new file)

Path: `ui/screen/onboarding/OnboardingScreen.kt`

4-step `HorizontalPager` (Accompanist or Compose Foundation):

| Page | Icon | Title | Subtitle |
|---|---|---|---|
| 0 | `Icons.Outlined.AccountBalanceWallet` | `onboarding_title_welcome` | `onboarding_subtitle_welcome` |
| 1 | `Icons.Outlined.ReceiptLong` | `onboarding_title_transactions` | `onboarding_subtitle_transactions` |
| 2 | `Icons.Outlined.Savings` | `onboarding_title_budget` | `onboarding_subtitle_budget` |
| 3 | `Icons.Outlined.CloudUpload` | `onboarding_title_backup` | `onboarding_subtitle_backup` |

Navigation controls:
- `HorizontalPagerIndicator` dots below content.
- Page 0–2: "Next" button (right) + "Skip" text button (top-right).
- Page 3: "Get Started" filled button — calls `viewModel.completeOnboarding()`, no Skip.

### Feature 7 — OnboardingViewModel

Path: `ui/screen/onboarding/OnboardingViewModel.kt`

- `@HiltViewModel`; inject `OnboardingRepository`.
- `fun completeOnboarding()`: calls `markOnboardingComplete()` in `viewModelScope`.
- Exposes no UI state — navigation is handled by `NavHost` guard.

### Feature 8 — NavGraph onboarding guard

- `ui/navigation/ExpenseTrackerNavigation.kt`: add `OnboardingScreen` route.
- In `NavHost` start destination logic: collect `OnboardingRepository.isOnboardingComplete` as start destination:
  - `false` → start at `Onboarding` route.
  - `true` → start at `Home` route.
- `OnboardingScreen` `onComplete` callback → `navController.navigate(Home) { popUpTo(Onboarding) { inclusive = true } }`.

### Strings required

`onboarding_title_welcome`, `onboarding_subtitle_welcome`, `onboarding_title_transactions`,
`onboarding_subtitle_transactions`, `onboarding_title_budget`, `onboarding_subtitle_budget`,
`onboarding_title_backup`, `onboarding_subtitle_backup`, `onboarding_next`, `onboarding_skip`,
`onboarding_get_started`.

### Session 2 Unit Tests

- `OnboardingViewModelTest`: `completeOnboarding()` calls `markOnboardingComplete()` on repository.
- `OnboardingRepositoryImplTest`: `markOnboardingComplete()` writes `true`; subsequent `isOnboardingComplete` emits `true`.

**Quality gate:** `./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest`

---

## SESSION 3 — Accessibility Enhancements WCAG AA (v3.2.3)

**Scope:** Fix 10 WCAG 2.1 AA violations identified by TalkBack audit and Accessibility Scanner.

### Feature 9 — DonutChart accessibility semantics

- `ui/component/DonutChart.kt`: wrap Canvas in `Box` with `semantics { contentDescription = buildString { categories.forEach { "${it.name}: ${it.formattedAmount}. " } }; role = Role.Image }`.
- Legend rows: add `contentDescription` per row — `"${category.name}, ${category.formattedAmount}, ${category.percentage}%"`.
- String prefix: `a11y_donut_chart_description` (`"Expense breakdown: %1$s"`).

### Feature 10 — MonthlyBarChart per-bar semantics

- `ui/component/MonthlyBarChart.kt`: add invisible `Box` overlay per bar using `semantics { contentDescription = "${monthName}: ${formattedAmount}"; stateDescription = if selected "selected" else ""; role = Role.Button; onClick(label = ...) { onMonthTapped?.invoke(index+1); true } }`.
- Strings: `a11y_bar_chart_month` (`"%1$s: %2$s"`), `a11y_select_month` (`"View %1$s details"`).

### Feature 11 — Income/Expense icon + color indicators

- `ui/component/TransactionListItem.kt` (or equivalent): replace color-only amount text with `Row { Icon(..., contentDescription = typeLabel) + Text(amount, color = typeColor) }`.
- Expense: `Icons.Rounded.ArrowDownward`, `colorScheme.error`, `contentDescription = stringResource(R.string.a11y_expense_indicator)`.
- Income: `Icons.Rounded.ArrowUpward`, `colorScheme.primary`, `contentDescription = stringResource(R.string.a11y_income_indicator)`.

### Feature 12 — Icon-only button content descriptions

- Audit all `IconButton` usages in `CategoriesScreen`, `RecurringTransactionsScreen`, `HomeScreen`, `AddEditTransactionScreen`.
- Add `contentDescription` param to every `Icon` inside an `IconButton`:
  - Delete: `R.string.a11y_delete_transaction` / `R.string.a11y_delete_category`
  - Edit: `R.string.a11y_edit_transaction`
  - Close/Back: `R.string.a11y_navigate_back`

### Feature 13 — Minimum 48dp touch targets

- Apply `Modifier.minimumInteractiveComponentSize()` to all `IconButton`, category color swatches, and date-picker arrow buttons that are visually smaller than 48dp.
- Verify with Accessibility Scanner — no "small touch target" warnings.

### Feature 14 — Focus order in AddEditTransactionScreen

- `ui/screen/addedit/AddEditTransactionScreen.kt`: declare `FocusRequester` chain: Amount → Category → Note → Date → Save.
- Wire `Modifier.focusRequester(...)` + `Modifier.onFocusChanged { if (it.isFocused) ... }` to advance focus on IME "Next" action.
- Amount field: `ImeAction.Next` → request `categoryFocus`.
- Note field: `ImeAction.Next` → request `dateFocus`.

### Feature 15 — Category color swatch labels

- `ui/screen/categories/CategoriesScreen.kt`: color swatch `Box` — add `semantics { contentDescription = colorName }` where `colorName` is a localised string (e.g. `R.string.color_red`).
- Add color name strings: `color_red`, `color_blue`, `color_green`, `color_orange`, `color_pink`, `color_purple`, `color_yellow`, `color_teal`.

### Strings required

`a11y_donut_chart_description`, `a11y_bar_chart_month`, `a11y_select_month`,
`a11y_expense_indicator`, `a11y_income_indicator`, `a11y_delete_transaction`,
`a11y_delete_category`, `a11y_edit_transaction`, `a11y_navigate_back`,
`color_red`, `color_blue`, `color_green`, `color_orange`, `color_pink`,
`color_purple`, `color_yellow`, `color_teal`.

### Session 3 Unit Tests

- `DonutChartAccessibilityTest`: `onNodeWithContentDescription("Expense breakdown:")` is present when categories non-empty; empty state node present when empty.
- `TransactionListItemTest`: income row has `contentDescription = "Income"`; expense row has `contentDescription = "Expense"`.

**Quality gate:** `./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest`

---

## SESSION 4 — In-App Feedback & Crash Reporting (v3.2.4)

**Scope:** Google Play In-App Review API, feedback bottom sheet in Settings, Firebase Crashlytics
with privacy-safe non-fatal logging.

### Feature 16 — Review preferences DataStore

- `data/preferences/ReviewPreferencesImpl.kt`: DataStore keys — `longPreferencesKey("last_review_prompt_millis")`, `intPreferencesKey("review_shown_count")`.
- Interface `ReviewPreferences`: `lastReviewPromptMillis: Flow<Long>`, `reviewShownCount: Flow<Int>`, `suspend fun markReviewShown()`.

### Feature 17 — InAppReviewManager

New file: `domain/review/InAppReviewManager.kt`

```
interface InAppReviewManager {
    suspend fun isEligibleForReview(): Boolean  // install >= 7d, transactions >= 5, cooldown >= 30d, shown < 2
    suspend fun requestReview(activity: Activity)
    suspend fun markReviewShown()
}
```

Impl `InAppReviewManagerImpl`: uses `com.google.android.play:review:2.0.1`. Eligibility check reads `ReviewPreferences` + `TransactionDao.getCount()`. On trigger: call `ReviewManager.requestReviewFlow()` then `launchReviewFlow()`. On completion: `markReviewShown()`.

Dependency: `gradle/libs.versions.toml` — `playReview = "2.0.1"`.
`app/build.gradle.kts` — `implementation(libs.play.review)`.

### Feature 18 — HomeScreen review trigger

- `HomeViewModel.kt`: inject `InAppReviewManager`; observe `transactionCount`; when count crosses 5 threshold and `isEligibleForReview()` — set `showReviewRequest = true` in UiState.
- `HomeScreen.kt`: `LaunchedEffect(showReviewRequest)` — if true, call `reviewManager.requestReview(activity)`.

### Feature 19 — FeedbackBottomSheet + Settings row

New file: `ui/screen/settings/FeedbackBottomSheet.kt`

- `ModalBottomSheet` with title `settings_send_feedback`.
- Two `OutlinedButton` rows: 👍 `feedback_loving_it` and 👎 `feedback_found_issue`.
- 👍 path: dismiss sheet → trigger `viewModel.requestReview(activity)`.
- 👎 path: show `OutlinedTextField` (`feedback_describe_issue`) + `Button("Send")` → launch `Intent.ACTION_SENDTO` email intent to `support@tuandoan.dev` with pre-filled subject + body (app version + device info — NO user data).
- `SettingsScreen.kt`: add `ListItem` row "Send Feedback"; `onClick` → `showFeedbackSheet = true`.
- Strings: `settings_send_feedback`, `feedback_loving_it`, `feedback_found_issue`, `feedback_describe_issue`, `feedback_send`, `feedback_cancel`, `feedback_thanks`, `feedback_email_subject`, `feedback_email_body_prefix`.

### Feature 20 — Firebase Crashlytics (privacy-safe)

Dependencies: `firebase-crashlytics-ktx` (BOM-managed).
`app/build.gradle.kts`: apply `com.google.firebase.crashlytics` plugin + `implementation(platform(libs.firebase.bom))` + `implementation(libs.firebase.crashlytics)`.

- `data/preferences/AnalyticsPreferences.kt`: `booleanPreferencesKey("analytics_consent")` — default `false`.
- `ExpenseTrackerApplication.kt`: on launch, read `analyticsConsent`; call `FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(consent)`.
- `SettingsScreen.kt`: add "Share anonymous crash reports" `Switch` row wired to `analyticsConsent`.
- Non-fatal logging: wrap `RecurringTransactionWorker.doWork()`, `BackupRepositoryImpl.restore()`, and `CsvExporter.export()` in try/catch — on exception call `FirebaseCrashlytics.getInstance().recordException(e)`. NEVER log amounts, category names, or any user data.
- Strings: `settings_analytics_title`, `settings_analytics_subtitle`.

### Session 4 Unit Tests

- `InAppReviewManagerTest`: `isEligibleForReview()` returns `false` when `reviewShownCount >= 2`; returns `true` when all conditions met.
- `ReviewPreferencesImplTest`: `markReviewShown()` increments count and updates timestamp.
- `SettingsViewModelTest`: `analyticsConsent` StateFlow reflects DataStore value.

**Quality gate:** `./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest`

---

## SESSION 5 — ASO & Play Store Listing (v3.2.5)

**Scope:** Optimised Play Store copy (EN + VI), ScreenshotSeedActivity for demo data,
Fastlane metadata directory, and release-note automation.

### Feature 21 — ScreenshotSeedActivity (debug only)

New file: `app/src/debug/java/.../ScreenshotSeedActivity.kt`

- `@AndroidEntryPoint` activity; inject `TransactionRepository`, `CategoryRepository`, `RecurringTransactionRepository`, `BudgetPreferences`.
- On `onCreate`: call `seedDemoData()` — insert 30 transactions across 6 categories, 2 budgets, 3 recurring items with varied next-due dates.
- Expose via `AndroidManifest` debug variant only. Not included in release APK.

### Feature 22 — Fastlane metadata structure

Create directory tree under `fastlane/metadata/android/`:

```
en-US/
  short_description.txt   — 80-char optimised English short description
  full_description.txt    — structured English long description (hook + bullets + privacy + CTA)
  changelogs/3020500.txt  — What's New for v3.2.5
  images/phoneScreenshots/  — 8 PNG placeholders (1080×1920)
  images/featureGraphic.png — 1024×500 placeholder
vi-VN/
  short_description.txt   — Vietnamese short description
  full_description.txt    — Vietnamese long description
  changelogs/3020500.txt  — Vietnamese What's New
  images/phoneScreenshots/  — 8 Vietnamese-locale PNG placeholders
```

Write the EN and VI text files with the copy below.

**EN short description (80 chars):**
`Track expenses, set budgets & manage multi-currency finances effortlessly.`

**EN long description structure:**
```
Take control of your finances with Expense Tracker — the simplest way to log
daily expenses, monitor budgets, and manage money in multiple currencies.

✅ Multi-currency: VND, USD, EUR, JPY, KRW, SGD
✅ Beautiful charts — donut & bar summaries by month/year
✅ Recurring transactions — automate rent, subscriptions & bills
✅ Budget alerts — monthly limits per currency
✅ Custom categories — organise spending your way
✅ Dark mode — easy on the eyes, day or night
✅ Cloud backup — never lose your data
✅ CSV export — analyse in Excel

Your data stays on your device. No account. No ads. Ever.

Download free and start tracking today.
```

**VI short description:**
`Theo dõi chi tiêu, đặt ngân sách & quản lý đa tiền tệ dễ dàng.`

### Feature 23 — What's New template automation

- `docs/release-notes-v3.2.5.md` (produced by prd-writer in Step 9):
  - Sections: Highlights, New Features (Dark Mode, Onboarding, Accessibility, Feedback), Bug Fixes, Known Limitations.
- `fastlane/metadata/android/en-US/changelogs/3020500.txt`: 500-char Play Store "What's New" distilled from release notes.

### Session 5 Unit Tests

- `ScreenshotSeedActivityTest` (Robolectric): `seedDemoData()` inserts exactly 30 transactions without throwing.

**Quality gate:** `./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest`

---

## Final Checklist Before Release (v3.2.5)

- [ ] `versionName = "3.2.5"` in `app/build.gradle.kts`
- [ ] All tests pass: `./gradlew :app:testDebugUnitTest`
- [ ] Spotless clean: `./gradlew spotlessApply && ./gradlew spotlessCheck`
- [ ] Theme toggle: Light/Dark/System in Settings — full app responds without restart
- [ ] Dark mode: all screens correct; no hardcoded colors
- [ ] Onboarding: shows on fresh install, skippable, never repeats after "Get Started"
- [ ] Onboarding: page 3 "Get Started" navigates to Home and clears back stack
- [ ] Accessibility: TalkBack reads DonutChart and MonthlyBarChart bars by name + amount
- [ ] Accessibility: income/expense rows announced with icon + type label
- [ ] Accessibility: all IconButtons have content descriptions
- [ ] Accessibility: no touch target < 48dp (Accessibility Scanner clean)
- [ ] Focus order: AddEdit screen tab order is Amount → Category → Note → Date → Save
- [ ] In-app review: triggers on 5th transaction (test with `ReviewManager.setFakeReturnValue()`)
- [ ] Feedback sheet: 👍 triggers review flow; 👎 opens email intent with device info only
- [ ] Crashlytics: disabled by default; enabled after user toggles consent in Settings
- [ ] Crashlytics: no amounts or category names in any log output
- [ ] Fastlane EN + VI metadata files written and correct length
- [ ] `docs/release-notes-v3.2.5.md` present
- [ ] No TODO/FIXME in new code
- [ ] All new strings in `res/values/strings.xml`

## New / Modified Files

**New:**
`data/preferences/ThemePreference.kt` · `data/preferences/ThemePreferencesRepository.kt` ·
`data/preferences/ThemePreferencesRepositoryImpl.kt` · `di/ThemeModule.kt` ·
`data/preferences/OnboardingRepository.kt` · `data/preferences/OnboardingRepositoryImpl.kt` ·
`di/OnboardingModule.kt` · `ui/screen/onboarding/OnboardingScreen.kt` ·
`ui/screen/onboarding/OnboardingViewModel.kt` ·
`data/preferences/ReviewPreferences.kt` · `data/preferences/ReviewPreferencesImpl.kt` ·
`domain/review/InAppReviewManager.kt` · `domain/review/InAppReviewManagerImpl.kt` ·
`di/ReviewModule.kt` · `ui/screen/settings/FeedbackBottomSheet.kt` ·
`data/preferences/AnalyticsPreferences.kt` ·
`app/src/debug/.../ScreenshotSeedActivity.kt` ·
`fastlane/metadata/android/en-US/short_description.txt` ·
`fastlane/metadata/android/en-US/full_description.txt` ·
`fastlane/metadata/android/vi-VN/short_description.txt` ·
`fastlane/metadata/android/vi-VN/full_description.txt` ·
`docs/phase-6-technical-summary.md` · `docs/release-notes-v3.2.5.md`

**Modified:**
`gradle/libs.versions.toml` · `app/build.gradle.kts` · `AndroidManifest.xml` ·
`ExpenseTrackerApplication.kt` · `MainActivity.kt` ·
`ui/navigation/ExpenseTrackerNavigation.kt` ·
`ui/component/DonutChart.kt` · `ui/component/MonthlyBarChart.kt` ·
`ui/screen/settings/SettingsScreen.kt` · `ui/screen/settings/SettingsViewModel.kt` ·
`ui/screen/home/HomeScreen.kt` · `ui/screen/home/HomeViewModel.kt` ·
`ui/screen/addedit/AddEditTransactionScreen.kt` ·
`data/worker/RecurringTransactionWorker.kt` ·
`data/backup/BackupRepositoryImpl.kt` · `data/export/CsvExporter.kt` ·
`res/values/strings.xml`
