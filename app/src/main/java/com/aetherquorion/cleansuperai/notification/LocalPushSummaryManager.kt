package com.aetherquorion.cleansuperai.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aetherquorion.cleansuperai.MainActivity
import com.aetherquorion.cleansuperai.R

internal object LocalPushSummaryManager {
    const val GROUP_KEY = "local_push_group"
    const val SUMMARY_ID = 2001
    const val CHANNEL_ID = "local_push_summary_channel"

    fun sync(context: Context, manager: NotificationManager) {
        val children = manager.activeNotifications.filter {
            it.id != SUMMARY_ID && it.notification.group == GROUP_KEY
        }
        if (children.isEmpty()) {
            manager.cancel(SUMMARY_ID)
            return
        }
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID,
                context.getString(R.string.notif_event_summary_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                setShowBadge(false); enableVibration(false); setSound(null, null)
            })
        val intent = PendingIntent.getActivity(context, SUMMARY_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(SUMMARY_ID, NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.notification_accent))
            .setColorized(false)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentIntent(intent).setGroup(GROUP_KEY).setGroupSummary(true)
            .setAutoCancel(true).setOnlyAlertOnce(true).setShowWhen(false).build())
    }
}
