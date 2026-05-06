package dev.tuandoan.expensetracker.domain.analytics

import dev.tuandoan.expensetracker.domain.model.TransactionType

/**
 * Maps a domain-layer [TransactionType] to its analytics wire enum
 * [TransactionKind]. Kept decoupled so future `TransactionType` growth
 * (e.g. a hypothetical `TRANSFER` variant) forces an explicit mapping
 * decision here — and in turn a privacy-policy + PRD-FR-A6 review if the
 * new type should ship to the dashboard.
 *
 * The `when` over [TransactionType] is compiler-checked exhaustive.
 */
internal fun TransactionType.toAnalyticsKind(): TransactionKind =
    when (this) {
        TransactionType.EXPENSE -> TransactionKind.EXPENSE
        TransactionType.INCOME -> TransactionKind.INCOME
    }
