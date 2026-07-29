package com.aetherquorion.cleansuperai.ui.clean.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aetherquorion.cleansuperai.data.scanner.AndroidMediaScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScreenshotListViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner by lazy { AndroidMediaScanner(application.applicationContext) }

    private val _uiState = MutableStateFlow(ScreenshotListUiState())
    val uiState: StateFlow<ScreenshotListUiState> = _uiState.asStateFlow()

    fun load(force: Boolean = false) {
        if (!force && (_uiState.value.isLoading || _uiState.value.items.isNotEmpty())) return

        viewModelScope.launch {
            _uiState.value = ScreenshotListUiState(isLoading = true)
            runCatching { scanner.loadScreenshotItems() }
                .onSuccess { items ->
                    _uiState.value = ScreenshotListUiState(
                        isLoading = false,
                        items = items,
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = ScreenshotListUiState(
                        isLoading = false,
                        errorMessage = throwable.message,
                    )
                }
        }
    }
}
