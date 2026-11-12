package com.aetherquorion.cleansuperai.ui.tools

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class NetworkRangeTest {
    @Test fun rollingRangesUseExpectedDurations() {
        val now = 1_000_000_000_000L
        assertEquals(now - 24L * 60 * 60 * 1000, NetworkRange.DAY.start(now))
        assertEquals(now - 30L * 24 * 60 * 60 * 1000, NetworkRange.THIRTY_DAYS.start(now))
    }
}
