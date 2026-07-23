package com.example.cleansuperai.ui.similar

import com.example.cleansuperai.data.model.SimilarMediaMode
import com.example.cleansuperai.data.model.SimilarPhotoGroup

data class SimilarPhotosUiState(
    val isLoading: Boolean = false,
    val mode: SimilarMediaMode = SimilarMediaMode.SIMILAR_PHOTOS,
    val groups: List<SimilarPhotoGroup> = emptyList(),
    val errorMessage: String? = null,
)
