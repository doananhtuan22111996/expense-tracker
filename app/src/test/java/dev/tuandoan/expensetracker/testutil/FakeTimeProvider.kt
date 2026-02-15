package dev.tuandoan.expensetracker.testutil

import dev.tuandoan.expensetracker.core.util.TimeProvider

class FakeTimeProvider(
    private var currentMillis: Long = 1700000000000L,
    private var monthRange: Pair<Long, Long> = 1700000000000L to 1702592000000L,
) : TimeProvider {
    override fun currentTimeMillis(): Long = currentMillis

    override fun currentMonthRange(): Pair<Long, Long> = monthRange

    fun setCurrentMillis(millis: Long) {
        currentMillis = millis
    }

    fun setMonthRange(range: Pair<Long, Long>) {
        monthRange = range
    }
}
