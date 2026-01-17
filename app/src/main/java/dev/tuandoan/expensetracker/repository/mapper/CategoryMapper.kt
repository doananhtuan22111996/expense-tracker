package dev.tuandoan.expensetracker.repository.mapper

import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.TransactionType

fun CategoryEntity.toDomain(): Category =
    Category(
        id = id,
        name = name,
        type = TransactionType.fromInt(type),
        iconKey = iconKey,
        colorKey = colorKey,
        isDefault = isDefault,
    )

fun Category.toEntity(): CategoryEntity =
    CategoryEntity(
        id = id,
        name = name,
        type = type.toInt(),
        iconKey = iconKey,
        colorKey = colorKey,
        isDefault = isDefault,
    )
