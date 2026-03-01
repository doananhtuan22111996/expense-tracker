package dev.tuandoan.expensetracker.domain.repository

import kotlinx.coroutines.flow.StateFlow
import java.time.YearMonth

interface SelectedMonthRepository {
    val selectedMonth: StateFlow<YearMonth>

    fun setMonth(yearMonth: YearMonth)

    fun goToPreviousMonth()

    fun goToNextMonth()
}
