package dev.tuandoan.expensetracker.repository.mapper

import dev.tuandoan.expensetracker.data.database.entity.TripEntity
import dev.tuandoan.expensetracker.domain.model.Trip

fun TripEntity.toDomain(): Trip =
    Trip(
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

fun Trip.toEntity(): TripEntity =
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
