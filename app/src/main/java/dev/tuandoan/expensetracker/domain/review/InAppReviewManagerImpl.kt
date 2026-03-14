package dev.tuandoan.expensetracker.domain.review

import android.app.Activity
import dev.tuandoan.expensetracker.data.preferences.ReviewPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [InAppReviewManager].
 *
 * Currently uses a no-op approach for [requestReview] since the Google Play Review
 * library requires a Play Store-distributed build to function. When the Play Review
 * dependency is wired in, replace the body of [requestReview] with the actual
 * ReviewManager flow.
 */
@Singleton
class InAppReviewManagerImpl
    @Inject
    constructor(
        private val reviewPreferences: ReviewPreferences,
    ) : InAppReviewManager {
        override suspend fun isEligibleForReview(): Boolean {
            val shownCount = reviewPreferences.reviewShownCount.first()
            if (shownCount >= MAX_REVIEW_PROMPTS) return false

            val lastPrompt = reviewPreferences.lastReviewPromptMillis.first()
            val now = System.currentTimeMillis()

            // First-time check: if never shown, eligible
            if (lastPrompt == 0L) return true

            // Cooldown check: at least 30 days since last prompt
            val daysSinceLastPrompt = (now - lastPrompt) / MILLIS_PER_DAY
            return daysSinceLastPrompt >= COOLDOWN_DAYS
        }

        override suspend fun requestReview(activity: Activity) {
            // No-op: Play Review library requires Play Store distribution.
            // When ready, use ReviewManagerFactory.create(activity) to launch the flow.
            markReviewShown()
        }

        override suspend fun markReviewShown() {
            reviewPreferences.markReviewShown()
        }

        companion object {
            /** Maximum number of times the review prompt is shown. */
            const val MAX_REVIEW_PROMPTS = 2

            /** Minimum days between review prompts. */
            const val COOLDOWN_DAYS = 30L

            private const val MILLIS_PER_DAY = 86_400_000L
        }
    }
