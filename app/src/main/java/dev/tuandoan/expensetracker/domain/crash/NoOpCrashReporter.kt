package dev.tuandoan.expensetracker.domain.crash

import javax.inject.Inject
import javax.inject.Singleton

/**
 * No-op implementation of [CrashReporter].
 * Used as the default when no crash reporting service (e.g. Firebase Crashlytics) is configured.
 * Replace this binding with a real implementation when Firebase credentials are available.
 */
@Singleton
class NoOpCrashReporter
    @Inject
    constructor() : CrashReporter {
        override fun recordException(e: Exception) {
            // No-op: crash reporting service not configured
        }

        override fun setCollectionEnabled(enabled: Boolean) {
            // No-op: crash reporting service not configured
        }
    }
