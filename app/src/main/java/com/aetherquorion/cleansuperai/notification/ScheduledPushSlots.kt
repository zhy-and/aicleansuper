package com.aetherquorion.cleansuperai.notification

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ScheduledPushSlot(
    val sceneKey: String,
    val index: Int,
    val timeLabel: String,
    val scheduledAtMillis: Long,
    val id: String,
)

object ScheduledPushSlots {
    const val MAX_LOOKBACK_MS = 2L * 60L * 60L * 1000L
    private const val MAX_SLOTS_PER_SCENE = 8

    fun dueSlots(
        timesByScene: Map<String, List<String>>,
        nowMillis: Long,
        lookbackMs: Long = MAX_LOOKBACK_MS,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): List<ScheduledPushSlot> {
        val dates = listOf(-1, 0).map { offset ->
            Calendar.getInstance(timeZone).apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply { this.timeZone = timeZone }
        return timesByScene.flatMap { (sceneKey, times) ->
            times.take(MAX_SLOTS_PER_SCENE).mapIndexedNotNull { index, raw ->
                val time = raw.trim()
                val parts = time.split(':')
                val hour = parts.getOrNull(0)?.toIntOrNull()
                val minute = parts.getOrNull(1)?.toIntOrNull()
                if (parts.size != 2 || hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
                    return@mapIndexedNotNull null
                }
                dates.mapNotNull { date ->
                    val scheduled = date.clone() as Calendar
                    scheduled.set(Calendar.HOUR_OF_DAY, hour)
                    scheduled.set(Calendar.MINUTE, minute)
                    val at = scheduled.timeInMillis
                    if (at > nowMillis || nowMillis - at > lookbackMs) null else ScheduledPushSlot(
                        sceneKey = sceneKey,
                        index = index,
                        timeLabel = time,
                        scheduledAtMillis = at,
                        id = "${dateFormat.format(Date(at))}|$sceneKey|$index|$time",
                    )
                }.maxByOrNull { it.scheduledAtMillis }
            }
        }.sortedBy { it.scheduledAtMillis }
    }
}
