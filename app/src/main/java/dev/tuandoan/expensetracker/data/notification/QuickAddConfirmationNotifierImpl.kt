package dev.tuandoan.expensetracker.data.notification

import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.domain.notification.QuickAddConfirmationNotifier
import dev.tuandoan.expensetracker.domain.notification.QuickAddNotificationSurface
import dev.tuandoan.expensetracker.domain.notification.QuickAddUndoWorkScheduler
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [QuickAddConfirmationNotifier].
 *
 * Depends on narrow domain interfaces ([QuickAddNotificationSurface] +
 * [QuickAddUndoWorkScheduler]) rather than the concrete `NotificationHelper`
 * + `WorkManager` — keeps the orchestration testable at the JVM unit-test
 * level (see T4.4's `QuickAddConfirmationNotifierImplTest`).
 *
 * Per [ADR-012](https://www.notion.so/35cb1772541381088642c3aea93f7fd7):
 * the scheduler schedules two expedited workers at T+10s (Mode.EXPIRE) +
 * T+30s (Mode.DISMISS), both tagged `quick-add-notify-{transactionId}`.
 */
@Singleton
class QuickAddConfirmationNotifierImpl
    @Inject
    constructor(
        private val notificationSurface: QuickAddNotificationSurface,
        private val workScheduler: QuickAddUndoWorkScheduler,
        private val currencyFormatter: CurrencyFormatter,
    ) : QuickAddConfirmationNotifier {
        override suspend fun post(
            transactionId: Long,
            amountMinor: Long,
            currencyCode: String,
            categoryName: String,
        ) {
            val amountFormatted = currencyFormatter.format(amountMinor, currencyCode)
            val notificationId =
                notificationSurface.showQuickAddConfirmation(
                    transactionId = transactionId,
                    amountFormatted = amountFormatted,
                    categoryName = categoryName,
                )
            workScheduler.scheduleExpiryAndDismiss(
                transactionId = transactionId,
                notificationId = notificationId,
                amountFormatted = amountFormatted,
                categoryName = categoryName,
            )
        }
    }
