package dev.tuandoan.expensetracker.domain.analytics

import javax.inject.Inject
import javax.inject.Singleton

/**
 * No-op implementation of [Analytics].
 *
 * Used by the **debug** source-set DI binding (ADR-010) so that debug
 * builds have zero Firebase Analytics classes on their classpath, making
 * event suppression a physical guarantee rather than a runtime check.
 *
 * Also available as a plain in-main implementation for tests that need
 * an `Analytics` instance without configuring a fake's behavior — mirrors
 * [dev.tuandoan.expensetracker.domain.crash.NoOpCrashReporter].
 */
@Singleton
class NoOpAnalytics
    @Inject
    constructor() : Analytics {
        override fun logEvent(event: AnalyticsEvent) {
            // Intentionally empty — no analytics backend configured.
        }

        override fun setCollectionEnabled(enabled: Boolean) {
            // Intentionally empty — no analytics backend to toggle.
        }
    }
