package dev.tuandoan.expensetracker.core.util

import android.database.sqlite.SQLiteException
import java.net.UnknownHostException

/**
 * Utility functions for consistent error handling and user-friendly error messages
 */
object ErrorUtils {
    /**
     * Converts exceptions to user-friendly error messages
     */
    fun getErrorMessage(exception: Throwable): String =
        when (exception) {
            is SQLiteException -> {
                "Database error occurred. Please try again."
            }
            is UnknownHostException -> {
                "Network connection error. Please check your internet connection."
            }
            is IllegalArgumentException -> {
                "Invalid data provided. Please check your input."
            }
            is IllegalStateException -> {
                "App state error. Please restart the app if the problem persists."
            }
            else -> {
                // Generic message for unknown exceptions
                exception.message?.takeIf { it.isNotBlank() } ?: "An unexpected error occurred. Please try again."
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
