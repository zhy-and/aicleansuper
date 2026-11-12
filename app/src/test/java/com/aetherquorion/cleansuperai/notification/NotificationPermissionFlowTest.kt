package com.aetherquorion.cleansuperai.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionFlowTest {
    @Test fun `splash requests on API 33 when not granted`() {
        assertTrue(NotificationPermissionFlow.shouldRequest(33, false))
        assertFalse(NotificationPermissionFlow.shouldRequest(32, false))
        assertFalse(NotificationPermissionFlow.shouldRequest(33, true))
    }

    @Test fun `home tip opens settings after two denials`() {
        assertEquals(NotificationPermissionFlow.TipAction.REQUEST, NotificationPermissionFlow.tipAction(0))
        assertEquals(NotificationPermissionFlow.TipAction.REQUEST, NotificationPermissionFlow.tipAction(1))
        assertEquals(NotificationPermissionFlow.TipAction.SETTINGS, NotificationPermissionFlow.tipAction(2))
    }
}
