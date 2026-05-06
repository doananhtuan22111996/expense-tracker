package dev.tuandoan.expensetracker.data.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around [FirebaseAnalytics]'s singleton so
 * [FirebaseAnalyticsImpl] can be unit-tested without Robolectric or a
 * Firebase app context. Mirrors the [dev.tuandoan.expensetracker.data.crash.FirebaseCrashlyticsWrapper]
 * pattern established in PR #103.
 *
 * The wrapper stays SDK-shaped on purpose — it speaks `(name, params)`,
 * not [dev.tuandoan.expensetracker.domain.analytics.AnalyticsEvent]. Sealed-hierarchy
 * serialization is [FirebaseAnalyticsImpl]'s job; the wrapper just owns
 * the static-singleton seam.
 *
 * The wrapper is intentionally dependency-free beyond the SDK singleton —
 * it does NOT observe consent, NOT branch, NOT filter. All policy logic
 * lives upstack.
 */
interface FirebaseAnalyticsWrapper {
    /**
     * Logs a raw event to the Firebase SDK. [params] is a pre-validated
     * string-to-string map — the impl is responsible for guaranteeing
     * FR-A7 compliance before calling this.
     */
    fun logEvent(
        name: String,
        params: Map<String, String>,
    )

    /**
     * Toggles SDK-level collection. Gated by
     * `AnalyticsPreferences.analyticsEventsConsent` via
     * `ExpenseTrackerApplication.onCreate`'s observer (Task 5.4).
     */
    fun setAnalyticsCollectionEnabled(enabled: Boolean)
}

@Singleton
class FirebaseAnalyticsWrapperImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : FirebaseAnalyticsWrapper {
        private val analytics: FirebaseAnalytics by lazy {
            FirebaseAnalytics.getInstance(context)
        }

        override fun logEvent(
            name: String,
            params: Map<String, String>,
        ) {
            val bundle =
                Bundle(params.size).apply {
                    params.forEach { (key, value) -> putString(key, value) }
                }
            analytics.logEvent(name, bundle)
        }

        override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
            analytics.setAnalyticsCollectionEnabled(enabled)
        }
    }
