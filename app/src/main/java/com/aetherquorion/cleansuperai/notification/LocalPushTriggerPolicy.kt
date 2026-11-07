package com.aetherquorion.cleansuperai.notification

/** Fixed client-side trigger thresholds, independent of notification frequency limits. */
object LocalPushTriggerPolicy {
    private const val STORAGE_CHANGE_BYTES = 30L * 1024 * 1024
    private const val INACTIVE_MS = 12L * 60 * 60 * 1000

    fun storageChangedEnough(baselineFreeBytes: Long, currentFreeBytes: Long): Boolean =
        currentFreeBytes > baselineFreeBytes + STORAGE_CHANGE_BYTES ||
            currentFreeBytes < baselineFreeBytes - STORAGE_CHANGE_BYTES

    fun trashedImagesDecreased(previousCount: Int, currentCount: Int): Boolean =
        previousCount > currentCount

    fun inactiveLongEnough(elapsedMs: Long): Boolean = elapsedMs >= INACTIVE_MS

    fun inactiveRemainingMs(elapsedMs: Long): Long = (INACTIVE_MS - elapsedMs).coerceAtLeast(0L)
}
