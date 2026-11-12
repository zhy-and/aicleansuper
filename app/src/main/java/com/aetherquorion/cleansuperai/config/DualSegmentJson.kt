package com.aetherquorion.cleansuperai.config

import com.google.gson.JsonObject
import com.google.gson.JsonParser

object DualSegmentJson {
    const val KEY_ORGANIC = "organic"
    const val KEY_PAID = "paid"

    fun wrapIdentical(segmentJson: String, version: Int = 1): String {
        val segment = JsonParser.parseString(segmentJson)
        require(segment.isJsonObject)
        return JsonObject().apply {
            addProperty("version", version)
            add(KEY_ORGANIC, segment)
            add(KEY_PAID, segment.deepCopy())
        }.toString()
    }

    fun extract(raw: String, preferPaid: Boolean): String? = runCatching {
        val root = JsonParser.parseString(raw).asJsonObject
        if (!root.has(KEY_ORGANIC) && !root.has(KEY_PAID)) return@runCatching root.toString()
        val selected = if (preferPaid) {
            root.get(KEY_PAID)?.takeIf { it.isJsonObject }
                ?: root.get(KEY_ORGANIC)?.takeIf { it.isJsonObject }
        } else root.get(KEY_ORGANIC)?.takeIf { it.isJsonObject }
        selected?.asJsonObject?.toString()
    }.getOrNull()
}
