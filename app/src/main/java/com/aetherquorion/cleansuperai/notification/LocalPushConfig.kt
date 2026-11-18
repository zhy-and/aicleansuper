package com.aetherquorion.cleansuperai.notification

import android.content.Context
import com.aetherquorion.cleansuperai.R
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/** Local defaults include the original 39 scenes and seven fixed-time feature reminders. */
object LocalPushConfig {
    private val addedFixedScenes = setOf(
        "scheduled_cache_scan", "scheduled_photo_compress", "scheduled_video_compress",
        "scheduled_image_enhance", "scheduled_contacts_manage",
        "scheduled_calendar_manage", "scheduled_speed_test",
    )
    data class Config(
        val enabled: Boolean = true,
        val global: Global = Global(),
        val buckets: Map<String, Bucket> = emptyMap(),
        val scenes: Map<String, Scene> = emptyMap(),
        val templates: Map<String, Template> = emptyMap(),
    )
    data class Global(
        @SerializedName("first_max") val firstMax: Int = 20,
        @SerializedName("daily_max") val dailyMax: Int = 20,
        @SerializedName("no_click_stop") val noClickStop: Int = 10,
        @SerializedName("cooldown_sec") val cooldownSec: Int = 0,
        @SerializedName("max_visible") val maxVisible: Int = 5,
        @SerializedName("skip_foreground") val skipForeground: Boolean = true,
        @SerializedName("install_age_sec") val installAgeSec: Int = 0,
        @SerializedName("repeat_submit") val repeatSubmit: RepeatSubmitConfig? = null,
    )
    data class RepeatSubmitConfig(
        val enabled: Boolean = false,
        @SerializedName("interval_ms") val intervalMs: Int? = null,
        @SerializedName("first_day_extra_count") val firstDayExtraCount: Int? = null,
        @SerializedName("later_day_extra_count") val laterDayExtraCount: Int? = null,
        @SerializedName("samsung_first_day_extra_count") val samsungFirstDayExtraCount: Int? = null,
        @SerializedName("samsung_later_day_extra_count") val samsungLaterDayExtraCount: Int? = null,
    )
    data class Bucket(
        val enabled: Boolean = true,
        @SerializedName("interval_sec") val intervalSec: Long = -1,
        @SerializedName("daily_max") val dailyMax: Int = -1,
        @SerializedName("no_click_stop") val noClickStop: Int = 10,
        @SerializedName("cooldown_sec") val cooldownSec: Long = -1,
        val repetitions: Int = 1,
        @SerializedName("max_visible") val maxVisible: Int = 1,
    )
    data class Scene(
        val enabled: Boolean = true,
        val bucket: String = "",
        val periodic: Boolean = false,
        val priority: Int = 0,
        @SerializedName("daily_max") val dailyMax: Int = -1,
        @SerializedName("cooldown_sec") val cooldownSec: Long = -1,
        @SerializedName("template_ids") val templateIds: List<String> = emptyList(),
        val router: String = "",
        val times: List<String> = emptyList(),
        @SerializedName("delay_sec") val delaySec: Int = -1,
        @SerializedName("repeat_submit_enabled") val repeatSubmitEnabled: Boolean? = null,
    )
    data class Template(
        val layout: String = "small",
        @SerializedName("button_bg") val buttonBg: String = "",
        @SerializedName("button_text") val buttonText: String = "",
        @SerializedName("button_anim") val buttonAnim: String = "scale",
    )

    @Volatile private var active: Config? = null
    fun current(context: Context): Config = active ?: synchronized(this) {
        active ?: Gson().fromJson(defaultSegmentJson(context), Config::class.java).also { active = it }
    }
    fun defaultSegmentJson(context: Context): String = context.resources.openRawResource(R.raw.local_push_default)
        .bufferedReader().use { it.readText() }
    fun defaultConfigJson(context: Context): String {
        val segment = defaultSegmentJson(context)
        return "{\"organic\":$segment,\"paid\":$segment}"
    }
    /** Accepts the FRC dual-segment envelope; caller determines the user's segment. */
    fun applyJson(raw: String, segment: String = "organic", context: Context? = null): Boolean = runCatching {
        val root = com.google.gson.JsonParser.parseString(raw).asJsonObject
        val selected = if (root.has("scenes")) root else root.getAsJsonObject(segment)
        require(LocalPushSnapshotPolicy.isValid(selected.toString()))
        val parsed = Gson().fromJson(selected, Config::class.java)
        require(parsed.scenes.isNotEmpty() && parsed.buckets.isNotEmpty())
        val defaults = context?.let { Gson().fromJson(defaultSegmentJson(it), Config::class.java) }
        active = if (defaults == null) parsed else parsed.copy(scenes =
            parsed.scenes + defaults.scenes.filterKeys { it in addedFixedScenes && it !in parsed.scenes })
        if (parsed.global.repeatSubmit?.enabled != true) LocalPushRepeatSubmitter.cancelAll()
        true
    }.getOrDefault(false)
}
