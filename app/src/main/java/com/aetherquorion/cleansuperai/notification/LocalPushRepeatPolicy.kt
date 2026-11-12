package com.aetherquorion.cleansuperai.notification

import java.util.Calendar
import java.util.TimeZone

/** Extra same-ID submissions for one logical local notification. */
internal object LocalPushRepeatPolicy {
    data class Plan(val extraCount: Int = 0, val intervalMs: Long = 3000L)

    fun select(
        config: LocalPushConfig.RepeatSubmitConfig?,
        sceneEnabled: Boolean?,
        installAtMs: Long,
        nowMs: Long,
        timeZone: TimeZone,
        manufacturer: String,
        brand: String,
    ): Plan {
        if (config?.enabled != true || sceneEnabled == false) return Plan()
        val samsung = manufacturer.equals("samsung", true) || brand.equals("samsung", true)
        val firstDay = installAtMs > 0L && sameDay(installAtMs, nowMs, timeZone)
        val rawCount = when {
            samsung && firstDay -> config.samsungFirstDayExtraCount ?: 1
            samsung -> config.samsungLaterDayExtraCount ?: 1
            firstDay -> config.firstDayExtraCount ?: 2
            else -> config.laterDayExtraCount ?: 5
        }
        val interval = config.intervalMs?.takeIf { it in 1000..60000 } ?: 3000
        return Plan(rawCount.coerceIn(0, 5), interval.toLong())
    }

    private fun sameDay(first: Long, second: Long, zone: TimeZone): Boolean {
        val a = Calendar.getInstance(zone).apply { timeInMillis = first }
        val b = Calendar.getInstance(zone).apply { timeInMillis = second }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
}
