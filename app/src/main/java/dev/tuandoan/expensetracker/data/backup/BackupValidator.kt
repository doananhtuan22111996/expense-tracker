package dev.tuandoan.expensetracker.data.backup

import dev.tuandoan.expensetracker.data.backup.model.BackupDocumentV1
import dev.tuandoan.expensetracker.data.backup.model.BackupGoldHoldingDto
import dev.tuandoan.expensetracker.data.backup.model.BackupGoldPriceDto
import dev.tuandoan.expensetracker.data.backup.model.BackupRecurringTransactionDto
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.domain.model.GoldType
import dev.tuandoan.expensetracker.domain.model.GoldWeightUnit
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

    data class DuplicateRecurringId(
        val id: Long,
    ) : BackupValidationError()

    data class InvalidRecurringAmount(
        val recurringId: Long,
        val amount: Long,
    ) : BackupValidationError()

    data class InvalidRecurringFrequency(
        val recurringId: Long,
        val frequency: Int,
    ) : BackupValidationError()

    data class BlankRecurringCurrencyCode(
        val recurringId: Long,
    ) : BackupValidationError()

    data class UnsupportedRecurringCurrencyCode(
        val recurringId: Long,
        val currencyCode: String,
    ) : BackupValidationError()

    data class OrphanedRecurringTransaction(
        val recurringId: Long,
        val categoryId: Long,
    ) : BackupValidationError()

    data class DuplicateGoldHoldingId(
        val id: Long,
    ) : BackupValidationError()

    data class InvalidGoldType(
        val goldHoldingId: Long,
        val type: String,
    ) : BackupValidationError()

    data class InvalidGoldWeightUnit(
        val goldHoldingId: Long,
        val unit: String,
    ) : BackupValidationError()

    data class NonPositiveGoldWeight(
        val goldHoldingId: Long,
        val weight: Double,
    ) : BackupValidationError()

    data class NegativeGoldPrice(
        val goldHoldingId: Long,
        val price: Long,
    ) : BackupValidationError()

    data class UnsupportedGoldCurrencyCode(
        val goldHoldingId: Long,
        val currencyCode: String,
    ) : BackupValidationError()

    data class DuplicateGoldPrice(
        val type: String,
        val unit: String,
    ) : BackupValidationError()

    data class InvalidGoldPriceType(
        val type: String,
    ) : BackupValidationError()

    data class InvalidGoldPriceUnit(
        val unit: String,
    ) : BackupValidationError()

    data class NegativeGoldPriceValue(
        val type: String,
        val unit: String,
        val price: Long,
    ) : BackupValidationError()

    data class UnsupportedGoldPriceCurrencyCode(
        val type: String,
        val unit: String,
        val currencyCode: String,
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

            // Validate recurring transactions
            validateRecurring(document.recurringTransactions, categoryIds, errors)

            // Validate gold holdings
            validateGoldHoldings(document.goldHoldings, errors)

            // Validate gold prices
            validateGoldPrices(document.goldPrices, errors)

            return if (errors.isEmpty()) {
                BackupValidationResult.Valid
            } else {
                BackupValidationResult.Invalid(errors)
            }
        }

        private val validGoldTypes = GoldType.entries.map { it.name }.toSet()
        private val validGoldWeightUnits = GoldWeightUnit.entries.map { it.name }.toSet()

        private fun validateGoldHoldings(
            goldHoldings: List<BackupGoldHoldingDto>,
            errors: MutableList<BackupValidationError>,
        ) {
            val goldIds = mutableSetOf<Long>()
            for (holding in goldHoldings) {
                if (!goldIds.add(holding.id)) {
                    errors.add(BackupValidationError.DuplicateGoldHoldingId(holding.id))
                }
                if (holding.type !in validGoldTypes) {
                    errors.add(BackupValidationError.InvalidGoldType(holding.id, holding.type))
                }
                if (holding.weightUnit !in validGoldWeightUnits) {
                    errors.add(BackupValidationError.InvalidGoldWeightUnit(holding.id, holding.weightUnit))
                }
                if (holding.weightValue <= 0) {
                    errors.add(BackupValidationError.NonPositiveGoldWeight(holding.id, holding.weightValue))
                }
                if (holding.buyPricePerUnit < 0) {
                    errors.add(BackupValidationError.NegativeGoldPrice(holding.id, holding.buyPricePerUnit))
                }
                if (SupportedCurrencies.byCode(holding.currencyCode) == null) {
                    errors.add(
                        BackupValidationError.UnsupportedGoldCurrencyCode(holding.id, holding.currencyCode),
                    )
                }
            }
        }

        private fun validateGoldPrices(
            goldPrices: List<BackupGoldPriceDto>,
            errors: MutableList<BackupValidationError>,
        ) {
            val seen = mutableSetOf<Pair<String, String>>()
            for (price in goldPrices) {
                val key = price.type to price.unit
                if (!seen.add(key)) {
                    errors.add(BackupValidationError.DuplicateGoldPrice(price.type, price.unit))
                }
                if (price.type !in validGoldTypes) {
                    errors.add(BackupValidationError.InvalidGoldPriceType(price.type))
                }
                if (price.unit !in validGoldWeightUnits) {
                    errors.add(BackupValidationError.InvalidGoldPriceUnit(price.unit))
                }
                if (price.pricePerUnit < 0) {
                    errors.add(
                        BackupValidationError.NegativeGoldPriceValue(price.type, price.unit, price.pricePerUnit),
                    )
                }
                if (SupportedCurrencies.byCode(price.currencyCode) == null) {
                    errors.add(
                        BackupValidationError.UnsupportedGoldPriceCurrencyCode(
                            price.type,
                            price.unit,
                            price.currencyCode,
                        ),
                    )
                }
            }
        }

        private fun validateRecurring(
            recurringTransactions: List<BackupRecurringTransactionDto>,
            categoryIds: Set<Long>,
            errors: MutableList<BackupValidationError>,
        ) {
            val recurringIds = mutableSetOf<Long>()
            for (recurring in recurringTransactions) {
                if (!recurringIds.add(recurring.id)) {
                    errors.add(BackupValidationError.DuplicateRecurringId(recurring.id))
                }
                if (recurring.amount <= 0) {
                    errors.add(
                        BackupValidationError.InvalidRecurringAmount(recurring.id, recurring.amount),
                    )
                }
                if (recurring.frequency !in 0..3) {
                    errors.add(
                        BackupValidationError.InvalidRecurringFrequency(
                            recurring.id,
                            recurring.frequency,
                        ),
                    )
                }
                if (recurring.currencyCode.isBlank()) {
                    errors.add(BackupValidationError.BlankRecurringCurrencyCode(recurring.id))
                } else if (SupportedCurrencies.byCode(recurring.currencyCode) == null) {
                    errors.add(
                        BackupValidationError.UnsupportedRecurringCurrencyCode(
                            recurring.id,
                            recurring.currencyCode,
                        ),
                    )
                }
                val catId = recurring.categoryId
                if (catId != null && catId !in categoryIds) {
                    errors.add(
                        BackupValidationError.OrphanedRecurringTransaction(recurring.id, catId),
                    )
                }
            }
        }
    }
