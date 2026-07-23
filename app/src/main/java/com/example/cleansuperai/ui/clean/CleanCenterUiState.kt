package com.example.cleansuperai.ui.clean

import com.example.cleansuperai.data.model.MediaScanSummary

data class CleanCenterUiState(
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val summary: MediaScanSummary? = null,
    val errorMessage: String? = null,
)
