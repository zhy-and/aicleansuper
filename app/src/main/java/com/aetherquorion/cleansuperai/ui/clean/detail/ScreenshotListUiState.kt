package com.aetherquorion.cleansuperai.ui.clean.detail

import com.aetherquorion.cleansuperai.data.model.ScreenshotItem

data class ScreenshotListUiState(
    val isLoading: Boolean = false,
    val items: List<ScreenshotItem> = emptyList(),
    val errorMessage: String? = null,
)
