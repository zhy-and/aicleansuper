package com.example.cleansuperai.ui.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class CleanerUiRulesTest {
    @Test
    fun `main destinations match the requested bottom navigation order`() {
        assertEquals(
            listOf("Home", "Swipe", "Compress", "Tools"),
            CleanerDestination.entries.map { it.label },
        )
    }

    @Test
    fun `photo timestamps are grouped by calendar month newest first`() {
        val utc = TimeZone.getTimeZone("UTC")
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }
        val photos = listOf(
            PhotoRecord(1, parser.parse("2026-06-05")!!.time),
            PhotoRecord(2, parser.parse("2026-07-01")!!.time),
            PhotoRecord(3, parser.parse("2026-06-20")!!.time),
        )

        val groups = PhotoMonthGrouper.group(photos, utc, Locale.ENGLISH)

        assertEquals(listOf("July 2026", "June 2026"), groups.map { it.label })
        assertEquals(listOf(2L), groups[0].photos.map { it.id })
        assertEquals(listOf(3L, 1L), groups[1].photos.map { it.id })
    }

    @Test
    fun `compression result reports saved bytes and only succeeds when smaller`() {
        val smaller = CompressionResult(originalBytes = 8_000_000, compressedBytes = 1_500_000)
        val larger = CompressionResult(originalBytes = 1_000_000, compressedBytes = 1_100_000)

        assertEquals(6_500_000, smaller.savedBytes)
        assertTrue(smaller.isWorthSaving)
        assertEquals(0, larger.savedBytes)
        assertFalse(larger.isWorthSaving)
    }
}
