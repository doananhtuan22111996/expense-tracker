package dev.tuandoan.expensetracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.expensetracker.data.crash.FirebaseCrashReporterImpl
import dev.tuandoan.expensetracker.data.crash.FirebaseCrashlyticsWrapper
import dev.tuandoan.expensetracker.data.crash.FirebaseCrashlyticsWrapperImpl
import dev.tuandoan.expensetracker.domain.crash.CrashReporter

/**
 * Release-only binding for [CrashReporter] — forwards to Firebase Crashlytics.
 *
 * Per ADR-010, debug builds bind [dev.tuandoan.expensetracker.domain.crash.NoOpCrashReporter]
 * instead (see the sibling module under `src/debug/`). This source-set split
 * makes the debug-suppression rule a physical constraint: debug builds have
 * no Firebase classes on classpath at all.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CrashReporterModule {
    @Binds
    abstract fun bindCrashReporter(impl: FirebaseCrashReporterImpl): CrashReporter

    @Binds
    abstract fun bindFirebaseCrashlyticsWrapper(impl: FirebaseCrashlyticsWrapperImpl): FirebaseCrashlyticsWrapper
}
