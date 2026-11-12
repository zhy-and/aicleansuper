package com.aetherquorion.cleansuperai.vault

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.media.MediaScannerConnection
import android.os.Environment
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

object VaultFileRepository {
    const val DIR_NAME = "private_vault"
    private const val RESTORE_FOLDER = "PurGuClean"

    fun vaultDir(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        val nomedia = File(dir, ".nomedia")
        if (!nomedia.exists()) {
            runCatching { nomedia.createNewFile() }
        }
        return dir
    }

    data class UriImportResult(val imported: Boolean, val originalRemoved: Boolean)

    fun importUri(context: Context, uri: Uri): UriImportResult {
        val resolver = context.contentResolver
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "media_${System.currentTimeMillis()}"
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val id = UUID.randomUUID().toString().replace("-", "")
        val extension = name.substringAfterLast('.', "").lowercase(Locale.US)
        val relative = if (extension.isEmpty()) id else "$id.$extension"
        val destination = File(vaultDir(context), relative)
        try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            } ?: return UriImportResult(false, false)
            if (!destination.exists() || destination.length() == 0L) {
                destination.delete()
                return UriImportResult(false, false)
            }
            val item = VaultMediaItem(id, relative, name, mime, destination.length(),
                System.currentTimeMillis(), mime.startsWith("video/"))
            VaultStore.add(item)
            if (VaultStore.list().none { it.id == id }) {
                destination.delete()
                return UriImportResult(false, false)
            }
            val removed = runCatching {
                DocumentsContract.deleteDocument(resolver, uri) || resolver.delete(uri, null, null) > 0
            }.getOrDefault(false)
            return UriImportResult(true, removed)
        } catch (_: Exception) {
            destination.delete()
            return UriImportResult(false, false)
        }
    }

    fun importFiles(
        context: Context,
        sources: List<File>,
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): ImportResult {
        var success = 0
        var failed = 0
        val dir = vaultDir(context)
        sources.forEachIndexed { index, source ->
            try {
                if (!source.exists() || !source.isFile) {
                    failed++
                    return@forEachIndexed
                }
                val ext = source.extension.lowercase(Locale.US)
                val id = UUID.randomUUID().toString().replace("-", "")
                val relative = if (ext.isBlank()) id else "$id.$ext"
                val dest = File(dir, relative)
                copyFile(source, dest)
                if (!dest.exists() || (dest.length() == 0L && source.length() > 0L)) {
                    dest.delete()
                    failed++
                    return@forEachIndexed
                }
                val isVideo = isVideoExt(ext)
                val mime = mimeForExt(ext, isVideo)
                VaultStore.add(
                    VaultMediaItem(
                        id = id,
                        vaultRelativePath = relative,
                        displayName = source.name,
                        mimeType = mime,
                        sizeBytes = dest.length(),
                        importedAt = System.currentTimeMillis(),
                        isVideo = isVideo
                    )
                )
                if (source.delete()) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(source.absolutePath),
                        arrayOf(mime),
                        null
                    )
                }
                success++
            } catch (_: Throwable) {
                failed++
            } finally {
                onProgress(index + 1, sources.size)
            }
        }
        return ImportResult(success, failed)
    }

    data class DeleteResult(val deleted: Int, val freedBytes: Long)

    fun deleteItems(context: Context, items: List<VaultMediaItem>): DeleteResult {
        var bytes = 0L
        val dir = vaultDir(context)
        val removed = mutableListOf<String>()
        items.forEach { item ->
            runCatching {
                val file = File(dir, item.vaultRelativePath)
                val size = if (file.exists()) file.length() else 0L
                if (!file.exists() || file.delete()) {
                    bytes += size
                    removed += item.id
                }
            }
        }
        VaultStore.remove(removed)
        return DeleteResult(removed.size, bytes)
    }

    fun restoreItems(
        context: Context,
        items: List<VaultMediaItem>,
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): Int {
        var restored = 0
        val dir = vaultDir(context)
        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val outDir = File(pictures, RESTORE_FOLDER)
        if (!outDir.exists()) outDir.mkdirs()
        items.forEachIndexed { index, item ->
            try {
                val source = File(dir, item.vaultRelativePath)
                if (!source.exists()) throw IllegalStateException("Missing vault file")
                val safeName = item.displayName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                var dest = File(outDir, safeName)
                if (dest.exists()) {
                    val base = dest.nameWithoutExtension
                    val ext = dest.extension
                    dest = File(
                        outDir,
                        if (ext.isBlank()) "${base}_${System.currentTimeMillis()}"
                        else "${base}_${System.currentTimeMillis()}.$ext"
                    )
                }
                copyFile(source, dest)
                if (!dest.exists()) throw IllegalStateException("Restore copy failed")
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(dest.absolutePath),
                    arrayOf(item.mimeType),
                    null
                )
                source.delete()
                VaultStore.remove(listOf(item.id))
                restored++
            } catch (_: Throwable) {
                // Continue restoring the remaining files and surface aggregate success at the end.
            } finally {
                onProgress(index + 1, items.size)
            }
        }
        return restored
    }

    fun listExisting(context: Context): List<VaultMediaItem> {
        val dir = vaultDir(context)
        val items = VaultStore.list()
        val existing = items.filter { File(dir, it.vaultRelativePath).exists() }
        if (existing.size != items.size) {
            val keep = existing.map { it.id }.toSet()
            VaultStore.remove(items.map { it.id }.filterNot { it in keep })
        }
        return existing
    }

    private fun copyFile(source: File, dest: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun isVideoExt(ext: String): Boolean {
        return ext in setOf("mp4", "mkv", "avi", "mov", "3gp", "webm")
    }

    private fun mimeForExt(ext: String, isVideo: Boolean): String {
        val fromMap = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        if (!fromMap.isNullOrBlank()) return fromMap
        return if (isVideo) "video/*" else "image/*"
    }

    data class ImportResult(val successCount: Int, val failedCount: Int)
}
