package dev.tuandoan.expensetracker.data.backup.mapper

import dev.tuandoan.expensetracker.data.backup.model.BackupGoldHoldingDto
import dev.tuandoan.expensetracker.data.database.entity.GoldHoldingEntity

fun GoldHoldingEntity.toBackupDto(): BackupGoldHoldingDto =
    BackupGoldHoldingDto(
        id = id,
        type = type,
        weightValue = weightValue,
        weightUnit = weightUnit,
        buyPricePerUnit = buyPricePerUnit,
        currencyCode = currencyCode,
        buyDateMillis = buyDateMillis,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun BackupGoldHoldingDto.toEntity(): GoldHoldingEntity =
    GoldHoldingEntity(
        id = id,
        type = type,
        weightValue = weightValue,
        weightUnit = weightUnit,
        buyPricePerUnit = buyPricePerUnit,
        currencyCode = currencyCode,
        buyDateMillis = buyDateMillis,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
