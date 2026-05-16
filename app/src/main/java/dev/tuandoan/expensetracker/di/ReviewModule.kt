package dev.tuandoan.expensetracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.expensetracker.data.preferences.AnalyticsPreferences
import dev.tuandoan.expensetracker.data.preferences.AnalyticsPreferencesImpl
import dev.tuandoan.expensetracker.data.preferences.BackupEncryptionPreferences
import dev.tuandoan.expensetracker.data.preferences.BackupEncryptionPreferencesImpl
import dev.tuandoan.expensetracker.data.preferences.HomeBannerPreferences
import dev.tuandoan.expensetracker.data.preferences.HomeBannerPreferencesImpl
import dev.tuandoan.expensetracker.data.preferences.InsightsCollapsePreferences
import dev.tuandoan.expensetracker.data.preferences.InsightsCollapsePreferencesImpl
import dev.tuandoan.expensetracker.data.preferences.ReviewPreferences
import dev.tuandoan.expensetracker.data.preferences.ReviewPreferencesImpl
import dev.tuandoan.expensetracker.data.preferences.WidgetCategoryPreferences
import dev.tuandoan.expensetracker.data.preferences.WidgetCategoryPreferencesImpl
import dev.tuandoan.expensetracker.domain.review.InAppReviewManager
import dev.tuandoan.expensetracker.domain.review.InAppReviewManagerImpl

/**
 * Hilt module that provides bindings for review, analytics, and backup-encryption preferences.
 *
 * The `CrashReporter` binding moved to source-set-specific modules (see
 * `src/debug/di/CrashReporterModule.kt` + `src/release/di/CrashReporterModule.kt`)
 * per ADR-010 — debug builds bind `NoOpCrashReporter`, release builds bind
 * `FirebaseCrashReporterImpl`.
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
    abstract fun bindBackupEncryptionPreferences(impl: BackupEncryptionPreferencesImpl): BackupEncryptionPreferences

    @Binds
    abstract fun bindInsightsCollapsePreferences(impl: InsightsCollapsePreferencesImpl): InsightsCollapsePreferences

    @Binds
    abstract fun bindWidgetCategoryPreferences(impl: WidgetCategoryPreferencesImpl): WidgetCategoryPreferences

    @Binds
    abstract fun bindHomeBannerPreferences(impl: HomeBannerPreferencesImpl): HomeBannerPreferences
}
