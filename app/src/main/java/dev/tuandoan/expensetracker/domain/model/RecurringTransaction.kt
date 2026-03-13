package dev.tuandoan.expensetracker.domain.model

enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    ;

    fun toInt(): Int = ordinal

    companion object {
        fun fromInt(value: Int): RecurrenceFrequency = entries[value]
    }
}

data class RecurringTransaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    val currencyCode: String,
    val categoryId: Long?,
    val categoryName: String = "",
    val note: String?,
    val frequency: RecurrenceFrequency,
    val dayOfMonth: Int?,
    val dayOfWeek: Int?,
    val nextDueMillis: Long,
    val isActive: Boolean = true,
)
