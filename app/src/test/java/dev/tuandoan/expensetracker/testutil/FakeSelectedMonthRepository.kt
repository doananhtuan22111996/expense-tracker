package dev.tuandoan.expensetracker.testutil

import dev.tuandoan.expensetracker.domain.repository.SelectedMonthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.YearMonth

class FakeSelectedMonthRepository(
    initialMonth: YearMonth = YearMonth.of(2026, 3),
) : SelectedMonthRepository {
    private val _selectedMonth = MutableStateFlow(initialMonth)
    override val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    override fun setMonth(yearMonth: YearMonth) {
        _selectedMonth.value = yearMonth
    }

    override fun goToPreviousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    override fun goToNextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }
}
