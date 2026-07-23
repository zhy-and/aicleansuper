package com.example.cleansuperai.ui.home

import com.example.cleansuperai.data.model.MediaScanSummary

data class HomeUiState(
    val scanStatus: ScanStatus = ScanStatus.IDLE,
    val summary: MediaScanSummary? = null,
    val errorMessage: String? = null,
) {
    enum class ScanStatus {
        IDLE,
        SCANNING,
        DONE,
        ERROR,
    }
}
