package com.aetherquorion.cleansuperai.data.model

import android.net.Uri

data class ScreenshotItem(
    val id: Long,
    val displayName: String,
    val bucketName: String,
    val sizeBytes: Long,
    val dateModifiedMs: Long,
    val contentUri: Uri,
)
