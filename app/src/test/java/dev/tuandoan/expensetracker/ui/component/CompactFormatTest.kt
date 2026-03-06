package dev.tuandoan.expensetracker.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class CompactFormatTest {
    @Test
    fun smallValues_noSuffix() {
        assertEquals("0", compactFormat(0L))
        assertEquals("999", compactFormat(999L))
    }

    @Test
    fun thousands_useKSuffix() {
        assertEquals("1K", compactFormat(1000L))
        assertEquals("1.5K", compactFormat(1500L))
        assertEquals("500K", compactFormat(500000L))
    }

    @Test
    fun millions_useMSuffix() {
        assertEquals("1M", compactFormat(1000000L))
        assertEquals("1.2M", compactFormat(1200000L))
        assertEquals("10M", compactFormat(10000000L))
    }

    @Test
    fun billions_useBSuffix() {
        assertEquals("1B", compactFormat(1000000000L))
        assertEquals("2.5B", compactFormat(2500000000L))
    }
}
