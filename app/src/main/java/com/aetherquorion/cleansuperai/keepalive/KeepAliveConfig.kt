package com.aetherquorion.cleansuperai.keepalive

import android.content.Context

/** Purgu kp_config_v1 switch names and review defaults. The config owner may write these to preferences. */
object KeepAliveConfig {
    const val FCM_TOPIC = "purgu_wake"
    const val QUICK_JOB_MIN_LATENCY_MS = 3_999L
    const val QUICK_JOB_DEADLINE_MS = 5_999L
    const val HEARTBEAT_MIN_LATENCY_MS = 1_446_000L
    const val HEARTBEAT_DEADLINE_MS = 1_449_999L
    const val ALARM_INTERVAL_MS = 28_799_365L

    private const val PREFS = "keepalive_switches"
    private val defaults = linkedMapOf(
        "kpa_boot_switch" to true,
        "kpa_service_switch" to true,
        "kpa_resume_switch" to true,
        "kpa_alarm_switch" to false,
        "kpa_job_switch" to false,
        "kpa_fcm_switch" to false,
        "kpa_home_switch" to false,
        "kpa_delete_intent_switch" to true,
    )

    fun enabled(context: Context, key: String): Boolean {
        val fallback = defaults[key] ?: false
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, if (fallback) "1" else "0") == "1"
    }

    /** Called by the kp_config_v1 adapter after segment selection. */
    fun applySwitches(context: Context, switches: Map<String, String>) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        switches.forEach { (key, value) ->
            if (key in defaults && (value == "0" || value == "1")) editor.putString(key, value)
        }
        editor.apply()
    }

    fun snapshot(context: Context): Map<String, String> = defaults.mapValues { (key, _) ->
        if (enabled(context, key)) "1" else "0"
    }
}
