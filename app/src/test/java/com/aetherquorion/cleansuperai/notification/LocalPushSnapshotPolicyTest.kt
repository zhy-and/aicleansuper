package com.aetherquorion.cleansuperai.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalPushSnapshotPolicyTest {
    private val organic = """{"version":1,"enabled":true,"buckets":{},"scenes":{},"templates":{}}"""
    private val paid = """{"version":2,"enabled":false,"buckets":{},"scenes":{},"templates":{}}"""

    @Test fun `selects the last valid snapshot for the requested segment`() {
        assertEquals(paid, LocalPushSnapshotPolicy.select("paid", organic, paid))
        assertEquals(organic, LocalPushSnapshotPolicy.select("organic", organic, paid))
    }

    @Test fun `malformed or incomplete snapshot falls back to built in`() {
        assertNull(LocalPushSnapshotPolicy.select("paid", organic, "{"))
        assertNull(LocalPushSnapshotPolicy.select("organic", "{}", paid))
    }

    @Test fun `snapshot with invalid scheduled time is rejected`() {
        val invalid = """{"enabled":true,"buckets":{},"templates":{},"scenes":{"scheduled_push_1":{"times":["25:00"]}}}"""
        assertNull(LocalPushSnapshotPolicy.select("organic", invalid, null))
    }
}
