package com.aetherquorion.cleansuperai.data.model

data class StorageOverview(
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long,
) {
    val availableFraction: Float
        get() = if (totalBytes > 0L) {
            (availableBytes.toDouble() / totalBytes.toDouble()).coerceIn(0.0, 1.0).toFloat()
        } else {
            0f
        }

    val availablePercent: Int
        get() = (availableFraction * 100).toInt()
}
