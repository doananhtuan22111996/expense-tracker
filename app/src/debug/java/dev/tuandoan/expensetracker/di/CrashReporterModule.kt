package dev.tuandoan.expensetracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.expensetracker.domain.crash.CrashReporter
import dev.tuandoan.expensetracker.domain.crash.NoOpCrashReporter

/**
 * Debug-only binding for [CrashReporter] — always no-op regardless of the
 * user's consent toggle (ADR-010). Debug development churn never pollutes
 * the production Crashlytics dashboard; the Firebase SDK is not even on the
 * debug classpath (see `releaseImplementation` scoping in `app/build.gradle.kts`).
 *
 * Release builds use the sibling module under `src/release/` that binds
 * [dev.tuandoan.expensetracker.data.crash.FirebaseCrashReporterImpl] instead.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CrashReporterModule {
    @Binds
    abstract fun bindCrashReporter(impl: NoOpCrashReporter): CrashReporter
}
