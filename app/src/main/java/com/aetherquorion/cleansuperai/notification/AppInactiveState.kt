package com.aetherquorion.cleansuperai.notification

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_FO
import com.blankj.utilcode.util.LogUtils
import com.tencent.mmkv.MMKV

/**
 * 「久未打开 App」状态：≥12h 可推，天数动态 1～99。
 * 「打开」= 进程进前台（ProcessLifecycle ON_START）。
 */
object AppInactiveState {
    private const val TAG = "AppInactiveState"
    private const val KEY_LAST_OPEN_AT = "local_push_app_last_open_at"
    private const val DAY_MS = 86_400_000L

    @Volatile
    private var observerRegistered = false

    fun startTracking(context: Context) {
        ensureMmkv(context)
        // 冷启动即算一次打开，避免装完立刻被当成「已失活」
        markOpened(context.applicationContext, reason = "bootstrap")
        if (observerRegistered) return
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                markOpened(context.applicationContext, reason = "process_foreground")
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        observerRegistered = true
        LogUtils.i(TAG, "已注册进程前台监听，刷新 last_open_at")
    }

    fun markOpened(context: Context, reason: String) {
        ensureMmkv(context)
        val now = System.currentTimeMillis()
        MMKV.defaultMMKV().putLong(KEY_LAST_OPEN_AT, now)
        LogUtils.i(TAG, "刷新 last_open_at=$now reason=$reason")
    }

    fun daysSinceLastOpen(context: Context): Int = status(context).daysSinceOpen

    fun status(context: Context): Status {
        val baseline = baselineAt(context)
        val now = System.currentTimeMillis()
        val elapsed = (now - baseline).coerceAtLeast(0L)
        return Status(
            baselineAt = baseline,
            elapsedMs = elapsed,
            remainingMs = LocalPushTriggerPolicy.inactiveRemainingMs(elapsed),
            daysSinceOpen = (elapsed / DAY_MS).toInt().coerceAtLeast(1).coerceAtMost(99),
        )
    }

    private fun baselineAt(context: Context): Long {
        ensureMmkv(context)
        val store = MMKV.defaultMMKV()
        val lastOpen = store.getLong(KEY_LAST_OPEN_AT, 0L)
        if (lastOpen > 0L) return lastOpen

        val firstOpenAt = store.getLong(DATA_CONSTANT_FO, 0L)
        val baseline = if (firstOpenAt > 0L) firstOpenAt else System.currentTimeMillis()
        store.putLong(KEY_LAST_OPEN_AT, baseline)
        return baseline
    }

    private fun ensureMmkv(context: Context) {
        runCatching { MMKV.defaultMMKV() }.onFailure {
            MMKV.initialize(context.applicationContext)
        }
    }

    data class Status(
        val baselineAt: Long,
        val elapsedMs: Long,
        val remainingMs: Long,
        val daysSinceOpen: Int,
    ) {
        val shouldShow: Boolean = LocalPushTriggerPolicy.inactiveLongEnough(elapsedMs)

        fun describe(): String {
            return "baselineAt=$baselineAt elapsed=${formatDuration(elapsedMs)} " +
                "remaining=${formatDuration(remainingMs)} days=$daysSinceOpen shouldShow=$shouldShow"
        }

        private fun formatDuration(ms: Long): String {
            val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            return "${hours}h ${minutes}m ${seconds}s"
        }
    }
}
