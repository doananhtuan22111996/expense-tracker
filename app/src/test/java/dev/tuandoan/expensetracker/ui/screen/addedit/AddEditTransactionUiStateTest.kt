package dev.tuandoan.expensetracker.ui.screen.addedit

import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.testutil.TestData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddEditTransactionUiStateTest {
    // isFormValid tests

    @Test
    fun isFormValid_emptyAmount_false() {
        val state =
            AddEditTransactionUiState(
                amountText = "",
                selectedCategory = TestData.expenseCategory,
            )
        assertFalse(state.isFormValid)
    }

    @Test
    fun isFormValid_blankAmount_false() {
        val state =
            AddEditTransactionUiState(
                amountText = "   ",
                selectedCategory = TestData.expenseCategory,
            )
        assertFalse(state.isFormValid)
    }

    @Test
    fun isFormValid_zeroAmount_false() {
        val state =
            AddEditTransactionUiState(
                amountText = "0",
                selectedCategory = TestData.expenseCategory,
            )
        assertFalse(state.isFormValid)
    }

    @Test
    fun isFormValid_noCategory_false() {
        val state =
            AddEditTransactionUiState(
                amountText = "50000",
                selectedCategory = null,
            )
        assertFalse(state.isFormValid)
    }

    @Test
    fun isFormValid_validAmountAndCategory_true() {
        val state =
            AddEditTransactionUiState(
                amountText = "50000",
                selectedCategory = TestData.expenseCategory,
            )
        assertTrue(state.isFormValid)
    }

    @Test
    fun isFormValid_formattedAmount_true() {
        val state =
            AddEditTransactionUiState(
                amountText = "50,000",
                selectedCategory = TestData.expenseCategory,
            )
        assertTrue(state.isFormValid)
    }

    @Test
    fun isFormValid_invalidAmountText_false() {
        val state =
            AddEditTransactionUiState(
                amountText = "abc",
                selectedCategory = TestData.expenseCategory,
            )
        assertFalse(state.isFormValid)
    }

    // hasUnsavedChanges tests

    @Test
    fun hasUnsavedChanges_noOriginalTransaction_false() {
        val state =
            AddEditTransactionUiState(
                originalTransaction = null,
                amountText = "50000",
                selectedCategory = TestData.expenseCategory,
            )
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_sameValues_false() {
        val original = TestData.sampleExpenseTransaction
        val state =
            AddEditTransactionUiState(
                originalTransaction = original,
                type = original.type,
                amountText = "50,000",
                selectedCategory = original.category,
                timestamp = original.timestamp,
                note = original.note ?: "",
            )
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_differentType_true() {
        val original = TestData.sampleExpenseTransaction
        val state =
            AddEditTransactionUiState(
                originalTransaction = original,
                type = TransactionType.INCOME,
                amountText = "50,000",
                selectedCategory = original.category,
                timestamp = original.timestamp,
                note = original.note ?: "",
            )
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_differentAmount_true() {
        val original = TestData.sampleExpenseTransaction
        val state =
            AddEditTransactionUiState(
                originalTransaction = original,
                type = original.type,
                amountText = "100000",
                selectedCategory = original.category,
                timestamp = original.timestamp,
                note = original.note ?: "",
            )
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_differentCategory_true() {
        val original = TestData.sampleExpenseTransaction
        val state =
            AddEditTransactionUiState(
                originalTransaction = original,
                type = original.type,
                amountText = "50,000",
                selectedCategory = TestData.transportCategory,
                timestamp = original.timestamp,
                note = original.note ?: "",
            )
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_differentTimestamp_true() {
        val original = TestData.sampleExpenseTransaction
        val state =
            AddEditTransactionUiState(
                originalTransaction = original,
                type = original.type,
                amountText = "50,000",
                selectedCategory = original.category,
                timestamp = original.timestamp + 86400000L,
                note = original.note ?: "",
            )
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_differentNote_true() {
        val original = TestData.sampleExpenseTransaction
        val state =
            AddEditTransactionUiState(
                originalTransaction = original,
                type = original.type,
                amountText = "50,000",
                selectedCategory = original.category,
                timestamp = original.timestamp,
                note = "Changed note",
            )
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_blankNoteMatchesNull_false() {
        val original = TestData.sampleExpenseTransaction.copy(note = null)
        val state =
            AddEditTransactionUiState(
                originalTransaction = original,
                type = original.type,
                amountText = "50,000",
                selectedCategory = original.category,
                timestamp = original.timestamp,
                note = "",
            )
        assertFalse(state.hasUnsavedChanges)
    }

    // isSaveEnabled tests

    @Test
    fun isSaveEnabled_invalidForm_false() {
        val state =
            AddEditTransactionUiState(
                amountText = "",
                selectedCategory = null,
            )
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun isSaveEnabled_validFormNewTransaction_true() {
        val state =
            AddEditTransactionUiState(
                originalTransaction = null,
                amountText = "50000",
                selectedCategory = TestData.expenseCategory,
            )
        assertTrue(state.isSaveEnabled)
    }

    @Test
    fun isSaveEnabled_validFormEditModeNoChanges_false() {
        val original = TestData.sampleExpenseTransaction
        val state =
            AddEditTransactionUiState(
                originalTransaction = original,
                type = original.type,
                amountText = "50,000",
                selectedCategory = original.category,
                timestamp = original.timestamp,
                note = original.note ?: "",
            )
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun isSaveEnabled_validFormEditModeWithChanges_true() {
        val original = TestData.sampleExpenseTransaction
        val state =
            AddEditTransactionUiState(
                originalTransaction = original,
                type = original.type,
                amountText = "100000",
                selectedCategory = original.category,
                timestamp = original.timestamp,
                note = original.note ?: "",
            )
        assertTrue(state.isSaveEnabled)
    }

    // Currency-related hasUnsavedChanges tests

    @Test
    fun hasUnsavedChanges_differentCurrencyCode_returnsTrue() {
        val original = TestData.sampleExpenseTransaction
        val state =
            AddEditTransactionUiState(
                originalTransaction = original,
                type = original.type,
                amountText = "50,000",
                selectedCategory = original.category,
                timestamp = original.timestamp,
                note = original.note ?: "",
                currencyCode = "USD",
            )
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_sameCurrencyCode_returnsFalse() {
        val original = TestData.sampleExpenseTransaction
        val state =
            AddEditTransactionUiState(
                originalTransaction = original,
                type = original.type,
                amountText = "50,000",
                selectedCategory = original.category,
                timestamp = original.timestamp,
                note = original.note ?: "",
                currencyCode = original.currencyCode,
            )
        assertFalse(state.hasUnsavedChanges)
    }
}
