package dev.tuandoan.expensetracker.domain.model

/** Controls whether transaction search is scoped to the current month or all time. */
enum class SearchScope {
    CURRENT_MONTH,
    ALL_MONTHS,
}
