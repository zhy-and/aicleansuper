package com.example.cleansuperai.data.model

data class SimilarPhotoGroup(
    val groupId: String,
    val bucketName: String,
    val items: List<SimilarPhotoItem>,
    val recommendedKeepId: Long,
    val estimatedReclaimBytes: Long,
) {
    val recommendedKeep: SimilarPhotoItem
        get() = items.first { it.id == recommendedKeepId }
}
