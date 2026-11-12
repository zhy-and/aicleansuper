package com.aetherquorion.cleansuperai.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.media.AudioAttributes
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.RemoteViews
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.aetherquorion.cleansuperai.MainActivity
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.analytics.AnalyticsBridge
import com.aetherquorion.cleansuperai.analytics.KeepAliveReporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** All trigger producers enter through [show]; quota and style selection stay centralized. */
object LocalEventPushHelper {
    private const val SCHEDULED_INSIGHT_VALUE_COLOR = 0xFF16A765.toInt()
    enum class EventType(val sceneKey: String) {
        SCREENSHOT("screenshot_capture"),
        APP_INSTALL("app_install"),
        APP_UNINSTALL("app_uninstall"),
        CLEAN_REMINDER("clean_reminder"),
        BATTERY_BELOW_50("battery_below_50"),
        BATTERY_BELOW_20("battery_below_20"),
        CHARGING("charging"),
        CHARGING_FULL("charging_full"),
        CHARGING_TO_80("charging_to_80"),
        CHARGING_DISCONNECT("charging_disconnect"),
        APP_UPDATE("app_update"),
        NETWORK_CHANGE("network_change"),
        SCREEN_LOCK("screen_lock"),
        SCREEN_ON_AFTER_LOCK("screen_on_after_lock"),
        STORAGE_FREE_DROP("storage_free_drop"),
        SCHEDULED_PUSH_1("scheduled_push_1"),
        SCHEDULED_PUSH_2("scheduled_push_2"),
        SCHEDULED_PUSH_3("scheduled_push_3"),
        SCHEDULED_STORAGE_CLEAN("scheduled_storage_clean"),
        SCHEDULED_LARGE_FILES("scheduled_large_files"),
        SCHEDULED_DUPLICATE_PHOTOS("scheduled_duplicate_photos"),
        SCHEDULED_SIMILAR_PHOTOS("scheduled_similar_photos"),
        SCHEDULED_VIDEOS("scheduled_videos"),
        SCHEDULED_AUDIO("scheduled_audio"),
        SCHEDULED_APPS("scheduled_apps"),
        SCHEDULED_DATA("scheduled_data"),
        APP_INACTIVE("app_inactive"),
        APP_BACKGROUND("app_background"),
        SCAN_DONE_UNCLEANED("scan_done_uncleaned"),
        SCAN_INTERRUPTED("scan_interrupted"),
        IMAGE_DELETED("image_deleted"),
        VIDEO_DELETED("video_deleted"),
        AUDIO_DELETED("audio_deleted"),
        FILE_DELETED("file_deleted"),
        IMAGE_DOWNLOAD("image_download"),
        VIDEO_DOWNLOAD("video_download"),
        AUDIO_DOWNLOAD("audio_download"),
        FILE_DOWNLOAD("file_download"),
        TRASH_EMPTIED("trash_emptied");
    }

    fun show(context: Context, type: EventType, appLabel: String? = null): Boolean =
        show(context, type.sceneKey, appLabel)

    const val CHANNEL_ID = "event_recall_channel_v2"
    private const val REPAIRED_CHANNEL_ID = "event_recall_channel_v3"
    const val SILENT_REPEAT_CHANNEL_ID = "event_recall_channel_v2_silent_repeat"
    private const val PREFS = "purgu_local_push"
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                setShowBadge(true)
                enableVibration(true)
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI,
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            })
            if (existing?.importance == NotificationManager.IMPORTANCE_DEFAULT) {
                manager.createNotificationChannel(NotificationChannel(REPAIRED_CHANNEL_ID,
                    "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    setShowBadge(true)
                    enableVibration(true)
                    setSound(Settings.System.DEFAULT_NOTIFICATION_URI,
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                })
            }
            manager.createNotificationChannel(NotificationChannel(SILENT_REPEAT_CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                setShowBadge(true)
                setSound(null, null)
                enableVibration(false)
            })
        }
    }

    private fun activeChannelId(context: Context): String {
        if (Build.VERSION.SDK_INT < 26) return CHANNEL_ID
        val manager = context.getSystemService(NotificationManager::class.java) ?: return CHANNEL_ID
        return if (manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_DEFAULT)
            REPAIRED_CHANNEL_ID else CHANNEL_ID
    }

    @Synchronized fun show(context: Context, sceneKey: String, appLabel: String? = null): Boolean {
        AnalyticsBridge.logLpFunnel(context, "尝试", sceneKey, sceneKey)
        fun reject(reason: String): Boolean {
            AnalyticsBridge.logLpFunnel(context, "跳过", sceneKey, sceneKey, skipReason = reason)
            return false
        }
        val config = LocalPushConfig.current(context)
        if (config.global.skipForeground && ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return reject("应用在前台")
        val scene = config.scenes[sceneKey] ?: return reject("场景不存在")
        val bucket = config.buckets[scene.bucket] ?: return reject("桶不存在")
        if (!config.enabled) return reject("总开关关闭")
        if (!scene.enabled) return reject("场景关闭")
        if (!bucket.enabled) return reject("桶关闭")
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return reject("通知权限未授权")
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        if (prefs.getString("day", null) != day) {
            val editor = prefs.edit().putString("day", day).putInt("global", 0)
                .putInt("unclicked:global", 0)
            config.buckets.keys.forEach { editor.putInt("unclicked:$it", 0) }
            editor.commit()
        }
        val install = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime }.getOrDefault(now)
        if (now - install < config.global.installAgeSec * 1000L) return reject("安装时长不足")
        val firstDay = now - install < 86_400_000L
        val globalLimit = if (firstDay) config.global.firstMax else config.global.dailyMax
        val globalCount = prefs.getInt(if (firstDay) "first24_global" else "global", 0)
        if (globalLimit >= 0 && globalCount >= globalLimit) return reject("全局展示上限")
        fun count(key: String): Int = prefs.getInt("$day:$key", 0)
        if (bucket.dailyMax >= 0 && count("bucket:${scene.bucket}") >= bucket.dailyMax) return reject("桶每日上限")
        if (scene.dailyMax >= 0 && count("scene:$sceneKey") >= scene.dailyMax) return reject("场景每日上限")
        val unclicked = prefs.getInt("unclicked:global", 0)
        if (config.global.noClickStop >= 0 && unclicked >= config.global.noClickStop) return reject("全局未点击停发")
        if (bucket.noClickStop >= 0 && prefs.getInt("unclicked:${scene.bucket}", 0) >= bucket.noClickStop) return reject("桶未点击停发")
        fun cooling(key: String, seconds: Long): Boolean = seconds >= 0 && now - prefs.getLong("last:$key", 0) < seconds * 1000L
        if (cooling("global", config.global.cooldownSec.toLong())) return reject("全局冷却")
        if (cooling("bucket:${scene.bucket}", bucket.cooldownSec)) return reject("桶冷却")
        if (cooling("scene:$sceneKey", scene.cooldownSec)) return reject("场景冷却")

        ensureChannel(context)
        val activeChannel = activeChannelId(context)
        val titleName = when (sceneKey) {
            "clean_reminder" -> "notif_clean_reminder_title"
            "screenshot_capture" -> "notif_event_screenshot_title"
            "app_install" -> "notif_event_install_title"
            "app_uninstall" -> "notif_event_uninstall_title"
            "scheduled_storage_clean", "scheduled_large_files", "scheduled_duplicate_photos",
            "scheduled_similar_photos", "scheduled_videos", "scheduled_audio", "scheduled_apps",
            "scheduled_data" -> "notif_$sceneKey"
            else -> "notif_event_${sceneKey}_title"
        }
        val titleId = context.resources.getIdentifier(titleName, "string", context.packageName)
        val arrayName = titleName.removeSuffix("_title") + "_titles"
        val arrayId = context.resources.getIdentifier(arrayName, "array", context.packageName)
        val baseTitle = if (arrayId != 0) {
            val choices = context.resources.getStringArray(arrayId)
            if (choices.isNotEmpty()) choices[prefs.getInt("copy:$sceneKey", 0) % choices.size]
            else if (titleId != 0) context.getString(titleId) else sceneKey.replace('_', ' ')
        } else if (titleId != 0) context.getString(titleId) else sceneKey.replace('_', ' ')
        val packageEvent = when (sceneKey) {
            "app_install" -> PackageEventNotificationCopy.Event.INSTALL
            "app_uninstall" -> PackageEventNotificationCopy.Event.UNINSTALL
            "app_update" -> PackageEventNotificationCopy.Event.UPDATE
            else -> null
        }
        val copiedTitle = packageEvent?.let {
            PackageEventNotificationCopy.resolve(it, appLabel, originalTitle = baseTitle)?.text
        } ?: baseTitle
        val title: CharSequence = if (ScheduledInsightCopy.isInsight(sceneKey)) {
            val rendered = ScheduledInsightCopy.render(copiedTitle, ScheduledInsightCopy.sample(sceneKey))
            SpannableString(rendered.text).apply {
                rendered.highlights.forEach { range ->
                    setSpan(ForegroundColorSpan(SCHEDULED_INSIGHT_VALUE_COLOR),
                        range.first, range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        } else copiedTitle
        val body = if (sceneKey == "app_background") {
            val values = context.resources.getStringArray(R.array.notif_event_app_background_contents)
            values.getOrNull(prefs.getInt("copy:$sceneKey", 0) % values.size.coerceAtLeast(1)).orEmpty()
        } else ""
        val displayTitle = if (body.isBlank()) title else "$title\n$body"
        val icon = when {
            sceneKey.contains("battery") || sceneKey.startsWith("charging") -> R.drawable.ic_push_battery
            sceneKey.contains("install") || sceneKey.contains("apps") || sceneKey == "app_update" -> R.drawable.ic_push_apps
            sceneKey.contains("network") -> R.drawable.ic_push_network
            sceneKey.contains("screen_lock") -> R.drawable.ic_push_screen
            sceneKey.contains("screenshot") || sceneKey.contains("photos") -> R.drawable.ic_push_screenshot
            sceneKey.contains("video") -> R.drawable.ic_push_video
            sceneKey.contains("audio") -> R.drawable.ic_push_audio
            sceneKey.contains("image") -> R.drawable.ic_push_photo
            sceneKey.contains("download") || sceneKey.contains("file") -> R.drawable.ic_push_file
            sceneKey.contains("storage") -> R.drawable.ic_push_storage
            sceneKey.contains("trash") -> R.drawable.ic_push_trash
            sceneKey.contains("scan") -> R.drawable.ic_push_scan
            sceneKey.startsWith("scheduled") || sceneKey == "app_inactive" -> R.drawable.ic_push_clock
            else -> R.drawable.ic_push_clean
        }
        val actionId = when {
            sceneKey.contains("battery") || sceneKey.startsWith("charging") -> R.string.notif_event_action_check_battery
            sceneKey.contains("network") -> R.string.notif_event_action_check_network
            sceneKey in setOf("app_install", "app_update", "app_inactive", "scan_interrupted") ||
                sceneKey.startsWith("scheduled_push") || sceneKey in setOf("scheduled_apps", "scheduled_data") ->
                R.string.notif_event_action_open_home
            else -> R.string.notif_event_action_clean
        }
        val action = context.getString(actionId)
        val route = scene.router
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_WIDGET_ROUTE, route)
            putExtra("local_push_scene", sceneKey)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return reject("通知管理器不可用")
        if (!manager.areNotificationsEnabled()) return reject("系统通知已关闭")
        if (Build.VERSION.SDK_INT >= 26 &&
            manager.getNotificationChannel(activeChannel)?.importance == NotificationManager.IMPORTANCE_NONE) return reject("通知渠道已关闭")
        val id = LocalPushNotificationIds.allocate(manager, emptyList())
        intent.putExtra("local_push_notify_id", id)
        val tap = PendingIntent.getActivity(context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val templateIds = scene.templateIds.filter { config.templates.containsKey(it) }
        val templateIndex = prefs.getInt("template:index:$sceneKey", 0)
        val templateId = templateIds.getOrNull(templateIndex % templateIds.size.coerceAtLeast(1)) ?: ""
        val template = config.templates[templateId]
        val mode = when (template?.layout) {
            "big" -> "big"
            "reminder_day" -> "reminder_day"
            else -> "small"
        }
        val layout = when (mode) {
            "big" -> R.layout.notification_big_collapsed
            "reminder_day" -> R.layout.notification_clean_reminder_collapsed
            else -> R.layout.notification_event_collapsed
        }
        val expanded = when (mode) {
            "big" -> R.layout.notification_big_expanded
            "reminder_day" -> R.layout.notification_clean_reminder_expanded
            else -> R.layout.notification_event_expanded
        }
        val days = if (sceneKey == "app_inactive") AppInactiveState.daysSinceLastOpen(context)
            else CleanReminderState.daysSinceLastClean(context)
        val small = buildViews(context, layout, mode, icon, displayTitle, action, template, tap, days)
        val big = buildViews(context, expanded, mode, icon, displayTitle, action, template, tap, days)
        val notification = NotificationCompat.Builder(context, activeChannel)
            .setSmallIcon(R.mipmap.ic_launcher).setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setColorized(false).setContentTitle(title)
            .setContentText(body.ifBlank { action }).setContentIntent(tap).setOngoing(true).setAutoCancel(false)
            .setGroup(LocalPushSummaryManager.GROUP_KEY)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(small).setCustomBigContentView(big).setCustomHeadsUpContentView(big)
            .build()
        val configuredMaximum = listOf(config.global.maxVisible, bucket.maxVisible)
            .filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
        val previousRepeatId = LocalPushRepeatSubmitter.pendingRepeatId()
        val maximum = if (previousRepeatId == null) configuredMaximum else maxOf(2, configuredMaximum)
        val children = manager.activeNotifications.filter {
            it.notification.group == LocalPushSummaryManager.GROUP_KEY &&
                it.id != LocalPushSummaryManager.SUMMARY_ID
        }.sortedBy { it.postTime }
        val toTrim = (children.size - maximum + 1).coerceAtLeast(0)
        children.filter { it.id != previousRepeatId }.take(toTrim).forEach {
            manager.cancel(it.id)
            LocalPushRepeatSubmitter.cancel(it.id)
        }
        val deliveryId = "$id:$now"
        if (runCatching { LocalPushRepeatSubmitter.postFirst(id, deliveryId) { manager.notify(id, notification) } }.isFailure) return reject("通知提交失败")
        LocalPushSummaryManager.sync(context, manager)
        prefs.edit().putInt(if (firstDay) "first24_global" else "global", globalCount + 1)
            .putInt("$day:bucket:${scene.bucket}", count("bucket:${scene.bucket}") + 1)
            .putInt("$day:scene:$sceneKey", count("scene:$sceneKey") + 1)
            .putInt("unclicked:global", unclicked + 1)
            .putInt("unclicked:${scene.bucket}", prefs.getInt("unclicked:${scene.bucket}", 0) + 1)
            .putLong("last:global", now).putLong("last:bucket:${scene.bucket}", now)
            .putLong("last:scene:$sceneKey", now)
            .putInt("copy:$sceneKey", prefs.getInt("copy:$sceneKey", 0) + 1).commit()
        if (templateIds.size > 1) {
            val used = prefs.getInt("template:used:$sceneKey", 0) + 1
            if (used >= bucket.repetitions.coerceAtLeast(1)) {
                prefs.edit().putInt("template:index:$sceneKey", (templateIndex + 1) % templateIds.size)
                    .putInt("template:used:$sceneKey", 0).commit()
            } else prefs.edit().putInt("template:used:$sceneKey", used).commit()
        }
        AnalyticsBridge.logLpFunnel(context, "展示", sceneKey, sceneKey,
            bucket = scene.bucket, notifyId = id.toString(), router = route)
        KeepAliveReporter.report(context, sceneKey, mapOf("route" to "本地推送", "result" to "展示成功"))
        val repeatPlan = LocalPushRepeatPolicy.select(config.global.repeatSubmit, scene.repeatSubmitEnabled,
            install, now, java.util.TimeZone.getDefault(), Build.MANUFACTURER, Build.BRAND)
        if (repeatPlan.extraCount > 0) {
            val silent = NotificationCompat.Builder(context, SILENT_REPEAT_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher).setColor(ContextCompat.getColor(context, R.color.notification_accent))
                .setColorized(false).setContentTitle(title)
                .setContentText(body.ifBlank { action }).setContentIntent(tap).setOngoing(true).setAutoCancel(false)
                .setGroup(LocalPushSummaryManager.GROUP_KEY).setSilent(true)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(small).setCustomBigContentView(big).setCustomHeadsUpContentView(big).build()
            LocalPushRepeatSubmitter.schedule(id, deliveryId, manager, silent, repeatPlan) {
                !ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                    LocalPushConfig.current(context).enabled &&
                    LocalPushConfig.current(context).scenes[sceneKey]?.enabled == true
            }
        }
        return true
    }

    private fun buildViews(context: Context, layout: Int, mode: String, icon: Int,
        title: CharSequence, action: String, template: LocalPushConfig.Template?, tap: PendingIntent,
        days: Int): RemoteViews = RemoteViews(context.packageName, layout).apply {
        setTextViewText(R.id.notif_event_title, title)
        setTextViewText(R.id.notif_event_action, action)
        when (mode) {
            "big" -> setImageViewResource(R.id.notif_big_logo, icon)
            "reminder_day" -> {
                setTextViewText(R.id.notif_days_number, days.toString())
                setTextViewText(R.id.notif_days_unit, context.getString(R.string.notif_days_unit))
            }
            else -> setImageViewResource(R.id.notif_event_icon, icon)
        }
        setOnClickPendingIntent(R.id.notif_event_action_wrap, tap)
        setOnClickPendingIntent(R.id.notif_event_action, tap)
        val bg = runCatching { android.graphics.Color.parseColor(template?.buttonBg) }
            .getOrDefault(0xFF0B7CFF.toInt())
        val pulse = when (bg) {
            0xFF0B7CFF.toInt() -> R.id.notif_event_action_pulse_blue
            0xFF14B86A.toInt() -> R.id.notif_event_action_pulse_green
            0xFFFF5A3D.toInt() -> R.id.notif_event_action_pulse_red
            0xFFFFE66D.toInt() -> R.id.notif_event_action_pulse_yellow
            else -> R.id.notif_event_action_pulse_black
        }
        val animate = template?.buttonAnim?.lowercase() !in setOf("none", "off", "false", "0")
        val pulses = intArrayOf(R.id.notif_event_action_pulse, R.id.notif_event_action_pulse_blue,
            R.id.notif_event_action_pulse_green, R.id.notif_event_action_pulse_red,
            R.id.notif_event_action_pulse_yellow, R.id.notif_event_action_pulse_black)
        pulses.forEach { id ->
            setProgressBar(id, 100, 0, true)
            setViewVisibility(id, if (animate && id == pulse) View.VISIBLE else View.GONE)
        }
        setImageViewResource(R.id.notif_event_action_bg, R.drawable.bg_notif_cta_tintable)
        setInt(R.id.notif_event_action_bg, "setColorFilter", bg)
        setViewVisibility(R.id.notif_event_action_bg, if (animate) View.GONE else View.VISIBLE)
        val textColor = runCatching { android.graphics.Color.parseColor(template?.buttonText) }
            .getOrDefault(android.graphics.Color.WHITE)
        setTextColor(R.id.notif_event_action, textColor)
    }

    fun recordClick(context: Context, sceneKey: String) {
        LocalPushRepeatSubmitter.cancelAll()
        val bucket = LocalPushConfig.current(context).scenes[sceneKey]?.bucket ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("unclicked:global", 0).putInt("unclicked:$bucket", 0).commit()
        AnalyticsBridge.logLpFunnel(context, "点击", sceneKey, sceneKey, bucket = bucket)
        KeepAliveReporter.report(context, sceneKey, mapOf("route" to "本地推送", "result" to "点击"))
    }
}

class LocalPushTapReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scene = intent.getStringExtra("scene") ?: return
        LocalEventPushHelper.recordClick(context, scene)
        val target = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_WIDGET_ROUTE, intent.getStringExtra("route"))
            putExtra("local_push_scene", scene)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(target)
    }
}
