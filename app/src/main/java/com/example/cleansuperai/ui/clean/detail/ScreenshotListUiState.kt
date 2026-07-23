package com.example.cleansuperai.ui.clean.detail

import com.example.cleansuperai.data.model.ScreenshotItem

data class ScreenshotListUiState(
    val isLoading: Boolean = false,
    val items: List<ScreenshotItem> = emptyList(),
    val errorMessage: String? = null,
)
