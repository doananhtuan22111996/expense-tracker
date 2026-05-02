package dev.tuandoan.expensetracker.data.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around [FirebaseCrashlytics]'s static singleton so
 * [FirebaseCrashReporterImpl] can be unit-tested without Robolectric or a
 * full Firebase app context. The wrapper is intentionally dependency-free —
 * it does NOT observe consent, NOT log, NOT branch. All policy logic lives
 * in [FirebaseCrashReporterImpl] and [dev.tuandoan.expensetracker.ExpenseTrackerApplication].
 */
interface FirebaseCrashlyticsWrapper {
    fun recordException(e: Exception)

    fun setCrashlyticsCollectionEnabled(enabled: Boolean)
}

@Singleton
class FirebaseCrashlyticsWrapperImpl
    @Inject
    constructor() : FirebaseCrashlyticsWrapper {
        override fun recordException(e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }

        override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
        }
    }
