package com.aetherquorion.cleansuperai.notification

import android.content.Context
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_FO
import com.tencent.mmkv.MMKV

object CleanReminderState {
    private const val KEY_LAST_CLEAN_AT = "clean_reminder_last_clean_at"
    private const val KEY_TRACKED_FROM_AT = "clean_reminder_tracked_from_at"
    private const val DAY_MS = 86_400_000L

    fun markCleaned(context: Context) {
        ensureMmkv(context)
        val now = System.currentTimeMillis()
        MMKV.defaultMMKV().putLong(KEY_LAST_CLEAN_AT, now)
        MMKV.defaultMMKV().putLong(KEY_TRACKED_FROM_AT, now)
    }

    fun shouldShowReminder(context: Context): Boolean {
        return status(context).shouldShow
    }

    fun daysSinceLastClean(context: Context): Int {
        return status(context).daysSinceClean
    }

    fun status(context: Context): Status {
        val baseline = baselineAt(context)
        val now = System.currentTimeMillis()
        val elapsed = (now - baseline).coerceAtLeast(0L)
        return Status(
            baselineAt = baseline,
            elapsedMs = elapsed,
            remainingMs = (DAY_MS - elapsed).coerceAtLeast(0L),
            daysSinceClean = (elapsed / DAY_MS).toInt().coerceAtLeast(1).coerceAtMost(99),
        )
    }

    private fun baselineAt(context: Context): Long {
        ensureMmkv(context)
        val store = MMKV.defaultMMKV()
        val lastCleanAt = store.getLong(KEY_LAST_CLEAN_AT, 0L)
        if (lastCleanAt > 0L) return lastCleanAt

        val trackedFromAt = store.getLong(KEY_TRACKED_FROM_AT, 0L)
        if (trackedFromAt > 0L) return trackedFromAt

        val firstOpenAt = store.getLong(DATA_CONSTANT_FO, 0L)
        val baseline = if (firstOpenAt > 0L) firstOpenAt else System.currentTimeMillis()
        store.putLong(KEY_TRACKED_FROM_AT, baseline)
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
        val daysSinceClean: Int,
    ) {
        val shouldShow: Boolean = remainingMs == 0L

        fun describe(): String {
            return "baselineAt=$baselineAt elapsed=${formatDuration(elapsedMs)} " +
                "remaining=${formatDuration(remainingMs)} days=$daysSinceClean shouldShow=$shouldShow"
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
