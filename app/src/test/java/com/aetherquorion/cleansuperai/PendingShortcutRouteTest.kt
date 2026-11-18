package com.aetherquorion.cleansuperai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingShortcutRouteTest {
    @Test fun latestShortcutIsDeliveredOnceAfterLaunch() {
        PendingShortcutRoute.remember("large")
        PendingShortcutRoute.remember("cache")

        assertEquals("cache", PendingShortcutRoute.consume())
        assertNull(PendingShortcutRoute.consume())
    }
}
