package dev.tuandoan.expensetracker.data.preferences

import dev.tuandoan.expensetracker.core.util.DateRangeCalculator
import dev.tuandoan.expensetracker.domain.repository.SelectedMonthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedMonthRepositoryImpl
    @Inject
    constructor(
        private val dateRangeCalculator: DateRangeCalculator,
    ) : SelectedMonthRepository {
        private val _selectedMonth = MutableStateFlow(dateRangeCalculator.currentMonth())
        override val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

        override fun setMonth(yearMonth: YearMonth) {
            _selectedMonth.value = yearMonth
        }

        override fun goToPreviousMonth() {
            _selectedMonth.value = dateRangeCalculator.previousMonth(_selectedMonth.value)
        }

        override fun goToNextMonth() {
            _selectedMonth.value = dateRangeCalculator.nextMonth(_selectedMonth.value)
        }
    }
