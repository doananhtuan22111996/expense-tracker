package dev.tuandoan.expensetracker.core.util

import android.database.sqlite.SQLiteException
import dev.tuandoan.expensetracker.R
import java.net.UnknownHostException

/**
 * Utility functions for consistent error handling and user-friendly error messages
 */
object ErrorUtils {
    /**
     * Converts exceptions to user-friendly error messages as UiText
     */
    fun getErrorMessage(exception: Throwable): UiText =
        when (exception) {
            is SQLiteException -> UiText.StringResource(R.string.error_database)
            is UnknownHostException -> UiText.StringResource(R.string.error_network)
            is IllegalArgumentException -> UiText.StringResource(R.string.error_invalid_data)
            is IllegalStateException -> UiText.StringResource(R.string.error_app_state)
            else -> {
                val message = exception.message?.takeIf { it.isNotBlank() }
                if (message != null) {
                    UiText.DynamicString(message)
                } else {
                    UiText.StringResource(R.string.error_generic)
                }
            }
        }

    /**
     * Determines if an error is recoverable and should show a retry option
     */
    fun isRecoverable(exception: Throwable): Boolean =
        when (exception) {
            is SQLiteException -> true // Database operations can usually be retried
            is UnknownHostException -> true // Network issues are often temporary
            is IllegalArgumentException -> false // Invalid data needs user correction
            is IllegalStateException -> false // State issues need app restart
            else -> true // Default to recoverable for unknown exceptions
        }

    /**
     * Categories for error types to help with analytics and debugging
     */
    enum class ErrorCategory {
        DATABASE,
        NETWORK,
        VALIDATION,
        STATE,
        UNKNOWN,
    }

    /**
     * Categorizes exceptions for better error tracking
     */
    fun categorizeError(exception: Throwable): ErrorCategory =
        when (exception) {
            is SQLiteException -> ErrorCategory.DATABASE
            is UnknownHostException -> ErrorCategory.NETWORK
            is IllegalArgumentException -> ErrorCategory.VALIDATION
            is IllegalStateException -> ErrorCategory.STATE
            else -> ErrorCategory.UNKNOWN
        }
}
