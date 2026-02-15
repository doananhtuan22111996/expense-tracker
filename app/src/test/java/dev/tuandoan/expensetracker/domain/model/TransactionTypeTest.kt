package dev.tuandoan.expensetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TransactionTypeTest {
    @Test
    fun toInt_expense_returns0() {
        assertEquals(0, TransactionType.EXPENSE.toInt())
    }

    @Test
    fun toInt_income_returns1() {
        assertEquals(1, TransactionType.INCOME.toInt())
    }

    @Test
    fun fromInt_0_returnsExpense() {
        assertEquals(TransactionType.EXPENSE, TransactionType.fromInt(0))
    }

    @Test
    fun fromInt_1_returnsIncome() {
        assertEquals(TransactionType.INCOME, TransactionType.fromInt(1))
    }

    @Test
    fun fromInt_invalid_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            TransactionType.fromInt(2)
        }
    }

    @Test
    fun fromInt_negative_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            TransactionType.fromInt(-1)
        }
    }

    @Test
    fun roundTrip_expense() {
        val original = TransactionType.EXPENSE
        assertEquals(original, TransactionType.fromInt(original.toInt()))
    }

    @Test
    fun roundTrip_income() {
        val original = TransactionType.INCOME
        assertEquals(original, TransactionType.fromInt(original.toInt()))
    }
}
