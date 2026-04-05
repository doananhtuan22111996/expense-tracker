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
        fun createChannels() {
            val channel =
                NotificationChannel(
                    CHANNEL_BUDGET_ALERTS,
                    context.getString(R.string.notification_channel_budget_alerts),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.notification_channel_budget_alerts_description)
                }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
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
            const val NOTIFICATION_ID_BUDGET_WARNING = 1001
            const val NOTIFICATION_ID_BUDGET_EXCEEDED = 1002
        }
    }
