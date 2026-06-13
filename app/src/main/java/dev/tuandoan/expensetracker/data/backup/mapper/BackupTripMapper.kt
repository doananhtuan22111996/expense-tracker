package dev.tuandoan.expensetracker.data.backup.mapper

import dev.tuandoan.expensetracker.data.backup.model.BackupTripDto
import dev.tuandoan.expensetracker.data.database.entity.TripEntity

fun TripEntity.toBackupDto(): BackupTripDto =
    BackupTripDto(
        id = id,
        name = name,
        destination = destination,
        startDateEpochDay = startDateEpochDay,
        endDateEpochDay = endDateEpochDay,
        foreignCurrencyCode = foreignCurrencyCode,
        foreignToHomeRate = foreignToHomeRate,
        originalCategoryId = originalCategoryId,
        originalCategoryNameSnapshot = originalCategoryNameSnapshot,
        originalCategoryIconSnapshot = originalCategoryIconSnapshot,
        originalCategoryColorSnapshot = originalCategoryColorSnapshot,
        createdAt = createdAt,
    )

fun BackupTripDto.toEntity(): TripEntity =
    TripEntity(
        id = id,
        name = name,
        destination = destination,
        startDateEpochDay = startDateEpochDay,
        endDateEpochDay = endDateEpochDay,
        foreignCurrencyCode = foreignCurrencyCode,
        foreignToHomeRate = foreignToHomeRate,
        originalCategoryId = originalCategoryId,
        originalCategoryNameSnapshot = originalCategoryNameSnapshot,
        originalCategoryIconSnapshot = originalCategoryIconSnapshot,
        originalCategoryColorSnapshot = originalCategoryColorSnapshot,
        createdAt = createdAt,
    )
