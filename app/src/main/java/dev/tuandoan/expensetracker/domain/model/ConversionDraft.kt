package dev.tuandoan.expensetracker.domain.model

/**
 * Pre-commit input for the legacy-category-to-Trip conversion wizard (Epic 4).
 *
 * The wizard is abortable at every step with zero DB writes; this draft is what flows
 * into `TripRepository.commitConversion(draft)` only when the user taps Commit on Step 3.
 *
 * Per ADR-002 supplementary decision, `amount_foreign_minor` stays NULL for all migrated
 * historical transactions even when the target trip has FX enabled — reverse-deriving a
 * foreign amount via the trip's rate is meaningless for rows logged before the rate
 * existed. The wizard surfaces that decision as helper text in Step 1.
 */
data class ConversionDraft(
    val sourceCategoryId: Long,
    val sourceCategorySnapshot: CategorySnapshot,
    val tripMetadata: TripMetadata,
    val rowDecisions: List<RowDecision>,
    val sourceDisposition: SourceDisposition,
) {
    /** Frozen at conversion time so revert can restore the exact pre-conversion category. */
    data class CategorySnapshot(
        val name: String,
        val iconKey: String?,
        val colorKey: String?,
    )

    /** User-supplied trip metadata. Foreign currency is optional. */
    data class TripMetadata(
        val name: String,
        val destination: String?,
        val startDateEpochDay: Long,
        val endDateEpochDay: Long,
        val foreignCurrencyCode: String?,
        val foreignToHomeRate: Double?,
    )

    /** One decision per transaction in the source category. */
    sealed interface RowDecision {
        val transactionId: Long

        /** Migrate this transaction to the new trip + reassign its category. */
        data class Migrate(
            override val transactionId: Long,
            val newCategoryId: Long,
        ) : RowDecision

        /** Leave this transaction untouched in the source category (forces source to be kept). */
        data class Skip(
            override val transactionId: Long,
        ) : RowDecision
    }

    /**
     * What happens to the source category after commit. `DELETE` is the auto-default when
     * every row is migrated; `KEEP` is forced when any row is skipped.
     */
    enum class SourceDisposition {
        DELETE,
        KEEP,
    }
}
