package dev.tuandoan.expensetracker.domain.crash

/**
 * Abstraction for crash reporting services.
 * Implementations must never log transaction amounts, category names, or any PII.
 */
interface CrashReporter {
    /** Records a non-fatal exception for crash reporting. */
    fun recordException(e: Exception)

    /** Enables or disables crash report collection based on user consent. */
    fun setCollectionEnabled(enabled: Boolean)
}
