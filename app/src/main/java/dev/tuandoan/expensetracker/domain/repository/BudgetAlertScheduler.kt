package dev.tuandoan.expensetracker.domain.repository

/**
 * Schedules an immediate budget alert check.
 * Used after transaction mutations to evaluate budget thresholds without delay.
 */
interface BudgetAlertScheduler {
    fun scheduleImmediateCheck()
}
