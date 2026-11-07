package com.aetherquorion.cleansuperai.keepalive

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

class KeepAliveBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val reason = when (intent?.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> KeepAliveReason.LOCKED_BOOT
            Intent.ACTION_USER_UNLOCKED -> KeepAliveReason.USER_UNLOCKED
            Intent.ACTION_MY_PACKAGE_REPLACED -> KeepAliveReason.PACKAGE_REPLACED
            "android.intent.action.QUICKBOOT_POWERON", "com.htc.intent.action.QUICKBOOT_POWERON" -> KeepAliveReason.QUICKBOOT
            else -> KeepAliveReason.BOOT
        }
        if (reason == KeepAliveReason.LOCKED_BOOT) KeepAliveCoordinator.bootstrapLockedBootWarmup(context)
        else KeepAliveCoordinator.bootstrap(context, reason)
    }
}

class KeepAliveAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        KeepAliveCoordinator.bootstrap(context, KeepAliveReason.ALARM)
    }
}

class KeepAliveQuickJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        KeepAliveCoordinator.bootstrap(this, KeepAliveReason.QUICK_JOB)
        return false
    }
    override fun onStopJob(params: JobParameters): Boolean = true
}

class KeepAliveHeartbeatJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        KeepAliveCoordinator.bootstrap(this, KeepAliveReason.HEARTBEAT_JOB)
        return false
    }
    override fun onStopJob(params: JobParameters): Boolean = true
}

class KeepAliveHomeKeyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
            KeepAliveCoordinator.bootstrap(context, KeepAliveReason.HOME_KEY)
        }
    }

    companion object {
        @Volatile private var registered = false
        @Synchronized fun register(context: Context) {
            if (registered) return
            runCatching {
                val app = context.applicationContext
                val filter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(KeepAliveHomeKeyReceiver(), filter, Context.RECEIVER_EXPORTED)
                else app.registerReceiver(KeepAliveHomeKeyReceiver(), filter)
                registered = true
            }.onFailure { Log.w("KeepAlive", "Home receiver registration failed", it) }
        }
    }
}
