package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Preferences for tracking in-app review prompt eligibility and cooldown.
 */
interface ReviewPreferences {
    /** Epoch millis when the review prompt was last shown. */
    val lastReviewPromptMillis: Flow<Long>

    /** Number of times the review prompt has been shown to the user. */
    val reviewShownCount: Flow<Int>

    /** Records that the review prompt was shown (increments count, updates timestamp). */
    suspend fun markReviewShown()
}
