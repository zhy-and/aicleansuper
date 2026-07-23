package com.example.cleansuperai.ui.similar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleansuperai.data.model.SimilarMediaMode
import com.example.cleansuperai.data.scanner.AndroidMediaScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SimilarPhotosViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner by lazy { AndroidMediaScanner(application.applicationContext) }

    private val _uiState = MutableStateFlow(SimilarPhotosUiState())
    val uiState: StateFlow<SimilarPhotosUiState> = _uiState.asStateFlow()

    fun load(mode: SimilarMediaMode, force: Boolean = false) {
        val current = _uiState.value
        if (!force && (current.isLoading || (current.groups.isNotEmpty() && current.mode == mode))) {
            return
        }

        viewModelScope.launch {
            _uiState.value = SimilarPhotosUiState(isLoading = true, mode = mode)
            runCatching {
                when (mode) {
                    SimilarMediaMode.SIMILAR_PHOTOS -> scanner.loadSimilarPhotoGroups()
                    SimilarMediaMode.DUPLICATE_PHOTOS -> scanner.loadDuplicatePhotoGroups()
                    SimilarMediaMode.DUPLICATE_VIDEOS -> scanner.loadDuplicateVideoGroups()
                }
            }.onSuccess { groups ->
                _uiState.value = SimilarPhotosUiState(
                    isLoading = false,
                    mode = mode,
                    groups = groups,
                )
            }.onFailure { throwable ->
                _uiState.value = SimilarPhotosUiState(
                    isLoading = false,
                    mode = mode,
                    errorMessage = throwable.message,
                )
            }
        }
    }
}
