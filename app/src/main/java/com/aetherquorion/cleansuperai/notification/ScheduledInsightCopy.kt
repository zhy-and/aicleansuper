package com.aetherquorion.cleansuperai.notification

import java.util.Locale
import kotlin.random.Random

/** 示例量级的提醒数字；每次实际展示时重新生成，同一条通知的折叠与展开态共用一份。 */
internal object ScheduledInsightCopy {
    val keys = listOf(
        "scheduled_storage_clean", "scheduled_large_files", "scheduled_duplicate_photos",
        "scheduled_similar_photos", "scheduled_videos", "scheduled_audio",
        "scheduled_apps", "scheduled_data",
    )

    fun isInsight(key: String): Boolean = key in keys

    fun sample(key: String, random: Random = Random.Default): Map<String, String> {
        fun gb(from: Int, to: Int) = String.format(Locale.US, "%.1fGB", random.nextInt(from, to + 1) / 10.0)
        fun mb(from: Int, to: Int) = "${random.nextInt(from, to + 1) / 10 * 10}MB"
        return when (key) {
            "scheduled_storage_clean" -> mapOf("size" to gb(8, 45))
            "scheduled_large_files" -> mapOf("total" to gb(12, 79))
            "scheduled_duplicate_photos" -> mapOf(
                "groups" to random.nextInt(12, 55).toString(),
                "size" to gb(3, 20),
            )
            "scheduled_similar_photos" -> mapOf("size" to mb(150, 990))
            "scheduled_videos" -> mapOf("total" to gb(25, 126), "n" to random.nextInt(2, 7).toString())
            "scheduled_audio" -> mapOf("total" to gb(2, 14))
            "scheduled_apps" -> mapOf("size" to gb(5, 28), "idle" to random.nextInt(5, 19).toString())
            "scheduled_data" -> mapOf("back" to mb(150, 960))
            else -> emptyMap()
        }
    }

    data class Rendered(val text: String, val highlights: List<IntRange>)

    fun render(template: String, values: Map<String, String>): Rendered {
        val output = StringBuilder()
        val highlights = mutableListOf<IntRange>()
        var cursor = 0
        val token = Regex("\\{([a-z]+)\\}")
        token.findAll(template).forEach { match ->
            output.append(template, cursor, match.range.first)
            val replacement = values[match.groupValues[1]] ?: match.value
            val start = output.length
            output.append(replacement)
            if (replacement != match.value) highlights += start until output.length
            cursor = match.range.last + 1
        }
        output.append(template, cursor, template.length)
        return Rendered(output.toString(), highlights)
    }
}
