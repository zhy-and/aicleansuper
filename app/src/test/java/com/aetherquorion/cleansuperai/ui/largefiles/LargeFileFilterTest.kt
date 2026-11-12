package com.aetherquorion.cleansuperai.ui.largefiles

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LargeFileFilterTest {
    @Test fun filtersByTypeSizeAndAge() {
        val now = 1_000_000_000L
        val file = LargeFileEntry("/storage/file.pdf", "file.pdf", 60L * 1024 * 1024, now - 40L * 24 * 60 * 60 * 1000)
        assertTrue(file.matches(LargeFileType.DOCUMENT, 50L * 1024 * 1024, 30L * 24 * 60 * 60 * 1000, now))
        assertFalse(file.matches(LargeFileType.VIDEO, 50L * 1024 * 1024, null, now))
        assertFalse(file.matches(LargeFileType.ALL, 100L * 1024 * 1024, null, now))
    }
}
