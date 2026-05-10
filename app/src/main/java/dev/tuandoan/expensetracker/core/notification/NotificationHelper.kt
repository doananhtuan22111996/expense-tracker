package dev.tuandoan.expensetracker.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.MainActivity
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.ui.screen.quickadd.UndoQuickAddReceiver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        /**
         * Registers all notification channels owned by the app. Safe to call
         * multiple times — the OS treats re-registration of the same channel
         * ID as a no-op, so a fresh install and an upgrade run through the
         * same path without branching.
         */
        fun createChannels() {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(budgetAlertsChannel())
            manager.createNotificationChannel(quickAddConfirmationChannel())
        }

        private fun budgetAlertsChannel(): NotificationChannel =
            NotificationChannel(
                CHANNEL_BUDGET_ALERTS,
                context.getString(R.string.notification_channel_budget_alerts),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_budget_alerts_description)
            }

        /**
         * Channel for the v3.12.0 widget quick-add confirmation notification.
         * `IMPORTANCE_LOW` by design: the confirmation is a quiet "expense
         * added" reassurance with a 10-second Undo action — no sound, no
         * heads-up, visible only in the shade. Matches the Design doc's
         * decision table (LOW over MIN because MIN would hide the Undo
         * affordance from the shade).
         */
        private fun quickAddConfirmationChannel(): NotificationChannel =
            NotificationChannel(
                CHANNEL_QUICK_ADD_CONFIRMATION,
                context.getString(R.string.notification_channel_quick_add_confirmation),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_channel_quick_add_confirmation_description)
            }

        fun showBudgetAlert(
            title: String,
            message: String,
            notificationId: Int = NOTIFICATION_ID_BUDGET_WARNING,
        ) {
            if (!hasNotificationPermission()) return

            val intent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_BUDGET_ALERTS)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }

        fun hasNotificationPermission(): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        // ------------------------------------------------------------------
        // v3.12.0 quick-add confirmation (T4.2)
        //
        // Three-step lifecycle per ADR-012:
        //   1. showQuickAddConfirmation(...)                — immediate post with Undo action
        //   2. updateQuickAddConfirmationWithoutUndo(...)   — T+10s worker drops the Undo button
        //   3. cancelQuickAddConfirmation(...)              — T+30s worker removes the notification
        // All three operate on the SAME notification id (derived from the
        // transaction id via [quickAddNotificationId]) so the OS treats the
        // expiry update as update-in-place, not a second notification.
        // ------------------------------------------------------------------

        /**
         * Posts the quick-add confirmation notification with an Undo action
         * on `CHANNEL_QUICK_ADD_CONFIRMATION`. Returns the notification id
         * used; callers schedule expiry/dismiss workers against this id.
         *
         * If the user has denied `POST_NOTIFICATIONS` on A33+, silently
         * no-ops — the transaction is still saved; the user just doesn't
         * see the confirmation or Undo path. Matches the existing budget-
         * alerts policy; a permission-rationale surface is deliberately
         * out of scope for v3.12.0.
         *
         * Lock-screen visibility is `VISIBILITY_PRIVATE`: the OS redacts the
         * body (category name + amount) on a locked screen, showing only
         * the app name + "Notification". Matches the privacy-forward posture
         * of the app — category + amount together could infer activity
         * patterns an observer shouldn't see.
         */
        fun showQuickAddConfirmation(
            transactionId: Long,
            amountFormatted: String,
            categoryName: String,
        ): Int {
            val notificationId = quickAddNotificationId(transactionId)
            if (!hasNotificationPermission()) return notificationId

            val notification =
                buildQuickAddConfirmation(
                    transactionId = transactionId,
                    amountFormatted = amountFormatted,
                    categoryName = categoryName,
                    withUndo = true,
                )
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            return notificationId
        }

        /**
         * Re-posts the quick-add confirmation notification with the Undo
         * action removed. Called by [QuickAddNotifierWorker] at T+10s once
         * the Undo window has closed.
         *
         * Same notification id as the original post → OS update-in-place.
         * If the user has already swiped the notification away, this
         * effectively re-shows it without Undo — a rare edge case accepted
         * in v3.12.0's Design doc as the cost of the low-infrastructure
         * 10-second window. Documented in T7.1 manual QA.
         */
        fun updateQuickAddConfirmationWithoutUndo(
            notificationId: Int,
            amountFormatted: String,
            categoryName: String,
        ) {
            if (!hasNotificationPermission()) return
            val notification =
                buildQuickAddConfirmation(
                    transactionId = 0L, // not used on the no-undo path
                    amountFormatted = amountFormatted,
                    categoryName = categoryName,
                    withUndo = false,
                )
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }

        /**
         * Cancels the quick-add confirmation notification. Called by
         * [QuickAddNotifierWorker] at T+30s for auto-dismiss, and by T4.3's
         * Undo receiver immediately before it posts the "Undone" update.
         */
        fun cancelQuickAddConfirmation(notificationId: Int) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        /**
         * Replaces the quick-add confirmation notification with an "Undone"
         * acknowledgment (v3.12.0 T4.3). Posted on the same `notificationId`
         * as the original "Expense added" post, so the OS treats this as
         * update-in-place rather than a second notification.
         *
         * Body is static copy ("Transaction removed") — no PendingIntent,
         * no action buttons, no category name. A separate 3-second dismiss
         * worker clears it from the shade automatically.
         *
         * Silent-by-design for the same reasons as the original post:
         * `setSilent(true)` + `setVisibility(VISIBILITY_PRIVATE)` keep the
         * update quiet and lock-screen-safe even if the user manually
         * promoted the channel's importance.
         */
        fun updateQuickAddConfirmationToUndone(notificationId: Int) {
            if (!hasNotificationPermission()) return
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_QUICK_ADD_CONFIRMATION)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(context.getString(R.string.notification_quick_add_undone_title))
                    .setContentText(context.getString(R.string.notification_quick_add_undone_body))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setAutoCancel(true)
                    .setSilent(true)
                    .build()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }

        /**
         * Build the `NotificationCompat.Builder` used by both the initial
         * post and the T+10s no-undo update. Extracted so the two call
         * sites stay byte-identical except for the Undo action presence.
         */
        private fun buildQuickAddConfirmation(
            transactionId: Long,
            amountFormatted: String,
            categoryName: String,
            withUndo: Boolean,
        ): android.app.Notification {
            val bodyText =
                context.getString(
                    R.string.notification_quick_add_body,
                    amountFormatted,
                    categoryName,
                )
            val titleText = context.getString(R.string.notification_quick_add_title)

            val builder =
                NotificationCompat
                    .Builder(context, CHANNEL_QUICK_ADD_CONFIRMATION)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(titleText)
                    .setContentText(bodyText)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setAutoCancel(true)
                    // Suppress sound/vibration even if the user manually
                    // promoted the channel's importance — the confirmation
                    // is silent by design.
                    .setSilent(true)

            if (withUndo) {
                val undoIntent =
                    Intent(context, UndoQuickAddReceiver::class.java).apply {
                        action = UndoQuickAddReceiver.ACTION_UNDO
                        putExtra(UndoQuickAddReceiver.EXTRA_TRANSACTION_ID, transactionId)
                    }
                // Request code = notification id so each in-flight undo has
                // its own PendingIntent; FLAG_UPDATE_CURRENT ensures the
                // latest extras win if a collision somehow occurs.
                val undoPendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        quickAddNotificationId(transactionId),
                        undoIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                builder.addAction(
                    0,
                    context.getString(R.string.notification_quick_add_undo_action),
                    undoPendingIntent,
                )
            }

            return builder.build()
        }

        companion object {
            const val CHANNEL_BUDGET_ALERTS = "budget_alerts"
            const val CHANNEL_QUICK_ADD_CONFIRMATION = "quick_add_confirmation"
            const val NOTIFICATION_ID_BUDGET_WARNING = 1001
            const val NOTIFICATION_ID_BUDGET_EXCEEDED = 1002

            /**
             * Maps an auto-increment transaction id (Long) to a notification
             * id (Int) for the quick-add confirmation. Uses the low 31 bits
             * so every reasonable primary-key value stays positive and
             * unique. Values could theoretically collide after ~2 billion
             * inserts; users in that regime have bigger problems.
             */
            fun quickAddNotificationId(transactionId: Long): Int = transactionId.and(Int.MAX_VALUE.toLong()).toInt()
        }
    }
