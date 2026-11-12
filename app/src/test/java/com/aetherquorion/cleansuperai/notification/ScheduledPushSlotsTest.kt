package com.aetherquorion.cleansuperai.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ScheduledPushSlotsTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun at(value: String): Long = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
        timeZone = utc
    }.parse(value)!!.time

    @Test fun `recovers only configured slots due in the last two hours`() {
        val slots = ScheduledPushSlots.dueSlots(
            mapOf("scheduled_push_1" to listOf("09:00", "10:00", "13:00")),
            at("2026-10-26 10:20"),
            2 * 60 * 60 * 1000L,
            utc,
        )
        assertEquals(listOf("09:00", "10:00"), slots.map { it.timeLabel })
        assertEquals(listOf(0, 1), slots.map { it.index })
    }

    @Test fun `midnight recovery uses the prior local date in slot identity`() {
        val slots = ScheduledPushSlots.dueSlots(
            mapOf("scheduled_push_3" to listOf("23:50")),
            at("2026-10-27 00:10"),
            2 * 60 * 60 * 1000L,
            utc,
        )
        assertEquals(1, slots.size)
        assertEquals("20261026|scheduled_push_3|0|23:50", slots.single().id)
        assertEquals(at("2026-10-26 23:50"), slots.single().scheduledAtMillis)
    }

    @Test fun `invalid times and ninth slot are ignored`() {
        val times = listOf("bad", "00:00", "01:00", "02:00", "03:00", "04:00", "05:00", "06:00", "07:00")
        val slots = ScheduledPushSlots.dueSlots(
            mapOf("scheduled_push_2" to times),
            at("2026-10-26 07:30"),
            8 * 60 * 60 * 1000L,
            utc,
        )
        assertTrue(slots.none { it.index == 8 || it.timeLabel == "bad" })
    }
}
