package com.aetherquorion.cleansuperai.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.Calendar

/** Starts only the notification producers; no widget lifecycle hooks are installed. */
object LocalEventPushBootstrap {
    private val handler = Handler(Looper.getMainLooper())
    private var started = false
    private var receiver: BroadcastReceiver? = null
    @Synchronized fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        LocalEventPushHelper.ensureChannel(app)
        LocalPushRepeatSubmitter.trackForeground()
        AppInactiveState.startTracking(app)
        ScreenshotCaptureWatcher.start(app)
        PackageChangeReceiver.registerDynamically(app)
        BatteryStatusWatcher.start(app)
        NetworkChangeWatcher.start(app)
        ScreenStateWatcher.start(app)
        AppBackgroundPushWatcher.start(app)
        MediaStoreDeleteWatcher.start(app)
        DownloadCompleteWatcher.start(app)
        MediaStoreDownloadWatcher.start(app)
        handler.postDelayed({
            if (CleanReminderState.status(app).shouldShow) {
                LocalEventPushHelper.show(app, "clean_reminder")
            }
            if (AppInactiveState.status(app).shouldShow) {
                LocalEventPushHelper.show(app, "app_inactive")
            }
        }, 1_500L)
        schedulePeriodic(app)
        scheduleFixed(app)
        ScheduledPushRecoveryJob.ensureScheduled(app)
        ScheduledPushDispatcher.recover(app)
    }

    fun restartSchedules(context: Context) {
        handler.removeCallbacksAndMessages(null)
        schedulePeriodic(context.applicationContext)
        scheduleFixed(context.applicationContext)
        ScheduledPushRecoveryJob.ensureScheduled(context.applicationContext)
        ScheduledPushDispatcher.recover(context.applicationContext)
    }

    private fun schedulePeriodic(context: Context) {
        LocalPushConfig.current(context).buckets.forEach { (key, bucket) ->
            if (!bucket.enabled || bucket.intervalSec < 0) return@forEach
            val interval = (bucket.intervalSec * 1000L).coerceAtLeast(1000L)
            fun tick() {
                LocalPushConfig.current(context).scenes.entries
                    .filter { it.value.enabled && it.value.periodic && it.value.bucket == key && it.value.times.isEmpty() }
                    .sortedByDescending { it.value.priority }
                    .firstOrNull { entry ->
                        val ready = when (entry.key) {
                            "app_inactive" -> AppInactiveState.status(context).shouldShow
                            "clean_reminder" -> CleanReminderState.status(context).shouldShow
                            "storage_free_drop" -> StorageFreeDropState.evaluateAndCommit(context).shouldShow
                            else -> true
                        }
                        ready && LocalEventPushHelper.show(context, entry.key)
                    }
                handler.postDelayed({ tick() }, interval)
            }
            handler.postDelayed({ tick() }, interval)
        }
    }

    private fun scheduleFixed(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        LocalPushConfig.current(context).scenes.forEach { (key, scene) ->
            repeat(8) { index ->
                val stale = PendingIntent.getBroadcast(context, key.hashCode() * 31 + index,
                    Intent(context, LocalPushAlarmReceiver::class.java),
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                stale?.let { alarm?.cancel(it); it.cancel() }
            }
            if (!scene.enabled) return@forEach
            scene.times.take(8).forEachIndexed { index, time ->
                val hour = time.substringBefore(':').toIntOrNull() ?: return@forEachIndexed
                val minute = time.substringAfter(':').toIntOrNull() ?: return@forEachIndexed
                if (hour !in 0..23 || minute !in 0..59) return@forEachIndexed
                val whenAt = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis - System.currentTimeMillis() <= 5_000L) add(Calendar.DATE, 1)
                }.timeInMillis
                val intent = Intent(context, LocalPushAlarmReceiver::class.java)
                    .putExtra("scene", key).putExtra("index", index).putExtra("time", time.trim())
                val pending = PendingIntent.getBroadcast(context, key.hashCode() * 31 + index, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                alarm?.cancel(pending)
                if (Build.VERSION.SDK_INT < 31 || alarm?.canScheduleExactAlarms() == true) {
                    alarm?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pending)
                } else alarm?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pending)
            }
        }
    }

    fun scheduleNextForSlot(context: Context, sceneKey: String, index: Int, time: String) {
        val app = context.applicationContext
        val config = LocalPushConfig.current(app)
        val scene = config.scenes[sceneKey] ?: return
        if (!config.enabled || !scene.enabled || scene.times.getOrNull(index)?.trim() != time) return
        val hour = time.substringBefore(':').toIntOrNull() ?: return
        val minute = time.substringAfter(':').toIntOrNull() ?: return
        if (hour !in 0..23 || minute !in 0..59) return
        val alarm = app.getSystemService(AlarmManager::class.java) ?: return
        val next = Calendar.getInstance().apply {
            add(Calendar.DATE, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val intent = Intent(app, LocalPushAlarmReceiver::class.java)
            .putExtra("scene", sceneKey).putExtra("index", index).putExtra("time", time)
        val pending = PendingIntent.getBroadcast(app, sceneKey.hashCode() * 31 + index, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms())
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending)
        else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending)
    }
}

class LocalPushAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scene = intent.getStringExtra("scene") ?: return
        val index = intent.getIntExtra("index", -1)
        val time = intent.getStringExtra("time").orEmpty()
        ScheduledPushDispatcher.onAlarm(context, scene, index, time)
        LocalEventPushBootstrap.scheduleNextForSlot(context, scene, index, time)
    }
}
