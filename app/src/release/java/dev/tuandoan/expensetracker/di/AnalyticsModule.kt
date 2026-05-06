package dev.tuandoan.expensetracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.expensetracker.data.analytics.FirebaseAnalyticsImpl
import dev.tuandoan.expensetracker.data.analytics.FirebaseAnalyticsWrapper
import dev.tuandoan.expensetracker.data.analytics.FirebaseAnalyticsWrapperImpl
import dev.tuandoan.expensetracker.domain.analytics.Analytics

/**
 * Release-only binding for [Analytics] — forwards to Firebase Analytics.
 *
 * Per ADR-010, debug builds bind [dev.tuandoan.expensetracker.domain.analytics.NoOpAnalytics]
 * instead (see the sibling module under `src/debug/`). This source-set
 * split makes the debug-suppression rule a physical constraint: debug
 * builds have no `firebase-analytics` classes on classpath at all.
 *
 * Runtime collection is still gated by
 * [dev.tuandoan.expensetracker.data.preferences.AnalyticsPreferences.analyticsEventsConsent]
 * — this module just provides the binding. The
 * `ExpenseTrackerApplication.onCreate` observer (Task 5.4) drives
 * `analytics.setCollectionEnabled(consent)` as the preference changes.
 *
 * Mirrors the [CrashReporterModule] pattern from PR #103.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds
    abstract fun bindAnalytics(impl: FirebaseAnalyticsImpl): Analytics

    @Binds
    abstract fun bindFirebaseAnalyticsWrapper(impl: FirebaseAnalyticsWrapperImpl): FirebaseAnalyticsWrapper
}
