package com.aetherquorion.cleansuperai.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStorageGateTest {
    @Test fun storageFeaturesRequireTheGate() {
        listOf("clean_center", "similar", "duplicates", "videos", "screenshots",
            "large_files", "cache_cleanup", "picture_cleanup", "video_cleanup",
            "audio_cleanup", "vault").forEach { assertTrue(it, HomeStorageGate.requiresAllFiles(it)) }
    }

    @Test fun independentFeaturesKeepTheirOwnPermissionFlow() {
        listOf("battery_info", "device_info", "contacts_cleanup", "network_traffic")
            .forEach { assertFalse(it, HomeStorageGate.requiresAllFiles(it)) }
    }
}
