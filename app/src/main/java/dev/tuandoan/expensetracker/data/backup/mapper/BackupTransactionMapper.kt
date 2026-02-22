package dev.tuandoan.expensetracker.data.backup.mapper

import dev.tuandoan.expensetracker.data.backup.model.BackupTransactionDto
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity

fun TransactionEntity.toBackupDto(): BackupTransactionDto =
    BackupTransactionDto(
        id = id,
        type = type,
        amount = amount,
        currencyCode = currencyCode,
        categoryId = categoryId,
        note = note,
        timestamp = timestamp,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun BackupTransactionDto.toEntity(): TransactionEntity =
    TransactionEntity(
        id = id,
        type = type,
        amount = amount,
        currencyCode = currencyCode,
        categoryId = categoryId,
        note = note,
        timestamp = timestamp,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
