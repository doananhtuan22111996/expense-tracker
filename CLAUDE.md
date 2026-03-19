# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Communication Style
- **Language**: English for everything — code, docs, commits, PRDs, design notes, Notion
- Direct and concise — no fluff, no essay explanations
- Code blocks for all code, commands, file paths
- Multiple options? State trade-offs briefly, then recommend best one
- Never explain obvious things

## Development Workflow

Every feature goes through these stages in order:

```
STAGE 1  IDEATION       → raw idea, problem statement, goals
STAGE 2  PRD            → requirements, user stories, acceptance criteria → Notion
STAGE 3  BREAKDOWN      → epics → features → milestones → release plan → Notion
STAGE 4  TASKS          → actionable tasks with estimates → Notion (tracked)
STAGE 5  DESIGN         → UI/UX concepts, component design, screen flows → Notion
STAGE 6  ARCHITECTURE   → approach decision, patterns, trade-offs, ADR → Notion
STAGE 7  IMPLEMENTATION → Android code, production quality
STAGE 8  CODE REVIEW    → self-review: logic, security, perf, edge cases
STAGE 9  TESTING        → unit tests, manual plan, UI tests, automation
STAGE 10 GIT & PR       → Conventional Commits, PR description, CI pass, merge
STAGE 11 RELEASE        → release notes, version bump, Play Store
STAGE 12 DOCUMENTATION  → document all learnings, decisions, changes → Notion
```

## Build & Development Commands

```bash
# Compile check (fast feedback loop)
./gradlew :app:compileDebugKotlin

# Run all unit tests
./gradlew :app:testDebugUnitTest

# Run a single test class
./gradlew :app:testDebugUnitTest --tests "dev.tuandoan.expensetracker.ui.screen.home.HomeViewModelTest"

# Run a single test method
./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest.testMethodName"

# Format code (must pass before every commit)
./gradlew spotlessApply

# Check formatting without fixing
./gradlew spotlessCheck

# Full pre-commit check
./gradlew spotlessApply && ./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest
```

Spotless uses ktlint 1.4.0 with `.editorconfig`. It runs on `*.kt` and `*.gradle.kts` files.

## Architecture

Single-module Android app: `app/`

**Package:** `dev.tuandoan.expensetracker`

**Pattern:** MVVM — `Screen (Composable) -> ViewModel -> Repository (interface) -> DAO/DataStore (impl)`

### Layer structure under `app/src/main/java/dev/tuandoan/expensetracker/`:

- `domain/model/` — Pure Kotlin domain models (`Transaction`, `Category`, `RecurringTransaction`, `CurrencyDefinition`, `BudgetStatus`)
- `domain/repository/` — Repository interfaces (`TransactionRepository`, `CategoryRepository`, `BackupRepository`, `BudgetPreferences`, etc.)
- `domain/review/` — In-app review manager interface + impl
- `domain/crash/` — CrashReporter interface + NoOp impl
- `data/database/` — Room database (`AppDatabase`), DAOs, entities, migrations. DB version 5, schemas exported to `app/schemas/`
- `data/database/entity/` — Room entities (`TransactionEntity`, `CategoryEntity`, `RecurringTransactionEntity`) + query row types
- `data/preferences/` — DataStore-backed preferences (theme, onboarding, currency, budget, analytics, review, selected month)
- `data/backup/` — Backup/restore logic with serialization, validation, and mappers
- `data/export/` — CSV export
- `data/worker/` — WorkManager workers (`RecurringTransactionWorker`)
- `data/seed/` — Demo data seeding
- `repository/` — Repository implementations (`TransactionRepositoryImpl`, `CategoryRepositoryImpl`, `RecurringTransactionRepositoryImpl`)
- `core/formatter/` — Currency formatting (`CurrencyFormatter` interface, `DefaultCurrencyFormatter`)
- `core/util/` — Date/time utilities, recurrence scheduler, error utils
- `di/` — Hilt modules (see below)
- `ui/screen/` — Screen-level composables + ViewModels, organized by feature (`home/`, `settings/`, `addedit/`, `categories/`, `recurring/`, `summary/`, `onboarding/`)
- `ui/component/` — Shared composables (`DonutChart`, `MonthlyBarChart`, `MonthSelector`, `BudgetProgressSection`, etc.)
- `ui/navigation/` — `ExpenseTrackerDestination` (route definitions) + `ExpenseTrackerNavigation` (NavHost)
- `ui/theme/` — Material 3 theme, colors, typography, `DesignSystem` tokens

### DI Modules (`di/`)

| Module | Type | Provides |
|---|---|---|
| `DatabaseModule` | `object` / `@Provides` | AppDatabase, DAOs, ContentResolver |
| `DispatcherModule` | `object` / `@Provides` | `@IoDispatcher`, `@DefaultDispatcher`, Clock, ZoneId |
| `RepositoryModule` | `abstract class` / `@Binds` | All repository interface -> impl bindings, CurrencyFormatter, TimeProvider, TransactionRunner |
| `ThemeModule` | `abstract class` / `@Binds` | ThemePreferencesRepository |
| `OnboardingModule` | `abstract class` / `@Binds` | OnboardingRepository |
| `ReviewModule` | `abstract class` / `@Binds` | ReviewPreferences, InAppReviewManager, AnalyticsPreferences |

### Navigation

Two route groups in `ExpenseTrackerDestination.kt`:
- `BottomNavDestination` — Home, Summary, Settings (shown in bottom nav, route prefix `main/`)
- `ModalDestination` — AddEditTransaction, Categories, Recurring, AddRecurring (full-screen, route prefix `modal/`)

Onboarding route is a separate start destination guard in `ExpenseTrackerNavigation.kt`.

## Key Conventions

**StateFlow pattern for ViewModels:**
- Mutable state: `private val _uiState = MutableStateFlow(UiState())` / `val uiState = _uiState.asStateFlow()`
- DataStore flows: `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), defaultValue)`
- Async operations use `BackupOperation` enum + `backupJob: Job?` pattern (see `SettingsViewModel`)

**Dispatcher injection:** Always inject `@IoDispatcher` for IO work, never use `Dispatchers.IO` directly.

**Amount storage:** `Long` values. Currencies with `minorUnitDigits=0` (VND, JPY, KRW) store whole units. Currencies with `minorUnitDigits=2` (USD, EUR, SGD) store cents. Use `CurrencyFormatter.formatBareAmount()` for display.

**DB transactions:** Use `TransactionRunner.runInTransaction {}` (wraps Room's `withTransaction`).

**No hardcoded strings** — all user-facing text in `res/values/strings.xml`.

**No hardcoded colors** — use `MaterialTheme.colorScheme.*` tokens. Category colors via `DesignSystem.categoryColor()`.

**Privacy:** Never log transaction amounts, category names, or PII.

## Core Engineering Principles

1. **Production quality** — assume this fails in prod; handle it now
2. **Security first** — auth, data exposure, input validation; EncryptedSharedPreferences for sensitive data
3. **Offline-first** — local cache, sync strategy, graceful degradation
4. **Clean architecture** — domain layer owns business logic; zero platform deps in domain
5. **Test coverage** — unit test business logic and ViewModels; skip trivial boilerplate
6. **Incremental** — small focused PRs; never big bang rewrites
7. **Consistency** — follow existing project conventions before introducing new patterns
8. **Explicit over clever** — readable code beats smart code

## Git Conventions

```
Branch:
  feat/<short-description>
  fix/<short-description>
  chore/<short-description>
  docs/<short-description>
  test/<short-description>
  refactor/<short-description>

Commit (Conventional Commits):
  feat(scope): description
  fix(scope): description
  chore(scope): description
  docs(scope): description
  refactor(scope): description
  test(scope): description
  perf(scope): description

PR title  = same style as commit
PR body   = what / why / how / test plan
```

## Code Review Checklist

Before any code is considered ready:
- [ ] Logic correct + all edge cases handled
- [ ] No sensitive data in logs, error messages, or API responses
- [ ] Auth and permission checks in place
- [ ] No force unwrap (`!!`) without explicit justification
- [ ] Error handling: network, DB, and unknown/unexpected errors
- [ ] No scope/memory leaks (lifecycle-aware)
- [ ] No unnecessary recompositions or N+1 queries
- [ ] Unit tests cover critical business logic
- [ ] Naming is clear and self-documenting

## Testing Patterns

- Framework: JUnit4, Mockito-Kotlin (`mock()`, `whenever()`, `verify()`), Turbine (Flow testing), kotlinx-coroutines-test
- `MainDispatcherRule` — custom `TestWatcher` in `testutil/` that sets `StandardTestDispatcher` as `Dispatchers.Main`
- `TestData` — shared test fixtures in `testutil/TestData.kt`
- Fake implementations in `testutil/` (e.g., `FakeTimeProvider`, `FakeCurrencyPreferenceRepository`, `FakeSelectedMonthRepository`)
- ViewModel tests use `@get:Rule val mainDispatcherRule = MainDispatcherRule()` and `advanceUntilIdle()`

## Version & Release

- Version in `app/build.gradle.kts`: `versionName` (semver), `versionCode` (epoch seconds, auto-generated)
- Room schema version: 5 (migrations in `data/database/migration/Migrations.kt`)
- Dependencies managed via `gradle/libs.versions.toml`
- Fastlane metadata in `fastlane/metadata/android/` (en-US, vi-VN)
- Release signing uses `secret.properties` or env vars (`SIGNING_KEY_ALIAS`, `SIGNING_KEYSTORE_PASSWORD`, `SIGNING_KEY_PASSWORD`)

## Notion Documentation Standard

Every doc saved to Notion:
- **Title format**: `[ExpenseTracker] [DocType] — [Feature/Topic]`
- **Date**: created + last updated
- **Status**: Draft | In Review | Approved | Archived
- **TL;DR**: 2-3 line summary at top
- **Decisions**: explicitly call out decisions made and WHY
- Tables and headers for structure

## Available Slash Commands

### Shared
```
/idea       → Capture and evaluate a raw idea
/prd        → Write a Product Requirements Document
/breakdown  → Break a feature/epic into tasks + estimates
/roadmap    → Plan milestones and release timeline
/design     → UI/UX design concept: flows, components, screens
/arch       → Architecture Decision Record (ADR)
/pr         → Generate PR title + body
/release    → Generate release notes for a version
/doc        → Format notes/decisions into Notion-ready documentation
/standup    → Generate standup update from current context
```

### Android
```
/impl       → Android implementation plan (Kotlin/Compose/MVVM)
/review     → Android self-code review checklist
/test       → Android test plan (JUnit + MockK + Turbine + Compose)
/debug      → Android debug session (ADB, Logcat, Coroutines)
/perf       → Android performance audit (Compose, Room, memory)
/deps       → Gradle dependency management (libs.versions.toml)
```

## Default Behavior

When receiving any task I automatically:
1. Read this `CLAUDE.md` first — understand conventions
2. Explore relevant files before writing any code
3. Think about edge cases, error states, and failure paths
4. Follow existing patterns; introduce new ones only when clearly better
5. Use English for all output

## When In Doubt

- Explore first, code second
- Ask 1 specific question, not multiple vague ones
- For multiple approaches: state trade-offs briefly, recommend best option
- If scope is unclear: make smallest correct change, flag what's left
