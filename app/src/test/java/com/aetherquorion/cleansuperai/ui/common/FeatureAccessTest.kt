package com.aetherquorion.cleansuperai.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeatureAccessTest {
    @Test fun storageFeaturesRequireAllFiles() {
        assertEquals(FeatureAccess.ALL_FILES, FeatureAccessPolicy.forFeature(FeatureRoute.CACHE))
        assertEquals(FeatureAccess.ALL_FILES, FeatureAccessPolicy.forFeature(FeatureRoute.LARGE_FILES))
        assertEquals(FeatureAccess.ALL_FILES, FeatureAccessPolicy.forFeature(FeatureRoute.AUDIO))
    }

    @Test fun networkRequiresUsageAccess() {
        assertEquals(FeatureAccess.USAGE, FeatureAccessPolicy.forFeature(FeatureRoute.NETWORK_TRAFFIC))
    }

    @Test fun informationPagesNeedNoAccess() {
        assertEquals(FeatureAccess.NONE, FeatureAccessPolicy.forFeature(FeatureRoute.BATTERY_INFO))
        assertEquals(FeatureAccess.NONE, FeatureAccessPolicy.forFeature(FeatureRoute.DEVICE_INFO))
    }

    @Test fun pendingRouteIsConsumedOnce() {
        val pending = PendingFeatureRoute()
        pending.set(FeatureRoute.LARGE_FILES)
        assertEquals(FeatureRoute.LARGE_FILES, pending.consume())
        assertNull(pending.consume())
    }
}
