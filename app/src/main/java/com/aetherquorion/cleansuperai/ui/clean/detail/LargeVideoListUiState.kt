package com.aetherquorion.cleansuperai.ui.clean.detail

import com.aetherquorion.cleansuperai.data.model.LargeVideoItem

data class LargeVideoListUiState(
    val isLoading: Boolean = false,
    val items: List<LargeVideoItem> = emptyList(),
    val errorMessage: String? = null,
)
