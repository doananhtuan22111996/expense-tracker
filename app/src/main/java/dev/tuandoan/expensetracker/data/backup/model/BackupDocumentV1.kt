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
)

@Serializable
data class BackupDocumentV1(
    @SerialName("schema_version") val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    @SerialName("app_version_name") val appVersionName: String,
    @SerialName("created_at_epoch_ms") val createdAtEpochMs: Long,
    @SerialName("categories") val categories: List<BackupCategoryDto>,
    @SerialName("transactions") val transactions: List<BackupTransactionDto>,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
