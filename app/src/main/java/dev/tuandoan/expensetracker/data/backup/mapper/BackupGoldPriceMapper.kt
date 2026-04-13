package dev.tuandoan.expensetracker.data.backup.mapper

import dev.tuandoan.expensetracker.data.backup.model.BackupGoldPriceDto
import dev.tuandoan.expensetracker.data.database.entity.GoldPriceEntity

fun GoldPriceEntity.toBackupDto(): BackupGoldPriceDto =
    BackupGoldPriceDto(
        type = type,
        unit = unit,
        pricePerUnit = pricePerUnit,
        buyBackPricePerUnit = buyBackPricePerUnit,
        currencyCode = currencyCode,
        updatedAt = updatedAt,
    )

fun BackupGoldPriceDto.toEntity(): GoldPriceEntity =
    GoldPriceEntity(
        type = type,
        unit = unit,
        pricePerUnit = pricePerUnit,
        buyBackPricePerUnit = buyBackPricePerUnit,
        currencyCode = currencyCode,
        updatedAt = updatedAt,
    )
