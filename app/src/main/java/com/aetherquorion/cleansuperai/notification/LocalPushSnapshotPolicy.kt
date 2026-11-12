package com.aetherquorion.cleansuperai.notification

import com.google.gson.JsonParser

/** Reject malformed cached or remote scene documents before activating them. */
object LocalPushSnapshotPolicy {
    private val timePattern = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")

    fun select(segment: String, organicRaw: String?, paidRaw: String?): String? =
        when (segment) {
            "organic" -> organicRaw
            "paid" -> paidRaw
            else -> null
        }?.takeIf(::isValid)

    fun isValid(raw: String): Boolean = runCatching {
        val root = JsonParser.parseString(raw).asJsonObject
        if (!root.has("enabled") || !root.get("enabled").isJsonPrimitive ||
            !root.get("enabled").asJsonPrimitive.isBoolean) return false
        for (name in listOf("buckets", "scenes", "templates")) {
            if (!root.has(name) || !root.get(name).isJsonObject) return false
        }
        root.getAsJsonObject("scenes").entrySet().all { (_, value) ->
            if (!value.isJsonObject) return@all false
            val times = value.asJsonObject.get("times")
            times == null || (times.isJsonArray && times.asJsonArray.size() <= 8 &&
                times.asJsonArray.all {
                    it.isJsonPrimitive && it.asJsonPrimitive.isString && timePattern.matches(it.asString)
                })
        }
    }.getOrDefault(false)
}
