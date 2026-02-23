package dev.tuandoan.expensetracker.data.backup

import dev.tuandoan.expensetracker.data.backup.model.BackupDocumentV1
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import javax.inject.Inject
import javax.inject.Singleton

sealed class BackupValidationResult {
    data object Valid : BackupValidationResult()

    data class Invalid(
        val errors: List<BackupValidationError>,
    ) : BackupValidationResult()
}

sealed class BackupValidationError {
    data class UnsupportedSchemaVersion(
        val version: Int,
    ) : BackupValidationError()

    data class DuplicateCategoryId(
        val id: Long,
    ) : BackupValidationError()

    data class DuplicateTransactionId(
        val id: Long,
    ) : BackupValidationError()

    data class BlankCategoryName(
        val categoryId: Long,
    ) : BackupValidationError()

    data class InvalidCategoryType(
        val categoryId: Long,
        val type: Int,
    ) : BackupValidationError()

    data class InvalidTransactionType(
        val transactionId: Long,
        val type: Int,
    ) : BackupValidationError()

    data class NegativeAmount(
        val transactionId: Long,
        val amount: Long,
    ) : BackupValidationError()

    data class UnsupportedCurrencyCode(
        val transactionId: Long,
        val currencyCode: String,
    ) : BackupValidationError()

    data class OrphanedTransaction(
        val transactionId: Long,
        val categoryId: Long,
    ) : BackupValidationError()
}

@Singleton
class BackupValidator
    @Inject
    constructor() {
        fun validate(document: BackupDocumentV1): BackupValidationResult {
            val errors = mutableListOf<BackupValidationError>()

            // Validate schema version
            if (document.schemaVersion != BackupDocumentV1.CURRENT_SCHEMA_VERSION) {
                errors.add(BackupValidationError.UnsupportedSchemaVersion(document.schemaVersion))
            }

            // Validate duplicate category IDs
            val categoryIds = mutableSetOf<Long>()
            for (category in document.categories) {
                if (!categoryIds.add(category.id)) {
                    errors.add(BackupValidationError.DuplicateCategoryId(category.id))
                }
            }

            // Validate duplicate transaction IDs
            val transactionIds = mutableSetOf<Long>()
            for (transaction in document.transactions) {
                if (!transactionIds.add(transaction.id)) {
                    errors.add(BackupValidationError.DuplicateTransactionId(transaction.id))
                }
            }

            // Validate category fields
            for (category in document.categories) {
                if (category.name.isBlank()) {
                    errors.add(BackupValidationError.BlankCategoryName(category.id))
                }
                if (category.type !in CategoryEntity.TYPE_EXPENSE..CategoryEntity.TYPE_INCOME) {
                    errors.add(BackupValidationError.InvalidCategoryType(category.id, category.type))
                }
            }

            // Validate transaction fields
            for (transaction in document.transactions) {
                if (transaction.type !in TransactionEntity.TYPE_EXPENSE..TransactionEntity.TYPE_INCOME) {
                    errors.add(
                        BackupValidationError.InvalidTransactionType(transaction.id, transaction.type),
                    )
                }
                if (transaction.amount < 0) {
                    errors.add(BackupValidationError.NegativeAmount(transaction.id, transaction.amount))
                }
                if (SupportedCurrencies.byCode(transaction.currencyCode) == null) {
                    errors.add(
                        BackupValidationError.UnsupportedCurrencyCode(
                            transaction.id,
                            transaction.currencyCode,
                        ),
                    )
                }
                if (transaction.categoryId !in categoryIds) {
                    errors.add(
                        BackupValidationError.OrphanedTransaction(transaction.id, transaction.categoryId),
                    )
                }
            }

            return if (errors.isEmpty()) {
                BackupValidationResult.Valid
            } else {
                BackupValidationResult.Invalid(errors)
            }
        }
    }
