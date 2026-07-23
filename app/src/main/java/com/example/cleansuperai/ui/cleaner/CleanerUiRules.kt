package com.example.cleansuperai.ui.cleaner

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class CleanerDestination(val label: String) {
    HOME("Home"),
    SWIPE("Swipe"),
    COMPRESS("Compress"),
    TOOLS("Tools"),
}

data class PhotoRecord(
    val id: Long,
    val dateModifiedMs: Long,
)

data class PhotoMonthGroup(
    val label: String,
    val photos: List<PhotoRecord>,
)

object PhotoMonthGrouper {
    fun group(
        photos: List<PhotoRecord>,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault(),
    ): List<PhotoMonthGroup> {
        val formatter = SimpleDateFormat("MMMM yyyy", locale).apply {
            this.timeZone = timeZone
        }
        return photos
            .sortedByDescending(PhotoRecord::dateModifiedMs)
            .groupBy { photo ->
                Calendar.getInstance(timeZone).apply {
                    timeInMillis = photo.dateModifiedMs
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            .entries
            .sortedByDescending { it.key }
            .map { (monthStartMs, items) ->
                PhotoMonthGroup(
                    label = formatter.format(monthStartMs),
                    photos = items,
                )
            }
    }
}

data class CompressionResult(
    val originalBytes: Long,
    val compressedBytes: Long,
) {
    val savedBytes: Long = (originalBytes - compressedBytes).coerceAtLeast(0)
    val isWorthSaving: Boolean = compressedBytes < originalBytes
}
