package dev.tuandoan.expensetracker.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-defined Trip. Travel-time container for transactions.
 *
 * Snapshot fields capture the source category at conversion time so the Trip can be
 * reverted (ADR-001). Non-conversion-origin trips leave them null.
 *
 * No SQLite-level FK to `categories` on `original_category_id` — the original row may
 * have been deleted on commit; revert restores from snapshot. No FK from `transactions`
 * to `trips` either; `TripRepository.deleteTrip` enforces ON DELETE SET NULL semantics
 * in code via `TransactionRunner.runInTransaction`.
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "destination")
    val destination: String?,
    @ColumnInfo(name = "start_date_epoch_day")
    val startDateEpochDay: Long,
    @ColumnInfo(name = "end_date_epoch_day")
    val endDateEpochDay: Long,
    @ColumnInfo(name = "foreign_currency_code")
    val foreignCurrencyCode: String?,
    @ColumnInfo(name = "foreign_to_home_rate")
    val foreignToHomeRate: Double?,
    @ColumnInfo(name = "original_category_id")
    val originalCategoryId: Long?,
    @ColumnInfo(name = "original_category_name_snapshot")
    val originalCategoryNameSnapshot: String?,
    @ColumnInfo(name = "original_category_icon_snapshot")
    val originalCategoryIconSnapshot: String?,
    @ColumnInfo(name = "original_category_color_snapshot")
    val originalCategoryColorSnapshot: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long, // Epoch millis (matches TransactionEntity / RecurringTransactionEntity / Gold* convention)
)
