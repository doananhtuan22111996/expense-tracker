package dev.tuandoan.expensetracker.data.crash

import dev.tuandoan.expensetracker.domain.crash.CrashReporter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real [CrashReporter] implementation bound in release builds only (ADR-010).
 *
 * This class is deliberately minimal: it forwards [recordException] and
 * [setCollectionEnabled] straight to Firebase Crashlytics via
 * [FirebaseCrashlyticsWrapper]. Consent observation and lifecycle wiring live
 * in [dev.tuandoan.expensetracker.ExpenseTrackerApplication.onCreate] — it
 * already collects [dev.tuandoan.expensetracker.data.preferences.AnalyticsPreferences.analyticsConsent]
 * and calls [setCollectionEnabled] on every emission, so this class doesn't
 * need its own observer scope.
 *
 * Privacy guarantees (PRD FR-10 / FR-11):
 * - No `setUserId` is ever called — Firebase's installation-level anonymous
 *   ID is the only identifier used (same as what Crashlytics uses by default).
 * - No `setCustomKey` is called with any user-supplied data.
 * - [recordException] forwards the caller's [Exception] object unchanged; the
 *   5 call sites that touch this method (audited 2026-05-02) all pass caught
 *   exception objects with no amount / category / note content concatenated
 *   into their messages.
 */
@Singleton
class FirebaseCrashReporterImpl
    @Inject
    constructor(
        private val crashlytics: FirebaseCrashlyticsWrapper,
    ) : CrashReporter {
        override fun recordException(e: Exception) {
            crashlytics.recordException(e)
        }

        override fun setCollectionEnabled(enabled: Boolean) {
            crashlytics.setCrashlyticsCollectionEnabled(enabled)
        }
    }
