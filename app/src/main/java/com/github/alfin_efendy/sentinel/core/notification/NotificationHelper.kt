package com.github.alfin_efendy.sentinel.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.github.alfin_efendy.sentinel.R
import com.github.alfin_efendy.sentinel.domain.model.MonitoringState
import com.github.alfin_efendy.sentinel.presentation.main.MainActivity
import com.github.alfin_efendy.sentinel.service.SentinelForegroundService

object NotificationHelper {

    const val CHANNEL_ID_MONITORING = "sentinel_monitoring"
    const val CHANNEL_ID_ALERTS = "sentinel_alerts"
    const val NOTIFICATION_ID_FOREGROUND = 1001
    const val NOTIFICATION_ID_ALERT = 1002

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val monitoringChannel = NotificationChannel(
            CHANNEL_ID_MONITORING,
            "Monitoring Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent notification while Sentinel is monitoring an app"
            setShowBadge(false)
        }

        val alertsChannel = NotificationChannel(
            CHANNEL_ID_ALERTS,
            "Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts for accessibility disconnected, app uninstalled, etc."
        }

        nm.createNotificationChannel(monitoringChannel)
        nm.createNotificationChannel(alertsChannel)
    }

    fun buildMonitoringNotification(
        context: Context,
        targetPackage: String,
        state: MonitoringState
    ): Notification {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (state) {
            is MonitoringState.Monitoring -> "Sentinel — Monitoring"
            is MonitoringState.Relaunching -> "Sentinel — Opening App (attempt ${state.attemptCount})"
            is MonitoringState.GracePeriod -> "Sentinel — Verifying..."
            is MonitoringState.Paused -> "Sentinel — Paused"
            else -> "Sentinel — Active"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_MONITORING)
            .setSmallIcon(R.drawable.ic_sentinel_notification)
            .setContentTitle(title)
            .setContentText("Watching: $targetPackage")
            .setContentIntent(tapPending)
            .setOngoing(true)
            .setSilent(true)

        if (state is MonitoringState.Paused) {
            val resumeIntent = Intent(context, SentinelForegroundService::class.java).apply {
                action = SentinelForegroundService.ACTION_RESUME_MONITORING
            }
            val resumePending = PendingIntent.getService(
                context, 1, resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Resume", resumePending)
        } else {
            val pauseIntent = Intent(context, SentinelForegroundService::class.java).apply {
                action = SentinelForegroundService.ACTION_PAUSE_MONITORING
            }
            val pausePending = PendingIntent.getService(
                context, 2, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Pause", pausePending)
        }

        val stopIntent = Intent(context, SentinelForegroundService::class.java).apply {
            action = SentinelForegroundService.ACTION_STOP_MONITORING
        }
        val stopPending = PendingIntent.getService(
            context, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "Stop", stopPending)

        return builder.build()
    }

    fun buildIdleNotification(context: Context): Notification {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_MONITORING)
            .setSmallIcon(R.drawable.ic_sentinel_notification)
            .setContentTitle("Sentinel — Idle")
            .setContentText("No target configured")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    fun showAlert(context: Context, title: String, message: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_sentinel_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID_ALERT, notification)
    }
}
