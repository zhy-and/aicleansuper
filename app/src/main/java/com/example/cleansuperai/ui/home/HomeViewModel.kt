package com.example.cleansuperai.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleansuperai.data.scanner.AndroidMediaScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner by lazy { AndroidMediaScanner(application.applicationContext) }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun scanMedia() {
        if (_uiState.value.scanStatus == HomeUiState.ScanStatus.SCANNING) return

        viewModelScope.launch {
            _uiState.value = HomeUiState(scanStatus = HomeUiState.ScanStatus.SCANNING)

            runCatching { scanner.scan() }
                .onSuccess { summary ->
                    _uiState.value = HomeUiState(
                        scanStatus = HomeUiState.ScanStatus.DONE,
                        summary = summary,
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = HomeUiState(
                        scanStatus = HomeUiState.ScanStatus.ERROR,
                        errorMessage = throwable.message,
                    )
                }
        }
    }
}
