package com.example.cleansuperai.data.model

import android.net.Uri

data class LargeVideoItem(
    val id: Long,
    val displayName: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val dateModifiedMs: Long,
    val contentUri: Uri,
)
