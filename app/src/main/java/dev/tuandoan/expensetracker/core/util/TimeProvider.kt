package dev.tuandoan.expensetracker.core.util

import javax.inject.Inject
import javax.inject.Singleton

interface TimeProvider {
    fun currentTimeMillis(): Long

    fun currentMonthRange(): Pair<Long, Long>
}

@Singleton
class SystemTimeProvider
    @Inject
    constructor() : TimeProvider {
        override fun currentTimeMillis(): Long = System.currentTimeMillis()

        override fun currentMonthRange(): Pair<Long, Long> = DateTimeUtil.getCurrentMonthRange()
    }
