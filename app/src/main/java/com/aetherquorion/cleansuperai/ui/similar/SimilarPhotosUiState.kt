package com.aetherquorion.cleansuperai.ui.similar

import com.aetherquorion.cleansuperai.data.model.SimilarMediaMode
import com.aetherquorion.cleansuperai.data.model.SimilarPhotoGroup

data class SimilarPhotosUiState(
    val isLoading: Boolean = false,
    val mode: SimilarMediaMode = SimilarMediaMode.SIMILAR_PHOTOS,
    val groups: List<SimilarPhotoGroup> = emptyList(),
    val errorMessage: String? = null,
)
