package dev.tuandoan.expensetracker.domain.model

data class Transaction(
    val id: Long = 0L,
    val type: TransactionType,
    val amount: Long,
    val category: Category,
    val note: String? = null,
    val timestamp: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class TransactionType {
    EXPENSE,
    INCOME,
    ;

    fun toInt(): Int =
        when (this) {
            EXPENSE -> 0
            INCOME -> 1
        }

    companion object {
        fun fromInt(value: Int): TransactionType =
            when (value) {
                0 -> EXPENSE
                1 -> INCOME
                else -> throw IllegalArgumentException("Invalid transaction type: $value")
            }
    }
}
