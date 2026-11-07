package com.aetherquorion.cleansuperai.vault

import java.io.File

data class VaultMediaItem(
    val id: String,
    val vaultRelativePath: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val importedAt: Long,
    val isVideo: Boolean,
    var selected: Boolean = false
) {
    fun file(filesDir: File): File = File(File(filesDir, VaultFileRepository.DIR_NAME), vaultRelativePath)
}
