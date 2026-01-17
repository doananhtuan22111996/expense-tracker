package dev.tuandoan.expensetracker.repository.mapper

import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType

fun TransactionEntity.toDomain(category: Category): Transaction =
    Transaction(
        id = id,
        type = TransactionType.fromInt(type),
        amount = amount,
        category = category,
        note = note,
        timestamp = timestamp,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Transaction.toEntity(): TransactionEntity =
    TransactionEntity(
        id = id,
        type = type.toInt(),
        amount = amount,
        categoryId = category.id,
        note = note,
        timestamp = timestamp,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
