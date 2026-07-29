package com.aetherquorion.cleansuperai.data.scanner

import com.aetherquorion.cleansuperai.data.model.LargeVideoItem
import com.aetherquorion.cleansuperai.data.model.MediaScanSummary
import com.aetherquorion.cleansuperai.data.model.ScreenshotItem
import com.aetherquorion.cleansuperai.data.model.SimilarPhotoGroup

interface MediaScanner {
    suspend fun scan(): MediaScanSummary
    suspend fun loadScreenshotItems(): List<ScreenshotItem>
    suspend fun loadLargeVideoItems(): List<LargeVideoItem>
    suspend fun loadSimilarPhotoGroups(): List<SimilarPhotoGroup>
    suspend fun loadDuplicatePhotoGroups(): List<SimilarPhotoGroup>
    suspend fun loadDuplicateVideoGroups(): List<SimilarPhotoGroup>
}
