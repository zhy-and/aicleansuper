package com.aetherquorion.cleansuperai.keepalive

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.UserManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aetherquorion.cleansuperai.MainActivity
import com.aetherquorion.cleansuperai.R

/** Independent foreground notification surface; routes to the current app home screen. */
class NotificationToolbarService : Service() {
    private var rejected = false
    private var channelName: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val speedUpdate = object : Runnable {
        override fun run() {
            if (!rejected) {
                runCatching {
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
                }
                handler.postDelayed(this, 7_000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        promote()
        handler.postDelayed(speedUpdate, 7_000L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DELETE) {
            if (KeepAliveConfig.enabled(this, "kpa_delete_intent_switch"))
                KeepAliveCoordinator.bootstrap(this, KeepAliveReason.NOTIFICATION_DELETE)
        }
        return if (promote()) START_STICKY else START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!rejected) KeepAliveCoordinator.bootstrap(this, KeepAliveReason.SERVICE_TASK_REMOVED)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        handler.removeCallbacks(speedUpdate)
        if (!rejected && isUserUnlocked()) KeepAliveCoordinator.bootstrap(this, KeepAliveReason.SERVICE_DESTROY)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun promote(): Boolean {
        if (rejected) return false
        return runCatching {
            startForeground(NOTIFICATION_ID, buildNotification())
            true
        }.getOrElse {
            rejected = true
            Log.w("KeepAlive", "Foreground notification rejected", it)
            stopSelf()
            false
        }
    }

    private fun buildNotification(): Notification {
        val localized = ContextCompat.getContextForLanguage(this)
        updateChannelName(localized.getString(R.string.home_quick_tools))
        val speed = NetworkSpeedTracker.sample()
        val speedUnit = localized.getString(R.string.notif_speed_unit)
        fun view(layout: Int, expanded: Boolean): RemoteViews = RemoteViews(packageName, layout).apply {
            setTextViewText(R.id.notif_speed_value, speed.value)
            setTextViewText(R.id.notif_speed_unit, speedUnit)
            setTextViewText(R.id.notif_label_cache, localized.getString(R.string.notif_junk))
            setTextViewText(R.id.notif_label_photos, localized.getString(R.string.photo_photos_label))
            setTextViewText(R.id.notif_label_apps, localized.getString(R.string.notif_apps))
            setTextViewText(R.id.notif_label_large, localized.getString(R.string.notif_large_file))
            setOnClickPendingIntent(R.id.notif_action_cache, route("cache", 1))
            setOnClickPendingIntent(R.id.notif_action_photos, route("picture_clean", 2))
            setOnClickPendingIntent(R.id.notif_action_apps, route("apps", 3))
            setOnClickPendingIntent(R.id.notif_action_large, route("large", 4))
            if (expanded) {
                setTextViewText(R.id.notif_label_clean, localized.getString(R.string.notif_clean_now))
                setOnClickPendingIntent(R.id.notif_action_clean, route("cache", 5))
            }
        }
        val now = System.currentTimeMillis()
        val pinWhen = com.tencent.mmkv.MMKV.defaultMMKV().getString("notification_pin_when", "1") == "1"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(localized.getString(R.string.app_name))
            .setContentText(speed.fullText)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(view(R.layout.notification_toolbar_collapsed, false))
            .setCustomBigContentView(view(R.layout.notification_toolbar_expanded, true))
            .setContentIntent(route("cache", 0))
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setShowWhen(false).setWhen(if (pinWhen) now + 2L * 365 * 24 * 60 * 60 * 1000 else now)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .apply {
                if (isUserUnlocked() && KeepAliveConfig.enabled(this@NotificationToolbarService, "kpa_delete_intent_switch")) {
                    val deleted = Intent(this@NotificationToolbarService, NotificationToolbarService::class.java).setAction(ACTION_DELETE)
                    val pending = if (Build.VERSION.SDK_INT >= 26)
                        PendingIntent.getForegroundService(this@NotificationToolbarService, 10_081, deleted, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    else PendingIntent.getService(this@NotificationToolbarService, 10_081, deleted, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    setDeleteIntent(pending)
                }
            }.build()
    }

    private fun route(destination: String, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(this, requestCode, Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_WIDGET_ROUTE, destination)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun updateChannelName(name: String) {
        if (Build.VERSION.SDK_INT >= 26 && channelName != name) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH).apply { setShowBadge(false) })
            channelName = name
        }
    }

    private fun isUserUnlocked(): Boolean = getSystemService(UserManager::class.java)?.isUserUnlocked != false

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "toolbar_channel_v2"
        private const val ACTION_DELETE = "com.aetherquorion.cleansuperai.action.NOTIFICATION_DELETE_KEEPALIVE"

        fun start(context: Context, allowLockedBoot: Boolean = false) {
            if (!allowLockedBoot && Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            val intent = Intent(context, NotificationToolbarService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NotificationToolbarService::class.java))
        }
    }
}
