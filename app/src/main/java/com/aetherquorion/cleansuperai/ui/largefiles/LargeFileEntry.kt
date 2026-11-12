package com.aetherquorion.cleansuperai.ui.largefiles

enum class LargeFileType { ALL, IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, APK, OTHER }

data class LargeFileEntry(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val modifiedAt: Long,
) {
    val type: LargeFileType get() = when (name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> LargeFileType.IMAGE
        "mp4", "mkv", "avi", "mov", "3gp", "webm" -> LargeFileType.VIDEO
        "mp3", "aac", "wav", "flac", "ogg", "m4a" -> LargeFileType.AUDIO
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv" -> LargeFileType.DOCUMENT
        "zip", "rar", "7z", "tar", "gz" -> LargeFileType.ARCHIVE
        "apk" -> LargeFileType.APK
        else -> LargeFileType.OTHER
    }

    fun matches(typeFilter: LargeFileType, minBytes: Long, olderThanMs: Long?, now: Long): Boolean =
        (typeFilter == LargeFileType.ALL || type == typeFilter) &&
            sizeBytes >= minBytes &&
            (olderThanMs == null || now - modifiedAt >= olderThanMs)
}
