package com.aetherquorion.cleansuperai.analytics

import android.content.Context
import android.os.Process
import java.util.UUID

object ProcessHeartbeatReporter {
    private val sessionId = UUID.randomUUID().toString()
    private val processStartMs = System.currentTimeMillis()
    private var heartbeatIndex = 0

    fun currentSessionId(context: Context): String = sessionId
    fun reportProcessHeartbeat(context: Context) {
        val now = System.currentTimeMillis()
        val index = synchronized(this) { ++heartbeatIndex }
        CenterReporter.report(context, "10019", "进程保活心跳", mapOf(
            "session_id" to sessionId, "pid" to Process.myPid().toString(),
            "process_start_time" to processStartMs.toString(), "event_time_ms" to now.toString(),
            "alive_duration_ms" to (now - processStartMs).toString(),
            "alive_duration_min" to ((now - processStartMs) / 60_000).toString(),
            "heartbeat_index" to index.toString()))
    }
    fun reportFcmReceived(context: Context, eventName: String, deliveredPriority: String,
        type: String, highPriority: Boolean, wakeMessage: Boolean, fcmWakeEnabled: Boolean,
        priority: Int, originalPriority: Int) {
        CenterReporter.report(context, "10018", eventName, mapOf(
            "session_id" to sessionId, "pid" to Process.myPid().toString(),
            "fcm_priority" to priority.toString(), "fcm_original_priority" to originalPriority.toString(),
            "fcm_delivered_priority" to deliveredPriority, "fcm_type" to type,
            "is_high_priority" to if (highPriority) "1" else "0",
            "is_wake_message" to if (wakeMessage) "1" else "0",
            "fcm_wake_enabled" to if (fcmWakeEnabled) "1" else "0"))
    }
}
