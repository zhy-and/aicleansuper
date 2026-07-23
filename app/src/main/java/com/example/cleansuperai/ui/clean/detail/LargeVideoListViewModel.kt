package com.example.cleansuperai.ui.clean.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleansuperai.data.scanner.AndroidMediaScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LargeVideoListViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner by lazy { AndroidMediaScanner(application.applicationContext) }

    private val _uiState = MutableStateFlow(LargeVideoListUiState())
    val uiState: StateFlow<LargeVideoListUiState> = _uiState.asStateFlow()

    fun load(force: Boolean = false) {
        if (!force && (_uiState.value.isLoading || _uiState.value.items.isNotEmpty())) return

        viewModelScope.launch {
            _uiState.value = LargeVideoListUiState(isLoading = true)
            runCatching { scanner.loadLargeVideoItems() }
                .onSuccess { items ->
                    _uiState.value = LargeVideoListUiState(
                        isLoading = false,
                        items = items,
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = LargeVideoListUiState(
                        isLoading = false,
                        errorMessage = throwable.message,
                    )
                }
        }
    }
}
