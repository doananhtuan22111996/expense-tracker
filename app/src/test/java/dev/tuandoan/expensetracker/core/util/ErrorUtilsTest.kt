package dev.tuandoan.expensetracker.core.util

import android.database.sqlite.SQLiteException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class ErrorUtilsTest {
    // getErrorMessage tests

    @Test
    fun getErrorMessage_sqliteException_returnsDatabaseMessage() {
        val exception = SQLiteException("table not found")
        assertEquals("Database error occurred. Please try again.", ErrorUtils.getErrorMessage(exception))
    }

    @Test
    fun getErrorMessage_unknownHostException_returnsNetworkMessage() {
        val exception = UnknownHostException("host not found")
        assertEquals(
            "Network connection error. Please check your internet connection.",
            ErrorUtils.getErrorMessage(exception),
        )
    }

    @Test
    fun getErrorMessage_illegalArgumentException_returnsValidationMessage() {
        val exception = IllegalArgumentException("bad input")
        assertEquals("Invalid data provided. Please check your input.", ErrorUtils.getErrorMessage(exception))
    }

    @Test
    fun getErrorMessage_illegalStateException_returnsStateMessage() {
        val exception = IllegalStateException("bad state")
        assertEquals(
            "App state error. Please restart the app if the problem persists.",
            ErrorUtils.getErrorMessage(exception),
        )
    }

    @Test
    fun getErrorMessage_unknownExceptionWithMessage_returnsMessage() {
        val exception = RuntimeException("Something specific happened")
        assertEquals("Something specific happened", ErrorUtils.getErrorMessage(exception))
    }

    @Test
    fun getErrorMessage_unknownExceptionWithBlankMessage_returnsGeneric() {
        val exception = RuntimeException("   ")
        assertEquals("An unexpected error occurred. Please try again.", ErrorUtils.getErrorMessage(exception))
    }

    @Test
    fun getErrorMessage_unknownExceptionWithNullMessage_returnsGeneric() {
        val exception = RuntimeException(null as String?)
        assertEquals("An unexpected error occurred. Please try again.", ErrorUtils.getErrorMessage(exception))
    }

    // isRecoverable tests

    @Test
    fun isRecoverable_sqliteException_true() {
        assertTrue(ErrorUtils.isRecoverable(SQLiteException("error")))
    }

    @Test
    fun isRecoverable_unknownHostException_true() {
        assertTrue(ErrorUtils.isRecoverable(UnknownHostException("error")))
    }

    @Test
    fun isRecoverable_illegalArgumentException_false() {
        assertFalse(ErrorUtils.isRecoverable(IllegalArgumentException("error")))
    }

    @Test
    fun isRecoverable_illegalStateException_false() {
        assertFalse(ErrorUtils.isRecoverable(IllegalStateException("error")))
    }

    @Test
    fun isRecoverable_unknownException_true() {
        assertTrue(ErrorUtils.isRecoverable(RuntimeException("error")))
    }

    // categorizeError tests

    @Test
    fun categorizeError_sqliteException_database() {
        assertEquals(ErrorUtils.ErrorCategory.DATABASE, ErrorUtils.categorizeError(SQLiteException("error")))
    }

    @Test
    fun categorizeError_unknownHostException_network() {
        assertEquals(ErrorUtils.ErrorCategory.NETWORK, ErrorUtils.categorizeError(UnknownHostException("error")))
    }

    @Test
    fun categorizeError_illegalArgumentException_validation() {
        assertEquals(
            ErrorUtils.ErrorCategory.VALIDATION,
            ErrorUtils.categorizeError(IllegalArgumentException("error")),
        )
    }

    @Test
    fun categorizeError_illegalStateException_state() {
        assertEquals(ErrorUtils.ErrorCategory.STATE, ErrorUtils.categorizeError(IllegalStateException("error")))
    }

    @Test
    fun categorizeError_unknownException_unknown() {
        assertEquals(ErrorUtils.ErrorCategory.UNKNOWN, ErrorUtils.categorizeError(RuntimeException("error")))
    }
}
