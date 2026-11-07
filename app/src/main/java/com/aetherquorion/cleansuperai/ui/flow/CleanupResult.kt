package com.aetherquorion.cleansuperai.ui.flow

data class CleanupResult(val attempted: Int, val deleted: Int, val freedBytes: Long) {
    companion object {
        fun fromAttempts(sizes: List<Long>, deletedIndexes: Set<Int>): CleanupResult =
            CleanupResult(sizes.size, deletedIndexes.size,
                sizes.indices.filter { it in deletedIndexes }.sumOf { sizes[it] })
    }
}
