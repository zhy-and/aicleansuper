package com.aetherquorion.cleansuperai.data.model

data class MediaScanSummary(
    val storageOverview: StorageOverview,
    val totalImages: Int,
    val totalVideos: Int,
    val screenshotCount: Int,
    val screenshotBytes: Long,
    val largeVideoCount: Int,
    val largeVideoBytes: Long,
    val similarGroupCount: Int,
    val similarPhotoCount: Int,
    val similarReclaimBytes: Long,
    val duplicatePhotoGroupCount: Int = 0,
    val duplicatePhotoCount: Int = 0,
    val duplicatePhotoReclaimBytes: Long = 0L,
    val duplicateVideoGroupCount: Int = 0,
    val duplicateVideoCount: Int = 0,
    val duplicateVideoReclaimBytes: Long = 0L,
) {
    val estimatedReclaimBytes: Long
        get() = screenshotBytes + largeVideoBytes + similarReclaimBytes +
            duplicatePhotoReclaimBytes + duplicateVideoReclaimBytes

    val mediaItemCount: Int
        get() = totalImages + totalVideos
}
