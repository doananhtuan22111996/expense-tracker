package dev.tuandoan.expensetracker.data.backup

import dev.tuandoan.expensetracker.data.backup.model.BackupCategoryDto
import dev.tuandoan.expensetracker.data.backup.model.BackupDocumentV1
import dev.tuandoan.expensetracker.data.backup.model.BackupGoldHoldingDto
import dev.tuandoan.expensetracker.data.backup.model.BackupRecurringTransactionDto
import dev.tuandoan.expensetracker.data.backup.model.BackupTransactionDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupAssembler
    @Inject
    constructor() {
        fun assemble(
            categories: List<BackupCategoryDto>,
            transactions: List<BackupTransactionDto>,
            recurringTransactions: List<BackupRecurringTransactionDto> = emptyList(),
            goldHoldings: List<BackupGoldHoldingDto> = emptyList(),
            defaultCurrencyCode: String,
            appVersionName: String,
            createdAtEpochMs: Long,
            deviceLocale: String,
        ): BackupDocumentV1 =
            BackupDocumentV1(
                appVersionName = appVersionName,
                createdAtEpochMs = createdAtEpochMs,
                defaultCurrencyCode = defaultCurrencyCode,
                deviceLocale = deviceLocale,
                categories = categories.sortedBy { it.id },
                transactions = transactions.sortedBy { it.id },
                recurringTransactions = recurringTransactions.sortedBy { it.id },
                goldHoldings = goldHoldings.sortedBy { it.id },
            )
    }
