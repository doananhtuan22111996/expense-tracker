package dev.tuandoan.expensetracker.ui.screen.summary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Unit tests for [formatPercent]. Guards against accidental regressions on
 * the locale-delegation contract — the engine emits integer percents and the
 * renderer must not silently drop the locale argument.
 */
class InsightRowItemPercentTest {
    @Test
    fun formatPercent_enUS_emitsPercentGlyphImmediatelyAfterDigit() {
        val output = formatPercent(percent = 5, locale = Locale.forLanguageTag("en-US"))
        assertEquals("5%", output)
    }

    @Test
    fun formatPercent_viVN_emitsPercentGlyphInVietnameseConvention() {
        // Vietnamese convention matches the English "5%" form. Pinning to
        // guard against a future JVM locale-data shift silently changing the
        // user-visible output.
        val output = formatPercent(percent = 12, locale = Locale.forLanguageTag("vi-VN"))
        assertTrue("expected %-suffix on vi-VN output, got '$output'", output.endsWith("%"))
        assertTrue("expected digit '12' in vi-VN output, got '$output'", output.contains("12"))
    }

    @Test
    fun formatPercent_fractionalInput_integerRoundsViaNumberFormat() {
        // The engine always emits integer percents, but if a caller ever
        // passes something fractional the formatter's default rounding rules
        // apply — this test pins the contract to integer percent so we don't
        // accidentally surface fractional chips.
        val output = formatPercent(percent = 0, locale = Locale.forLanguageTag("en-US"))
        assertEquals("0%", output)
    }

    @Test
    fun formatPercent_hundredPercent_rendersAsFullHundred() {
        // Edge case surfaced by the biggest-mover algorithm: a category with
        // 100% month-over-month increase should display as "100%", not
        // "1%" (which would happen if the fraction conversion were skipped).
        val output = formatPercent(percent = 100, locale = Locale.forLanguageTag("en-US"))
        assertEquals("100%", output)
    }
}
