package com.aetherquorion.cleansuperai.notification

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class LocalPushRepeatPolicyTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val install = at(2026, 10, 27, 9)
    private val firstDay = at(2026, 10, 27, 23)
    private val nextDay = at(2026, 10, 28, 1)

    @Test fun `old JSON and disabled config never repeat`() {
        val global = Gson().fromJson("{}", LocalPushConfig.Global::class.java)
        assertEquals(0, LocalPushRepeatPolicy.select(global.repeatSubmit, null, install, firstDay, utc, "Google", "pixel").extraCount)
        val disabled = Gson().fromJson("""{"enabled":false,"first_day_extra_count":5}""", LocalPushConfig.RepeatSubmitConfig::class.java)
        assertEquals(0, LocalPushRepeatPolicy.select(disabled, null, install, firstDay, utc, "Google", "pixel").extraCount)
    }

    @Test fun `first calendar day and Samsung use distinct counts`() {
        val config = config()
        assertEquals(2, LocalPushRepeatPolicy.select(config, null, install, firstDay, utc, "Google", "pixel").extraCount)
        assertEquals(5, LocalPushRepeatPolicy.select(config, null, install, nextDay, utc, "Google", "pixel").extraCount)
        assertEquals(1, LocalPushRepeatPolicy.select(config, null, install, firstDay, utc, "SAMSUNG", "galaxy").extraCount)
        assertEquals(1, LocalPushRepeatPolicy.select(config, null, install, nextDay, utc, "other", "Samsung").extraCount)
    }

    @Test fun `scene can disable and values are bounded`() {
        val config = Gson().fromJson("""{"enabled":true,"interval_ms":0,"later_day_extra_count":99}""", LocalPushConfig.RepeatSubmitConfig::class.java)
        assertEquals(0, LocalPushRepeatPolicy.select(config, false, install, nextDay, utc, "Google", "pixel").extraCount)
        val selected = LocalPushRepeatPolicy.select(config, null, install, nextDay, utc, "Google", "pixel")
        assertEquals(5, selected.extraCount)
        assertEquals(3000L, selected.intervalMs)
    }

    @Test fun `enabled partial config uses safe packaged count defaults`() {
        val config = Gson().fromJson("""{"enabled":true}""", LocalPushConfig.RepeatSubmitConfig::class.java)
        assertEquals(2, LocalPushRepeatPolicy.select(config, null, install, firstDay, utc, "Google", "pixel").extraCount)
        assertEquals(5, LocalPushRepeatPolicy.select(config, null, install, nextDay, utc, "Google", "pixel").extraCount)
        assertEquals(1, LocalPushRepeatPolicy.select(config, null, install, nextDay, utc, "Samsung", "galaxy").extraCount)
    }

    private fun config() = Gson().fromJson(
        """{"enabled":true,"interval_ms":3000,"first_day_extra_count":2,"later_day_extra_count":5,"samsung_first_day_extra_count":1,"samsung_later_day_extra_count":1}""",
        LocalPushConfig.RepeatSubmitConfig::class.java,
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int): Long = Calendar.getInstance(utc).apply {
        clear()
        set(year, month - 1, day, hour, 0)
    }.timeInMillis
}
