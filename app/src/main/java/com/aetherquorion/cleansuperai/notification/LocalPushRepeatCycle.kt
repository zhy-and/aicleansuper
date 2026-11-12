package com.aetherquorion.cleansuperai.notification

/** The count belongs to a delivery, never to a scene trigger or quota. */
internal class LocalPushRepeatCycle(private val deliveryId: String, extraCount: Int) {
    private val limit = extraCount.coerceIn(0, 5)
    private var claimed = 0
    private var cancelled = false

    fun claim(currentDeliveryId: String): Int? {
        if (cancelled || currentDeliveryId != deliveryId || claimed >= limit) return null
        claimed += 1
        return claimed
    }

    fun cancel() { cancelled = true }
}
