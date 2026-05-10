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

        companion object {
            const val CHANNEL_BUDGET_ALERTS = "budget_alerts"
            const val CHANNEL_QUICK_ADD_CONFIRMATION = "quick_add_confirmation"
            const val NOTIFICATION_ID_BUDGET_WARNING = 1001
            const val NOTIFICATION_ID_BUDGET_EXCEEDED = 1002
        }
    }
