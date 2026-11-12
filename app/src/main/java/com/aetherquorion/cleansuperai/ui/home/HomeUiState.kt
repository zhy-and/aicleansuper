package com.aetherquorion.cleansuperai.ui.home

import com.aetherquorion.cleansuperai.data.model.MediaScanSummary

data class HomeUiState(
    val scanStatus: ScanStatus = ScanStatus.IDLE,
    val scanProgress: Int = 0,
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
