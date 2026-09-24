package dev.tuandoan.expensetracker.domain.model

/**
 * Domain representation of a trip's budget progress and daily allowance (v3.15.0).
 *
 * @property budgetAmount The total budget allocated for the trip in primary/home currency minor units.
 * @property spentAmount The total expenditure in primary/home currency minor units recorded for this trip so far.
 * @property dailyAllowance The calculated remaining daily allowance in home currency minor units,
 *                          or null if the trip is in the past.
 * @property remainingDays The number of days remaining on the trip (inclusive of today for active trips),
 *                         or 0 for past trips.
 */
data class TripBudgetStatus(
    val budgetAmount: Long,
    val spentAmount: Long,
    val dailyAllowance: Long?,
    val remainingDays: Int?,
) {
    val remainingAmount: Long get() = budgetAmount - spentAmount

    val progressFraction: Float
        get() =
            if (budgetAmount <= 0L) {
                0f
            } else {
                (spentAmount.toFloat() / budgetAmount.toFloat()).coerceAtLeast(0f)
            }

    val status: BudgetStatusLevel
        get() =
            when {
                progressFraction >= 1.0f -> BudgetStatusLevel.OVER_BUDGET
                progressFraction >= 0.8f -> BudgetStatusLevel.WARNING
                else -> BudgetStatusLevel.OK
            }

    companion object {
        /**
         * Calculates [TripBudgetStatus] from a trip, its total spend in home currency, and current date.
         *
         * Returns null if the trip has no budget (null or <= 0).
         */
        fun calculate(
            trip: Trip,
            spentAmount: Long,
            nowEpochDay: Long,
        ): TripBudgetStatus? {
            val budget = trip.budgetAmount ?: return null
            if (budget <= 0L) return null

            val startDate = trip.startDateEpochDay
            val endDate = trip.endDateEpochDay

            val (dailyAllowance, remainingDays) =
                when {
                    nowEpochDay < startDate -> {
                        val totalDays = (endDate - startDate + 1).toInt().coerceAtLeast(1)
                        val allowance = budget / totalDays
                        allowance to totalDays
                    }
                    nowEpochDay <= endDate -> {
                        val daysLeft = (endDate - nowEpochDay + 1).toInt().coerceAtLeast(1)
                        val remaining = budget - spentAmount
                        val allowance = if (remaining > 0L) remaining / daysLeft else 0L
                        allowance to daysLeft
                    }
                    else -> {
                        null to 0
                    }
                }

            return TripBudgetStatus(
                budgetAmount = budget,
                spentAmount = spentAmount,
                dailyAllowance = dailyAllowance,
                remainingDays = remainingDays,
            )
        }
    }
}
