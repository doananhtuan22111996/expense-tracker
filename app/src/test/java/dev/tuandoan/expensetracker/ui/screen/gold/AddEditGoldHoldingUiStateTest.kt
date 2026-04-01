package dev.tuandoan.expensetracker.ui.screen.gold

import dev.tuandoan.expensetracker.domain.model.GoldHolding
import dev.tuandoan.expensetracker.domain.model.GoldType
import dev.tuandoan.expensetracker.domain.model.GoldWeightUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddEditGoldHoldingUiStateTest {
    private val sampleHolding =
        GoldHolding(
            id = 1L,
            type = GoldType.SJC,
            weightValue = 1.0,
            weightUnit = GoldWeightUnit.TAEL,
            buyPricePerUnit = 50000000L,
            currencyCode = "VND",
            buyDateMillis = 1700000000000L,
            note = "test note",
        )

    // hasUnsavedChanges — add mode

    @Test
    fun hasUnsavedChanges_addModeEmptyForm_false() {
        val state = AddEditGoldHoldingUiState()
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_addModeWithWeight_true() {
        val state = AddEditGoldHoldingUiState(weightText = "1.5")
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_addModeWithBuyPrice_true() {
        val state = AddEditGoldHoldingUiState(buyPriceText = "50000000")
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_addModeWithNote_true() {
        val state = AddEditGoldHoldingUiState(note = "some note")
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_addModeBlankInputs_false() {
        val state =
            AddEditGoldHoldingUiState(
                weightText = "  ",
                buyPriceText = "  ",
                note = "  ",
            )
        assertFalse(state.hasUnsavedChanges)
    }

    // hasUnsavedChanges — edit mode

    @Test
    fun hasUnsavedChanges_editModeSameValues_false() {
        val state =
            AddEditGoldHoldingUiState(
                originalHolding = sampleHolding,
                type = sampleHolding.type,
                weightText = "1.0",
                weightUnit = sampleHolding.weightUnit,
                buyPriceText = "50000000",
                buyDateMillis = sampleHolding.buyDateMillis,
                note = sampleHolding.note ?: "",
            )
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_editModeDifferentWeight_true() {
        val state =
            AddEditGoldHoldingUiState(
                originalHolding = sampleHolding,
                type = sampleHolding.type,
                weightText = "2.0",
                weightUnit = sampleHolding.weightUnit,
                buyPriceText = "50000000",
                buyDateMillis = sampleHolding.buyDateMillis,
                note = sampleHolding.note ?: "",
            )
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_editModeDifferentType_true() {
        val state =
            AddEditGoldHoldingUiState(
                originalHolding = sampleHolding,
                type = GoldType.GOLD_24K,
                weightText = "1.0",
                weightUnit = sampleHolding.weightUnit,
                buyPriceText = "50000000",
                buyDateMillis = sampleHolding.buyDateMillis,
                note = sampleHolding.note ?: "",
            )
        assertTrue(state.hasUnsavedChanges)
    }

    // isFormValid

    @Test
    fun isFormValid_validInputs_true() {
        val state =
            AddEditGoldHoldingUiState(
                weightText = "1.0",
                buyPriceText = "50000000",
            )
        assertTrue(state.isFormValid)
    }

    @Test
    fun isFormValid_emptyWeight_false() {
        val state = AddEditGoldHoldingUiState(buyPriceText = "50000000")
        assertFalse(state.isFormValid)
    }

    @Test
    fun isFormValid_zeroWeight_false() {
        val state =
            AddEditGoldHoldingUiState(
                weightText = "0",
                buyPriceText = "50000000",
            )
        assertFalse(state.isFormValid)
    }

    @Test
    fun isFormValid_emptyPrice_false() {
        val state = AddEditGoldHoldingUiState(weightText = "1.0")
        assertFalse(state.isFormValid)
    }

    // isSaveEnabled

    @Test
    fun isSaveEnabled_addModeValidForm_true() {
        val state =
            AddEditGoldHoldingUiState(
                weightText = "1.0",
                buyPriceText = "50000000",
            )
        assertTrue(state.isSaveEnabled)
    }

    @Test
    fun isSaveEnabled_editModeNoChanges_false() {
        val state =
            AddEditGoldHoldingUiState(
                originalHolding = sampleHolding,
                type = sampleHolding.type,
                weightText = "1.0",
                weightUnit = sampleHolding.weightUnit,
                buyPriceText = "50000000",
                buyDateMillis = sampleHolding.buyDateMillis,
                note = sampleHolding.note ?: "",
            )
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun isSaveEnabled_editModeWithChanges_true() {
        val state =
            AddEditGoldHoldingUiState(
                originalHolding = sampleHolding,
                type = sampleHolding.type,
                weightText = "2.0",
                weightUnit = sampleHolding.weightUnit,
                buyPriceText = "50000000",
                buyDateMillis = sampleHolding.buyDateMillis,
                note = sampleHolding.note ?: "",
            )
        assertTrue(state.isSaveEnabled)
    }
}
