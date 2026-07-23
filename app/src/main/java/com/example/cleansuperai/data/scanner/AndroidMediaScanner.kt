package com.example.cleansuperai.data.scanner

import android.content.ContentUris
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.example.cleansuperai.R
import com.example.cleansuperai.data.model.LargeVideoItem
import com.example.cleansuperai.data.model.MediaScanSummary
import com.example.cleansuperai.data.model.ScreenshotItem
import com.example.cleansuperai.data.model.SimilarPhotoGroup
import com.example.cleansuperai.data.model.SimilarPhotoItem
import com.example.cleansuperai.data.model.StorageOverview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import java.util.Locale

class AndroidMediaScanner(
    private val context: Context,
) : MediaScanner {

    override suspend fun scan(): MediaScanSummary = withContext(Dispatchers.IO) {
        val storageOverview = loadStorageOverview()
        val imageScan = loadImageStats()
        val videoScan = loadVideoStats()
        val similarGroups = loadSimilarPhotoGroupsInternal()
        val duplicatePhotoGroups = loadDuplicatePhotoGroupsInternal()
        val duplicateVideoGroups = loadDuplicateVideoGroupsInternal()

        MediaScanSummary(
            storageOverview = storageOverview,
            totalImages = imageScan.totalCount,
            totalVideos = videoScan.totalCount,
            screenshotCount = imageScan.screenshotCount,
            screenshotBytes = imageScan.screenshotBytes,
            largeVideoCount = videoScan.largeVideoCount,
            largeVideoBytes = videoScan.largeVideoBytes,
            similarGroupCount = similarGroups.size,
            similarPhotoCount = similarGroups.sumOf { it.items.size },
            similarReclaimBytes = similarGroups.sumOf { it.estimatedReclaimBytes },
            duplicatePhotoGroupCount = duplicatePhotoGroups.size,
            duplicatePhotoCount = duplicatePhotoGroups.sumOf { it.items.size },
            duplicatePhotoReclaimBytes = duplicatePhotoGroups.sumOf { it.estimatedReclaimBytes },
            duplicateVideoGroupCount = duplicateVideoGroups.size,
            duplicateVideoCount = duplicateVideoGroups.sumOf { it.items.size },
            duplicateVideoReclaimBytes = duplicateVideoGroups.sumOf { it.estimatedReclaimBytes },
        )
    }

    override suspend fun loadScreenshotItems(): List<ScreenshotItem> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
        )

        val items = mutableListOf<ScreenshotItem>()

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val displayName = cursor.getString(nameIndex).orEmpty()
                val bucketName = cursor.getString(bucketIndex).orEmpty()

                if (isScreenshot(displayName, bucketName)) {
                    val id = cursor.getLong(idIndex)
                    items += ScreenshotItem(
                        id = id,
                        displayName = displayName,
                        bucketName = bucketName,
                        sizeBytes = cursor.getLong(sizeIndex),
                        dateModifiedMs = cursor.getLong(dateIndex) * 1000L,
                        contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id,
                        ),
                    )
                }
            }
        }

        items
    }

    override suspend fun loadLargeVideoItems(): List<LargeVideoItem> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_MODIFIED,
        )

        val items = mutableListOf<LargeVideoItem>()

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.SIZE} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val size = cursor.getLong(sizeIndex)
                if (size >= LARGE_VIDEO_THRESHOLD_BYTES) {
                    val id = cursor.getLong(idIndex)
                    items += LargeVideoItem(
                        id = id,
                        displayName = cursor.getString(nameIndex).orEmpty(),
                        sizeBytes = size,
                        durationMs = cursor.getLong(durationIndex),
                        dateModifiedMs = cursor.getLong(dateIndex) * 1000L,
                        contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id,
                        ),
                    )
                }
            }
        }

        items
    }

    override suspend fun loadSimilarPhotoGroups(): List<SimilarPhotoGroup> = withContext(Dispatchers.IO) {
        loadSimilarPhotoGroupsInternal()
    }

    override suspend fun loadDuplicatePhotoGroups(): List<SimilarPhotoGroup> = withContext(Dispatchers.IO) {
        loadDuplicatePhotoGroupsInternal()
    }

    override suspend fun loadDuplicateVideoGroups(): List<SimilarPhotoGroup> = withContext(Dispatchers.IO) {
        loadDuplicateVideoGroupsInternal()
    }

    private fun loadStorageOverview(): StorageOverview {
        val statFs = StatFs(Environment.getDataDirectory().absolutePath)
        val totalBytes = statFs.totalBytes
        val availableBytes = statFs.availableBytes
        return StorageOverview(
            totalBytes = totalBytes,
            usedBytes = totalBytes - availableBytes,
            availableBytes = availableBytes,
        )
    }

    private fun loadImageStats(): ImageScanResult {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
        )

        var totalCount = 0
        var screenshotCount = 0
        var screenshotBytes = 0L

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (cursor.moveToNext()) {
                totalCount++
                val displayName = cursor.getString(nameIndex).orEmpty()
                val bucketName = cursor.getString(bucketIndex).orEmpty()
                val size = cursor.getLong(sizeIndex)

                if (isScreenshot(displayName, bucketName)) {
                    screenshotCount++
                    screenshotBytes += size
                }
            }
        }

        return ImageScanResult(
            totalCount = totalCount,
            screenshotCount = screenshotCount,
            screenshotBytes = screenshotBytes,
        )
    }

    private fun loadVideoStats(): VideoScanResult {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.SIZE,
        )

        var totalCount = 0
        var largeVideoCount = 0
        var largeVideoBytes = 0L

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            while (cursor.moveToNext()) {
                totalCount++
                val size = cursor.getLong(sizeIndex)
                if (size >= LARGE_VIDEO_THRESHOLD_BYTES) {
                    largeVideoCount++
                    largeVideoBytes += size
                }
            }
        }

        return VideoScanResult(
            totalCount = totalCount,
            largeVideoCount = largeVideoCount,
            largeVideoBytes = largeVideoBytes,
        )
    }

    private fun loadSimilarPhotoGroupsInternal(): List<SimilarPhotoGroup> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
        )

        val images = mutableListOf<SimilarPhotoItem>()

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val displayName = cursor.getString(nameIndex).orEmpty()
                val bucketName = cursor.getString(bucketIndex).orEmpty()
                if (isScreenshot(displayName, bucketName)) continue

                val width = cursor.getInt(widthIndex)
                val height = cursor.getInt(heightIndex)
                if (width <= 0 || height <= 0) continue

                val id = cursor.getLong(idIndex)
                images += SimilarPhotoItem(
                    id = id,
                    displayName = displayName,
                    bucketName = bucketName,
                    sizeBytes = cursor.getLong(sizeIndex),
                    dateModifiedMs = cursor.getLong(dateIndex) * 1000L,
                    width = width,
                    height = height,
                    contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id,
                    ),
                )
            }
        }

        if (images.size < 2) return emptyList()

        val sorted = images.sortedWith(
            compareByDescending<SimilarPhotoItem> { it.bucketName.lowercase(Locale.ROOT) }
                .thenByDescending { it.dateModifiedMs },
        )

        val groups = mutableListOf<List<SimilarPhotoItem>>()
        var current = mutableListOf<SimilarPhotoItem>()

        sorted.forEach { image ->
            if (current.isEmpty()) {
                current.add(image)
            } else {
                val anchor = current.last()
                if (belongsToSameBurst(anchor, image)) {
                    current.add(image)
                } else {
                    if (current.size >= 2) groups += current.toList()
                    current = mutableListOf(image)
                }
            }
        }
        if (current.size >= 2) groups += current.toList()

        return groups.mapIndexedNotNull { index, items ->
            val normalized = splitByVisualSimilarity(items)
            normalized.mapIndexedNotNull { innerIndex, similarItems ->
                if (similarItems.size < 2) {
                    null
                } else {
                    val recommended = similarItems.maxByOrNull(::scoreRecommendedKeep) ?: return@mapIndexedNotNull null
                    SimilarPhotoGroup(
                        groupId = "similar_${index}_$innerIndex",
                        bucketName = recommended.bucketName,
                        items = similarItems.sortedByDescending { it.dateModifiedMs },
                        recommendedKeepId = recommended.id,
                        estimatedReclaimBytes = similarItems
                            .filterNot { it.id == recommended.id }
                            .sumOf { it.sizeBytes },
                    )
                }
            }
        }.flatten().sortedByDescending { it.estimatedReclaimBytes }
    }

    private fun loadDuplicatePhotoGroupsInternal(): List<SimilarPhotoGroup> {
        val images = loadAllPhotoItems(excludeScreenshots = true)
        if (images.size < 2) return emptyList()

        return images
            .groupBy { Triple(it.sizeBytes, it.width, it.height) }
            .filter { (key, items) ->
                key.first > 0 && key.second > 0 && key.third > 0 && items.size >= 2
            }
            .entries
            .mapIndexed { index, (_, items) ->
                toExactDuplicateGroup(
                    groupId = "duplicate_photo_$index",
                    items = items,
                )
            }
            .sortedByDescending { it.estimatedReclaimBytes }
    }

    private fun loadDuplicateVideoGroupsInternal(): List<SimilarPhotoGroup> {
        val videos = loadAllVideoItems()
        if (videos.size < 2) return emptyList()

        return videos
            .groupBy { Triple(it.sizeBytes, it.durationMs, it.width to it.height) }
            .filter { (key, items) ->
                key.first > 0 && items.size >= 2
            }
            .entries
            .mapIndexed { index, (_, items) ->
                toExactDuplicateGroup(
                    groupId = "duplicate_video_$index",
                    items = items,
                )
            }
            .sortedByDescending { it.estimatedReclaimBytes }
    }

    private fun loadAllPhotoItems(excludeScreenshots: Boolean): List<SimilarPhotoItem> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
        )

        val images = mutableListOf<SimilarPhotoItem>()
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val displayName = cursor.getString(nameIndex).orEmpty()
                val bucketName = cursor.getString(bucketIndex).orEmpty()
                if (excludeScreenshots && isScreenshot(displayName, bucketName)) continue

                val width = cursor.getInt(widthIndex)
                val height = cursor.getInt(heightIndex)
                if (width <= 0 || height <= 0) continue

                val id = cursor.getLong(idIndex)
                images += SimilarPhotoItem(
                    id = id,
                    displayName = displayName,
                    bucketName = bucketName,
                    sizeBytes = cursor.getLong(sizeIndex),
                    dateModifiedMs = cursor.getLong(dateIndex) * 1000L,
                    width = width,
                    height = height,
                    contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id,
                    ),
                )
            }
        }
        return images
    }

    private fun loadAllVideoItems(): List<SimilarPhotoItem> {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
        )

        val videos = mutableListOf<SimilarPhotoItem>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val size = cursor.getLong(sizeIndex)
                if (size <= 0L) continue

                val id = cursor.getLong(idIndex)
                videos += SimilarPhotoItem(
                    id = id,
                    displayName = cursor.getString(nameIndex).orEmpty(),
                    bucketName = cursor.getString(bucketIndex).orEmpty()
                        .ifBlank { context.getString(R.string.media_bucket_video) },
                    sizeBytes = size,
                    dateModifiedMs = cursor.getLong(dateIndex) * 1000L,
                    width = cursor.getInt(widthIndex).coerceAtLeast(0),
                    height = cursor.getInt(heightIndex).coerceAtLeast(0),
                    contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id,
                    ),
                    durationMs = cursor.getLong(durationIndex).coerceAtLeast(0L),
                )
            }
        }
        return videos
    }

    private fun toExactDuplicateGroup(
        groupId: String,
        items: List<SimilarPhotoItem>,
    ): SimilarPhotoGroup {
        val recommended = items.maxByOrNull(::scoreRecommendedKeep) ?: items.first()
        return SimilarPhotoGroup(
            groupId = groupId,
            bucketName = recommended.bucketName,
            items = items.sortedByDescending { it.dateModifiedMs },
            recommendedKeepId = recommended.id,
            estimatedReclaimBytes = items
                .filterNot { it.id == recommended.id }
                .sumOf { it.sizeBytes },
        )
    }

    private fun belongsToSameBurst(first: SimilarPhotoItem, second: SimilarPhotoItem): Boolean {
        if (!first.bucketName.equals(second.bucketName, ignoreCase = true)) return false
        val timeDiffMs = abs(first.dateModifiedMs - second.dateModifiedMs)
        if (timeDiffMs > BURST_WINDOW_MS) return false
        return isVisuallyComparable(first, second)
    }

    private fun splitByVisualSimilarity(items: List<SimilarPhotoItem>): List<List<SimilarPhotoItem>> {
        val groups = mutableListOf<MutableList<SimilarPhotoItem>>()

        items.forEach { item ->
            val targetGroup = groups.firstOrNull { group ->
                group.any { candidate -> isVisuallyComparable(candidate, item) }
            }

            if (targetGroup != null) {
                targetGroup += item
            } else {
                groups += mutableListOf(item)
            }
        }

        return groups
    }

    private fun isVisuallyComparable(first: SimilarPhotoItem, second: SimilarPhotoItem): Boolean {
        val aspectFirst = first.width.toFloat() / first.height.toFloat()
        val aspectSecond = second.width.toFloat() / second.height.toFloat()
        val aspectDelta = abs(aspectFirst - aspectSecond)
        if (aspectDelta > MAX_ASPECT_RATIO_DELTA) return false

        val widthRatio = ratio(first.width, second.width)
        val heightRatio = ratio(first.height, second.height)
        val sizeRatio = ratio(first.sizeBytes, second.sizeBytes)

        return widthRatio >= MIN_DIMENSION_RATIO &&
            heightRatio >= MIN_DIMENSION_RATIO &&
            sizeRatio >= MIN_SIZE_RATIO
    }

    private fun scoreRecommendedKeep(item: SimilarPhotoItem): Long {
        val resolutionScore = item.width.toLong() * item.height.toLong()
        return resolutionScore + item.sizeBytes + item.dateModifiedMs
    }

    private fun ratio(first: Int, second: Int): Float {
        val high = max(first, second).toFloat()
        val low = min(first, second).toFloat()
        return if (high == 0f) 0f else low / high
    }

    private fun ratio(first: Long, second: Long): Float {
        val high = max(first, second).toFloat()
        val low = min(first, second).toFloat()
        return if (high == 0f) 0f else low / high
    }

    private fun isScreenshot(displayName: String, bucketName: String): Boolean {
        val normalized = "$displayName $bucketName".lowercase(Locale.ROOT)
        return SCREENSHOT_KEYWORDS.any(normalized::contains)
    }

    private data class ImageScanResult(
        val totalCount: Int,
        val screenshotCount: Int,
        val screenshotBytes: Long,
    )

    private data class VideoScanResult(
        val totalCount: Int,
        val largeVideoCount: Int,
        val largeVideoBytes: Long,
    )

    companion object {
        private const val LARGE_VIDEO_THRESHOLD_BYTES = 100L * 1024L * 1024L
        private const val BURST_WINDOW_MS = 45_000L
        private const val MAX_ASPECT_RATIO_DELTA = 0.08f
        private const val MIN_DIMENSION_RATIO = 0.82f
        private const val MIN_SIZE_RATIO = 0.55f

        private val SCREENSHOT_KEYWORDS = listOf(
            "screenshot",
            "screen_shot",
            "screen-shot",
            "screen shot",
            "截屏",
            "截图",
        )
    }
}
