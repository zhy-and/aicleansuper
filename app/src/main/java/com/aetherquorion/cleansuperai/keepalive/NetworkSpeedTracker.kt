package com.aetherquorion.cleansuperai.keepalive

import android.net.TrafficStats
import android.os.SystemClock
import kotlin.math.roundToInt

object NetworkSpeedTracker {
    private var lastRxBytes = 0L
    private var lastSampleTime = 0L
    private var cachedValue = "0.0"
    private var cachedFull = "0.0 KB/s"

    data class SpeedDisplay(val value: String, val fullText: String)

    fun sample(): SpeedDisplay {
        val now = SystemClock.elapsedRealtime()
        val rxBytes = TrafficStats.getTotalRxBytes()
        if (rxBytes == TrafficStats.UNSUPPORTED.toLong()) {
            return SpeedDisplay(cachedValue, cachedFull)
        }
        if (lastSampleTime == 0L) {
            lastSampleTime = now
            lastRxBytes = rxBytes
            return SpeedDisplay(cachedValue, cachedFull)
        }
        val elapsed = now - lastSampleTime
        if (elapsed < 5_000L) {
            return SpeedDisplay(cachedValue, cachedFull)
        }
        val speedBytesPerSec = ((rxBytes - lastRxBytes).coerceAtLeast(0L) * 1000.0) / elapsed
        lastSampleTime = now
        lastRxBytes = rxBytes
        cachedValue = formatValue(speedBytesPerSec)
        cachedFull = "$cachedValue KB/s"
        return SpeedDisplay(cachedValue, cachedFull)
    }

    private fun formatValue(bytesPerSec: Double): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> {
                val mb = (bytesPerSec / (1024 * 1024) * 10).roundToInt() / 10.0
                mb.toString()
            }
            bytesPerSec >= 1024 -> {
                val kb = (bytesPerSec / 1024 * 10).roundToInt() / 10.0
                kb.toString()
            }
            else -> {
                val kb = (bytesPerSec / 1024 * 10).roundToInt() / 10.0
                kb.toString()
            }
        }
    }
}
