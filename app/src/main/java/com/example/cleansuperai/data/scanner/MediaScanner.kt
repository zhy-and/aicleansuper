package com.example.cleansuperai.data.scanner

import com.example.cleansuperai.data.model.LargeVideoItem
import com.example.cleansuperai.data.model.MediaScanSummary
import com.example.cleansuperai.data.model.ScreenshotItem
import com.example.cleansuperai.data.model.SimilarPhotoGroup

interface MediaScanner {
    suspend fun scan(): MediaScanSummary
    suspend fun loadScreenshotItems(): List<ScreenshotItem>
    suspend fun loadLargeVideoItems(): List<LargeVideoItem>
    suspend fun loadSimilarPhotoGroups(): List<SimilarPhotoGroup>
    suspend fun loadDuplicatePhotoGroups(): List<SimilarPhotoGroup>
    suspend fun loadDuplicateVideoGroups(): List<SimilarPhotoGroup>
}
