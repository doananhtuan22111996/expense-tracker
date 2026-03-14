package dev.tuandoan.expensetracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.expensetracker.data.preferences.AnalyticsPreferences
import dev.tuandoan.expensetracker.data.preferences.AnalyticsPreferencesImpl
import dev.tuandoan.expensetracker.data.preferences.ReviewPreferences
import dev.tuandoan.expensetracker.data.preferences.ReviewPreferencesImpl
import dev.tuandoan.expensetracker.domain.crash.CrashReporter
import dev.tuandoan.expensetracker.domain.crash.NoOpCrashReporter
import dev.tuandoan.expensetracker.domain.review.InAppReviewManager
import dev.tuandoan.expensetracker.domain.review.InAppReviewManagerImpl

/**
 * Hilt module that provides bindings for review, analytics, and crash reporting.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ReviewModule {
    @Binds
    abstract fun bindReviewPreferences(impl: ReviewPreferencesImpl): ReviewPreferences

    @Binds
    abstract fun bindInAppReviewManager(impl: InAppReviewManagerImpl): InAppReviewManager

    @Binds
    abstract fun bindAnalyticsPreferences(impl: AnalyticsPreferencesImpl): AnalyticsPreferences

    @Binds
    abstract fun bindCrashReporter(impl: NoOpCrashReporter): CrashReporter
}
