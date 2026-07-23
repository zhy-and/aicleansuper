package com.example.cleansuperai.ui.clean.detail

import android.net.Uri

data class DisplayMediaItem(
    val stableId: Long,
    val thumbnailUri: Uri,
    val title: String,
    val metaPrimary: String,
    val metaSecondary: String,
    val sizeText: String,
    val isVideo: Boolean = false,
    val isSelectable: Boolean = false,
    val isSelected: Boolean = false,
)
