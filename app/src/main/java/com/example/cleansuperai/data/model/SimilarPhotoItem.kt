package com.example.cleansuperai.data.model

import android.net.Uri

data class SimilarPhotoItem(
    val id: Long,
    val displayName: String,
    val bucketName: String,
    val sizeBytes: Long,
    val dateModifiedMs: Long,
    val width: Int,
    val height: Int,
    val contentUri: Uri,
    val durationMs: Long = 0L,
)
