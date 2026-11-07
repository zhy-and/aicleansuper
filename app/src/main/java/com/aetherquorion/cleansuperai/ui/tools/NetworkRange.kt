package com.aetherquorion.cleansuperai.ui.tools

import java.util.Calendar

enum class NetworkRange {
    MONTH, THIRTY_DAYS, DAY;

    fun start(now: Long): Long = when (this) {
        MONTH -> Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        THIRTY_DAYS -> now - 30L * 24 * 60 * 60 * 1000
        DAY -> now - 24L * 60 * 60 * 1000
    }
}
