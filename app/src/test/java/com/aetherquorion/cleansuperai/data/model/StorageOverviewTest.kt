package com.aetherquorion.cleansuperai.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageOverviewTest {
    @Test fun availableSpaceUsesFreeBytesOverTotalBytes() {
        val overview = StorageOverview(totalBytes = 128L, usedBytes = 32L, availableBytes = 96L)
        assertEquals(0.75f, overview.availableFraction, 0.0001f)
        assertEquals(75, overview.availablePercent)
    }

    @Test fun invalidTotalsCannotProduceAnOutOfRangeRing() {
        assertEquals(0, StorageOverview(0L, 0L, 0L).availablePercent)
        assertEquals(100, StorageOverview(100L, 0L, 150L).availablePercent)
        assertEquals(0, StorageOverview(100L, 100L, -1L).availablePercent)
    }
}
