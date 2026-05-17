package dev.tuandoan.expensetracker.domain.model

/**
 * A user-defined Trip — travel-time container for transactions (v3.13.0).
 *
 * Snapshot fields are non-null only for trips created through the legacy-category
 * conversion wizard (Epic 4). They feed [DeleteTripBehavior.REVERT_TO_ORIGINAL_CATEGORY]
 * per ADR-001.
 *
 * `createdAt` is epoch millis to match the project-wide convention shared with
 * [Transaction], [RecurringTransaction], and the gold entities.
 */
data class Trip(
    val id: Long = 0L,
    val name: String,
    val destination: String?,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long,
    val foreignCurrencyCode: String?,
    val foreignToHomeRate: Double?,
    val originalCategoryId: Long?,
    val originalCategoryNameSnapshot: String?,
    val originalCategoryIconSnapshot: String?,
    val originalCategoryColorSnapshot: String?,
    val createdAt: Long,
) {
    /** True when this trip was produced by the conversion wizard and supports revert. */
    val isConversionOrigin: Boolean
        get() = originalCategoryId != null
}
