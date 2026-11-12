package com.aetherquorion.cleansuperai.keepalive

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.atomic.AtomicBoolean

/** Purgu's service, 8-hour alarm, 4-second and 24-minute jobs, FCM and Home-key routes. */
object KeepAliveCoordinator {
    private const val TAG = "KeepAlive"
    private const val QUICK_JOB_ID = 20_849_604
    private const val HEARTBEAT_JOB_ID = 20_849_624
    private val running = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    private var retry: Runnable? = null
    private var retryDelay = 30_000L

    /** Analytics adapter may be installed by Application without coupling this module to network APIs. */
    @Volatile var outcomeReporter: ((Context, String, KeepAliveReason, String, Map<String, String>) -> Unit)? = null

    @JvmStatic fun bootstrapFromFcm(context: Context) = bootstrap(context, KeepAliveReason.FCM_HIGH)

    @JvmStatic fun bootstrap(context: Context, reason: KeepAliveReason) {
        val app = context.applicationContext
        val switches = KeepAliveConfig
        val routeKey = when (reason) {
            KeepAliveReason.LOCKED_BOOT, KeepAliveReason.BOOT, KeepAliveReason.USER_UNLOCKED,
            KeepAliveReason.PACKAGE_REPLACED, KeepAliveReason.QUICKBOOT -> "kpa_boot_switch"
            KeepAliveReason.SERVICE_START, KeepAliveReason.SERVICE_DESTROY,
            KeepAliveReason.SERVICE_TASK_REMOVED, KeepAliveReason.NOTIFICATION_DELETE -> "kpa_service_switch"
            KeepAliveReason.QUICK_JOB, KeepAliveReason.HEARTBEAT_JOB -> "kpa_job_switch"
            KeepAliveReason.ALARM -> "kpa_alarm_switch"
            KeepAliveReason.FCM_HIGH -> "kpa_fcm_switch"
            KeepAliveReason.APP_FOREGROUND -> "kpa_resume_switch"
            KeepAliveReason.HOME_KEY -> "kpa_home_switch"
            else -> null
        }
        if (routeKey != null && !switches.enabled(app, routeKey)) {
            outcomeReporter?.invoke(app, routeKey, reason, "失败", mapOf("failure_reason" to "入口开关关闭"))
            return
        }
        if (!running.compareAndSet(false, true)) return
        val steps = linkedMapOf<String, String>()
        try {
            mark(app, reason)
            if (switches.enabled(app, "kpa_service_switch")) step(steps, "service_start") {
                NotificationToolbarService.start(app)
            }
            if (switches.enabled(app, "kpa_alarm_switch")) step(steps, "alarm_schedule") { scheduleAlarm(app) }
            if (switches.enabled(app, "kpa_job_switch")) {
                if (reason != KeepAliveReason.QUICK_JOB) step(steps, "quick_job_schedule") { scheduleJob(app, true) }
                step(steps, "heartbeat_job_schedule") { scheduleJob(app, false) }
            }
            if (switches.enabled(app, "kpa_fcm_switch")) step(steps, "fcm_channel") { ensureFcmChannel(app) }
            if (switches.enabled(app, "kpa_home_switch")) step(steps, "home_receiver_register") {
                KeepAliveHomeKeyReceiver.register(app)
            }
            Log.i(TAG, "bootstrap ${reason.value}: $steps")
            val started = steps.filterValues { it == "成功" }.keys.joinToString(",")
            val failures = steps.filterValues { it.startsWith("失败") }
            outcomeReporter?.invoke(app, routeKey ?: "manual", reason,
                if (failures.isEmpty()) "成功" else "失败",
                mapOf("started_routes" to started, "failure_reason" to failures.entries.joinToString(",") { "${it.key}:${it.value}" }))
        } finally {
            running.set(false)
        }
    }

    /** Device-protected phase: no credential-backed switch or analytics access. */
    fun bootstrapLockedBootWarmup(context: Context) {
        val app = context.applicationContext
        runCatching { NotificationToolbarService.start(app, allowLockedBoot = true) }
        runCatching { scheduleJob(app, true) }
        runCatching { scheduleJob(app, false) }
        runCatching { scheduleAlarm(app) }
    }

    private fun step(steps: MutableMap<String, String>, name: String, block: () -> Unit) {
        steps[name] = runCatching { block(); "成功" }
            .getOrElse { Log.w(TAG, "$name failed", it); "失败:${it.javaClass.simpleName}" }
    }

    private fun mark(context: Context, reason: KeepAliveReason) {
        context.getSharedPreferences("keepalive_state", Context.MODE_PRIVATE).edit()
            .putString("last_reason", reason.value).putLong("last_time", System.currentTimeMillis()).apply()
    }

    private fun scheduleJob(context: Context, quick: Boolean) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        val id = if (quick) QUICK_JOB_ID else HEARTBEAT_JOB_ID
        val component = ComponentName(context, if (quick) KeepAliveQuickJobService::class.java else KeepAliveHeartbeatJobService::class.java)
        val info = JobInfo.Builder(id, component)
            .setMinimumLatency(if (quick) KeepAliveConfig.QUICK_JOB_MIN_LATENCY_MS else KeepAliveConfig.HEARTBEAT_MIN_LATENCY_MS)
            .setOverrideDeadline(if (quick) KeepAliveConfig.QUICK_JOB_DEADLINE_MS else KeepAliveConfig.HEARTBEAT_DEADLINE_MS)
            .setPersisted(true).build()
        check(scheduler.schedule(info) == JobScheduler.RESULT_SUCCESS)
    }

    private fun scheduleAlarm(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, KeepAliveAlarmReceiver::class.java).setAction("${context.packageName}.KEEPALIVE_ALARM")
        val pending = PendingIntent.getBroadcast(context, 15_254, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarm.cancel(pending)
        alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + KeepAliveConfig.ALARM_INTERVAL_MS, pending)
    }

    private fun ensureFcmChannel(context: Context) {
        FirebaseMessaging.getInstance().token.addOnFailureListener { Log.w(TAG, "FCM token failed", it) }
        subscribe(context)
    }

    private fun subscribe(context: Context) {
        FirebaseMessaging.getInstance().subscribeToTopic(KeepAliveConfig.FCM_TOPIC)
            .addOnSuccessListener {
                synchronized(this) {
                    retry?.let(handler::removeCallbacks)
                    retry = null
                    retryDelay = 30_000L
                }
            }.addOnFailureListener {
                Log.w(TAG, "FCM topic failed", it)
                synchronized(this) {
                    if (retry != null) return@addOnFailureListener
                    val task = Runnable {
                        synchronized(this) { retry = null }
                        if (KeepAliveConfig.enabled(context, "kpa_fcm_switch")) subscribe(context)
                    }
                    retry = task
                    handler.postDelayed(task, retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(86_400_000L)
                }
            }
    }
}
