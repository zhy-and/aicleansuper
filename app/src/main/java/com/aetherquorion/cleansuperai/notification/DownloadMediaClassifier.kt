package com.aetherquorion.cleansuperai.notification

/**
 * Download/ 路径判定与 MIME/后缀分类，供 DownloadManager / MediaStore 两条链路共用。
 */
object DownloadMediaClassifier {

    fun isDownloadPath(path: String): Boolean {
        val p = path.lowercase().replace('\\', '/')
        return p.contains("download/") ||
            p.contains("/download") ||
            p.contains("downloads/") ||
            p.contains("/downloads") ||
            p == "download" ||
            p == "downloads"
    }

    fun classify(
        mime: String,
        name: String,
        pathOrUri: String,
    ): LocalEventPushHelper.EventType {
        val hay = "$mime $name $pathOrUri".lowercase()
        return when {
            mime.startsWith("image/") ||
                hasExt(hay, listOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".heic", ".bmp")) ->
                LocalEventPushHelper.EventType.IMAGE_DOWNLOAD
            mime.startsWith("video/") ||
                hasExt(hay, listOf(".mp4", ".mkv", ".avi", ".mov", ".webm", ".3gp")) ->
                LocalEventPushHelper.EventType.VIDEO_DOWNLOAD
            mime.startsWith("audio/") ||
                hasExt(hay, listOf(".mp3", ".aac", ".wav", ".flac", ".m4a", ".ogg")) ->
                LocalEventPushHelper.EventType.AUDIO_DOWNLOAD
            else -> LocalEventPushHelper.EventType.FILE_DOWNLOAD
        }
    }

    private fun hasExt(hay: String, exts: List<String>): Boolean = exts.any { hay.contains(it) }
}
