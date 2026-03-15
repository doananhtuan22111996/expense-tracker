package dev.tuandoan.expensetracker.domain.review

import android.app.Activity

/**
 * Manages in-app review prompt eligibility and presentation.
 *
 * Eligibility criteria:
 * - App installed for at least 7 days
 * - Review cooldown of at least 30 days since last prompt
 * - Review has been shown fewer than 2 times total
 */
interface InAppReviewManager {
    /**
     * Checks whether the user is eligible for an in-app review prompt.
     * @return `true` if all eligibility criteria are met
     */
    suspend fun isEligibleForReview(): Boolean

    /**
     * Requests presentation of the in-app review flow.
     * This is a no-op if the Play Review library is not available.
     * @param activity the activity context for launching the review flow
     */
    suspend fun requestReview(activity: Activity)

    /** Records that the review prompt was shown to the user. */
    suspend fun markReviewShown()
}
