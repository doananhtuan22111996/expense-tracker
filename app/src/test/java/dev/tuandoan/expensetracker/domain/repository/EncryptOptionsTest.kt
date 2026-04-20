package dev.tuandoan.expensetracker.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptOptionsTest {
    @Test
    fun close_zeroesPasswordArray() {
        val password = "correctPW1".toCharArray()
        val options = EncryptOptions(password)

        options.close()

        for (c in password) {
            assertEquals('\u0000', c)
        }
    }

    @Test
    fun equals_samePassword_returnsTrue() {
        val a = EncryptOptions("correctPW1".toCharArray())
        val b = EncryptOptions("correctPW1".toCharArray())

        assertTrue(a == b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentPassword_returnsFalse() {
        val a = EncryptOptions("correctPW1".toCharArray())
        val b = EncryptOptions("wrongPW1234".toCharArray())

        assertFalse(a == b)
    }
}
