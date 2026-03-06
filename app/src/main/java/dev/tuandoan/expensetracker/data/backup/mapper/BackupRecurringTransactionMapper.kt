package dev.tuandoan.expensetracker.data.backup.mapper

import dev.tuandoan.expensetracker.data.backup.model.BackupRecurringTransactionDto
import dev.tuandoan.expensetracker.data.database.entity.RecurringTransactionEntity

fun RecurringTransactionEntity.toBackupDto(): BackupRecurringTransactionDto =
    BackupRecurringTransactionDto(
        id = id,
        type = type,
        amount = amount,
        currencyCode = currencyCode,
        categoryId = categoryId,
        note = note,
        frequency = frequency,
        dayOfMonth = dayOfMonth,
        dayOfWeek = dayOfWeek,
        nextDueMillis = nextDueMillis,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun BackupRecurringTransactionDto.toEntity(): RecurringTransactionEntity =
    RecurringTransactionEntity(
        id = id,
        type = type,
        amount = amount,
        currencyCode = currencyCode,
        categoryId = categoryId,
        note = note,
        frequency = frequency,
        dayOfMonth = dayOfMonth,
        dayOfWeek = dayOfWeek,
        nextDueMillis = nextDueMillis,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
