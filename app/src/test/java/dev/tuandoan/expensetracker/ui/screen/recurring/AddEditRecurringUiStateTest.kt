package dev.tuandoan.expensetracker.ui.screen.recurring

import dev.tuandoan.expensetracker.domain.model.RecurrenceFrequency
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.testutil.TestData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddEditRecurringUiStateTest {
    // hasUnsavedChanges — add mode

    @Test
    fun hasUnsavedChanges_addModeEmptyForm_false() {
        val state = AddEditRecurringUiState(isLoading = false)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_addModeWithAmount_true() {
        val state = AddEditRecurringUiState(amountText = "50000", isLoading = false)
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_addModeWithNote_true() {
        val state = AddEditRecurringUiState(note = "rent", isLoading = false)
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_addModeWithCategory_true() {
        val state =
            AddEditRecurringUiState(
                selectedCategory = TestData.expenseCategory,
                isLoading = false,
            )
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_addModeBlankInputs_false() {
        val state =
            AddEditRecurringUiState(
                amountText = "  ",
                note = "  ",
                selectedCategory = null,
                isLoading = false,
            )
        assertFalse(state.hasUnsavedChanges)
    }

    // hasUnsavedChanges — edit mode

    @Test
    fun hasUnsavedChanges_editModeSameValues_false() {
        val state =
            AddEditRecurringUiState(
                amountText = "50000",
                note = "rent",
                selectedCategory = TestData.expenseCategory,
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = 1700000000000L,
                currencyCode = "VND",
                type = TransactionType.EXPENSE,
                originalAmountText = "50000",
                originalNote = "rent",
                originalCategoryId = TestData.expenseCategory.id,
                originalFrequency = RecurrenceFrequency.MONTHLY,
                originalStartDate = 1700000000000L,
                originalCurrencyCode = "VND",
                originalType = TransactionType.EXPENSE,
                isLoading = false,
            )
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_editModeDifferentAmount_true() {
        val state =
            AddEditRecurringUiState(
                amountText = "100000",
                note = "rent",
                selectedCategory = TestData.expenseCategory,
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = 1700000000000L,
                currencyCode = "VND",
                type = TransactionType.EXPENSE,
                originalAmountText = "50000",
                originalNote = "rent",
                originalCategoryId = TestData.expenseCategory.id,
                originalFrequency = RecurrenceFrequency.MONTHLY,
                originalStartDate = 1700000000000L,
                originalCurrencyCode = "VND",
                originalType = TransactionType.EXPENSE,
                isLoading = false,
            )
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_editModeBlankNoteMatchesNull_false() {
        val state =
            AddEditRecurringUiState(
                amountText = "50000",
                note = "",
                selectedCategory = TestData.expenseCategory,
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = 1700000000000L,
                currencyCode = "VND",
                type = TransactionType.EXPENSE,
                originalAmountText = "50000",
                originalNote = null,
                originalCategoryId = TestData.expenseCategory.id,
                originalFrequency = RecurrenceFrequency.MONTHLY,
                originalStartDate = 1700000000000L,
                originalCurrencyCode = "VND",
                originalType = TransactionType.EXPENSE,
                isLoading = false,
            )
        assertFalse(state.hasUnsavedChanges)
    }

    // isFormValid

    @Test
    fun isFormValid_validInputs_true() {
        val state =
            AddEditRecurringUiState(
                amountText = "50000",
                selectedCategory = TestData.expenseCategory,
                isLoading = false,
            )
        assertTrue(state.isFormValid)
    }

    @Test
    fun isFormValid_emptyAmount_false() {
        val state =
            AddEditRecurringUiState(
                amountText = "",
                selectedCategory = TestData.expenseCategory,
                isLoading = false,
            )
        assertFalse(state.isFormValid)
    }

    @Test
    fun isFormValid_zeroAmount_false() {
        val state =
            AddEditRecurringUiState(
                amountText = "0",
                selectedCategory = TestData.expenseCategory,
                isLoading = false,
            )
        assertFalse(state.isFormValid)
    }

    @Test
    fun isFormValid_noCategory_false() {
        val state =
            AddEditRecurringUiState(
                amountText = "50000",
                selectedCategory = null,
                isLoading = false,
            )
        assertFalse(state.isFormValid)
    }
}
