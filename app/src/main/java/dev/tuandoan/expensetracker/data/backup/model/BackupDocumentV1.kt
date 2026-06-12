package dev.tuandoan.expensetracker.data.backup.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackupCategoryDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("type") val type: Int,
    @SerialName("icon_key") val iconKey: String? = null,
    @SerialName("color_key") val colorKey: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
)

@Serializable
data class BackupTransactionDto(
    @SerialName("id") val id: Long,
    @SerialName("type") val type: Int,
    @SerialName("amount") val amount: Long,
    @SerialName("currency_code") val currencyCode: String,
    @SerialName("category_id") val categoryId: Long,
    @SerialName("note") val note: String? = null,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("trip_id") val tripId: Long? = null,
    @SerialName("amount_foreign_minor") val amountForeignMinor: Long? = null,
)

@Serializable
data class BackupTripDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("destination") val destination: String? = null,
    @SerialName("start_date_epoch_day") val startDateEpochDay: Long,
    @SerialName("end_date_epoch_day") val endDateEpochDay: Long,
    @SerialName("foreign_currency_code") val foreignCurrencyCode: String? = null,
    @SerialName("foreign_to_home_rate") val foreignToHomeRate: Double? = null,
    @SerialName("original_category_id") val originalCategoryId: Long? = null,
    @SerialName("original_category_name_snapshot") val originalCategoryNameSnapshot: String? = null,
    @SerialName("original_category_icon_snapshot") val originalCategoryIconSnapshot: String? = null,
    @SerialName("original_category_color_snapshot") val originalCategoryColorSnapshot: String? = null,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class BackupRecurringTransactionDto(
    @SerialName("id") val id: Long,
    @SerialName("type") val type: Int,
    @SerialName("amount") val amount: Long,
    @SerialName("currency_code") val currencyCode: String,
    @SerialName("category_id") val categoryId: Long? = null,
    @SerialName("note") val note: String? = null,
    @SerialName("frequency") val frequency: Int,
    @SerialName("day_of_month") val dayOfMonth: Int? = null,
    @SerialName("day_of_week") val dayOfWeek: Int? = null,
    @SerialName("next_due_millis") val nextDueMillis: Long,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class BackupDocumentV1(
    @SerialName("schema_version") val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    @SerialName("app_version_name") val appVersionName: String,
    @SerialName("created_at_epoch_ms") val createdAtEpochMs: Long,
    @SerialName("default_currency_code") val defaultCurrencyCode: String = "",
    @SerialName("device_locale") val deviceLocale: String = "",
    @SerialName("categories") val categories: List<BackupCategoryDto>,
    @SerialName("transactions") val transactions: List<BackupTransactionDto>,
    @SerialName("recurring_transactions") val recurringTransactions: List<BackupRecurringTransactionDto> = emptyList(),
    @SerialName("gold_holdings") val goldHoldings: List<BackupGoldHoldingDto> = emptyList(),
    @SerialName("gold_prices") val goldPrices: List<BackupGoldPriceDto> = emptyList(),
    @SerialName("trips") val trips: List<BackupTripDto> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
