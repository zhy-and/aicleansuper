package com.aetherquorion.cleansuperai.ui.flow

import org.junit.Assert.assertEquals
import org.junit.Test

class CleanupResultTest {
    @Test fun onlyConfirmedFilesCountAsCleaned() {
        val result = CleanupResult.fromAttempts(listOf(100L, 200L, 300L), setOf(0, 2))
        assertEquals(3, result.attempted)
        assertEquals(2, result.deleted)
        assertEquals(400L, result.freedBytes)
    }
}
