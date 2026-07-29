package com.aetherquorion.cleansuperai.ui.clean

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aetherquorion.cleansuperai.data.scanner.AndroidMediaScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CleanCenterViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner by lazy { AndroidMediaScanner(application.applicationContext) }

    private val _uiState = MutableStateFlow(CleanCenterUiState())
    val uiState: StateFlow<CleanCenterUiState> = _uiState.asStateFlow()

    fun updatePermission(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermission = hasPermission)
        if (hasPermission && _uiState.value.summary == null && !_uiState.value.isLoading) {
            loadSummary()
        }
    }

    fun loadSummary() {
        if (_uiState.value.isLoading || !_uiState.value.hasPermission) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { scanner.scan() }
                .onSuccess { summary ->
                    _uiState.value = CleanCenterUiState(
                        hasPermission = true,
                        isLoading = false,
                        summary = summary,
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = CleanCenterUiState(
                        hasPermission = true,
                        isLoading = false,
                        errorMessage = throwable.message,
                    )
                }
        }
    }
}
