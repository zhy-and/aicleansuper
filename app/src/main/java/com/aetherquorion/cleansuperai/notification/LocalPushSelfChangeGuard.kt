package com.aetherquorion.cleansuperai.notification

/**
 * 本 App 主动删文件时短暂压制 MediaStore 删除监听，避免自触发 image/video/..._deleted。
 */
object LocalPushSelfChangeGuard {
    private const val DEFAULT_SUPPRESS_MS = 8_000L

    @Volatile
    private var suppressUntilElapsedRealtime = 0L

    fun suppress(durationMs: Long = DEFAULT_SUPPRESS_MS) {
        val until = android.os.SystemClock.elapsedRealtime() + durationMs.coerceAtLeast(0L)
        if (until > suppressUntilElapsedRealtime) {
            suppressUntilElapsedRealtime = until
        }
    }

    fun isSuppressed(): Boolean {
        return android.os.SystemClock.elapsedRealtime() < suppressUntilElapsedRealtime
    }
}
