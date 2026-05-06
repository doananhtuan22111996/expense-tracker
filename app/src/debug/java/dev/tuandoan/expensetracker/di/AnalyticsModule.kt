package dev.tuandoan.expensetracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.expensetracker.domain.analytics.Analytics
import dev.tuandoan.expensetracker.domain.analytics.NoOpAnalytics

/**
 * Debug-only binding for [Analytics] — always no-op regardless of the
 * user's `analyticsEventsConsent` preference (ADR-010 / ADR-011). Debug
 * builds have zero `firebase-analytics` classes on classpath (see
 * `releaseImplementation` scoping in `app/build.gradle.kts`), making
 * the no-collection guarantee a physical constraint rather than a
 * runtime check.
 *
 * Release builds use the sibling module under `src/release/` that binds
 * [dev.tuandoan.expensetracker.data.analytics.FirebaseAnalyticsImpl] +
 * [dev.tuandoan.expensetracker.data.analytics.FirebaseAnalyticsWrapperImpl]
 * instead. Mirrors the [CrashReporterModule] pattern from PR #103.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds
    abstract fun bindAnalytics(impl: NoOpAnalytics): Analytics
}
