package dev.tuandoan.expensetracker.data.backup.mapper

import dev.tuandoan.expensetracker.data.backup.model.BackupCategoryDto
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity

fun CategoryEntity.toBackupDto(): BackupCategoryDto =
    BackupCategoryDto(
        id = id,
        name = name,
        type = type,
        iconKey = iconKey,
        colorKey = colorKey,
        isDefault = isDefault,
    )

fun BackupCategoryDto.toEntity(): CategoryEntity =
    CategoryEntity(
        id = id,
        name = name,
        type = type,
        iconKey = iconKey,
        colorKey = colorKey,
        isDefault = isDefault,
    )
