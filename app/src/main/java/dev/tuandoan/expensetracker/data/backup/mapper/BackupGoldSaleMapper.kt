package dev.tuandoan.expensetracker.data.backup.mapper

import dev.tuandoan.expensetracker.data.backup.model.BackupGoldSaleDto
import dev.tuandoan.expensetracker.data.database.entity.GoldSaleEntity

fun GoldSaleEntity.toBackupDto(): BackupGoldSaleDto =
    BackupGoldSaleDto(
        id = id,
        holdingId = holdingId,
        type = type,
        soldWeight = soldWeight,
        weightUnit = weightUnit,
        buyPricePerUnit = buyPricePerUnit,
        sellPricePerUnit = sellPricePerUnit,
        currencyCode = currencyCode,
        saleDateMillis = saleDateMillis,
        note = note,
        createdAt = createdAt,
    )

fun BackupGoldSaleDto.toEntity(): GoldSaleEntity =
    GoldSaleEntity(
        id = id,
        holdingId = holdingId,
        type = type,
        soldWeight = soldWeight,
        weightUnit = weightUnit,
        buyPricePerUnit = buyPricePerUnit,
        sellPricePerUnit = sellPricePerUnit,
        currencyCode = currencyCode,
        saleDateMillis = saleDateMillis,
        note = note,
        createdAt = createdAt,
    )
