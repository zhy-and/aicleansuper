package com.aetherquorion.cleansuperai.notification

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.blankj.utilcode.util.LogUtils

/**
 * MediaStore 新增且落在 Download/ → image/video/audio/file_download。
 * 覆盖不走系统 DownloadManager、直接写文件的 App（微信保存等）。
 */
object MediaStoreDownloadWatcher {
    private const val TAG = "MediaStoreDownloadWatcher"
    private const val DEBOUNCE_MS = 800L
    private const val RECENT_WINDOW_SEC = 90L

    @Volatile
    private var registered = false
    private var pendingUri: Uri? = null
    private var pendingCheck: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    fun start(context: Context) {
        if (registered) {
            LogUtils.i(TAG, "已注册，跳过")
            return
        }
        val appContext = context.applicationContext
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                scheduleCheck(appContext, null)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                scheduleCheck(appContext, uri)
            }
        }
        val cr = appContext.contentResolver
        runCatching {
            cr.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
            cr.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
            cr.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
            cr.registerContentObserver(MediaStore.Files.getContentUri("external"), true, observer)
            registered = true
            LogUtils.i(TAG, "MediaStore Download 监听注册成功")
        }.onFailure {
            LogUtils.e(TAG, "注册失败：${it.message}")
        }
    }

    private fun scheduleCheck(context: Context, uri: Uri?) {
        if (uri != null) pendingUri = uri
        pendingCheck?.let { handler.removeCallbacks(it) }
        val task = Runnable {
            val target = pendingUri
            pendingUri = null
            checkDownload(context, target)
        }
        pendingCheck = task
        handler.postDelayed(task, DEBOUNCE_MS)
    }

    private fun checkDownload(context: Context, changedUri: Uri?) {
        val kind = resolveKind(changedUri)
        val row = resolveLatest(context, changedUri, kind) ?: run {
            LogUtils.w(TAG, "无媒体行 kind=$kind uri=$changedUri")
            return
        }
        val nowSec = System.currentTimeMillis() / 1000
        val newest = maxOf(row.dateAdded, row.dateModified)
        if (nowSec - newest > RECENT_WINDOW_SEC) {
            LogUtils.w(
                TAG,
                "过期忽略 id=${row.id} age=${nowSec - newest}s name=${row.name} path=${row.path}",
            )
            return
        }
        if (!DownloadMediaClassifier.isDownloadPath(row.path)) {
            return
        }
        // 截屏由 ScreenshotCaptureWatcher 负责，避免 Download 里偶发截屏名双推
        if (kind == MediaKind.IMAGE && isScreenshot(row.name, row.path)) {
            LogUtils.i(TAG, "Download 路径但是截屏，交给 ScreenshotCaptureWatcher name=${row.name}")
            return
        }
        // Files 集合里图/视/音会再被专用 URI 命中；非 none 时交给专用通道，减少重复查询噪声
        if (kind == MediaKind.FILE && row.mediaType != MediaStore.Files.FileColumns.MEDIA_TYPE_NONE) {
            val mediaType = row.mediaType
            if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ||
                mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ||
                mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO
            ) {
                return
            }
        }
        val type = DownloadMediaClassifier.classify(row.mime, row.name, row.path)
        if (!DownloadPushDeduper.tryClaim(row.name)) {
            LogUtils.i(TAG, "已推过同名下载，跳过 name=${row.name} → ${type.name}")
            return
        }
        LogUtils.i(
            TAG,
            "MediaStore Download → ${type.name} name=${row.name} path=${row.path} mime=${row.mime}",
        )
        LocalEventPushHelper.show(context, type)
    }

    private fun resolveKind(uri: Uri?): MediaKind {
        val s = uri?.toString().orEmpty()
        return when {
            s.contains("/images/", ignoreCase = true) -> MediaKind.IMAGE
            s.contains("/video/", ignoreCase = true) -> MediaKind.VIDEO
            s.contains("/audio/", ignoreCase = true) -> MediaKind.AUDIO
            s.contains("/file", ignoreCase = true) -> MediaKind.FILE
            else -> MediaKind.UNKNOWN
        }
    }

    private fun resolveLatest(context: Context, changedUri: Uri?, kind: MediaKind): MediaRow? {
        val collection = when (kind) {
            MediaKind.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaKind.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaKind.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            MediaKind.FILE, MediaKind.UNKNOWN -> MediaStore.Files.getContentUri("external")
        }
        if (changedUri != null && changedUri != collection) {
            queryRow(context, changedUri, kind)?.let { return it }
        }
        // UNKNOWN：分别取图/视/音/文件里最新一条，选时间最新且在 Download 的
        if (kind == MediaKind.UNKNOWN) {
            val candidates = listOf(
                queryLatest(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaKind.IMAGE),
                queryLatest(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaKind.VIDEO),
                queryLatest(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaKind.AUDIO),
                queryLatest(context, MediaStore.Files.getContentUri("external"), MediaKind.FILE),
            ).filterNotNull()
            return candidates.maxByOrNull { maxOf(it.dateAdded, it.dateModified) }
        }
        return queryLatest(context, collection, kind)
    }

    private fun queryLatest(context: Context, collection: Uri, kind: MediaKind): MediaRow? {
        val pathColumn = pathColumn()
        val sort = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        return runCatching {
            context.contentResolver.query(collection, projection(kind, pathColumn), null, null, sort)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    readRow(cursor, pathColumn, kind)
                }
        }.onFailure {
            LogUtils.e(TAG, it.message)
        }.getOrNull()
    }

    private fun queryRow(context: Context, uri: Uri, kind: MediaKind): MediaRow? {
        val pathColumn = pathColumn()
        return runCatching {
            context.contentResolver.query(uri, projection(kind, pathColumn), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    readRow(cursor, pathColumn, kind)
                }
        }.getOrNull()
    }

    private fun pathColumn(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            MediaStore.MediaColumns.DATA
        }
    }

    private fun projection(kind: MediaKind, pathColumn: String): Array<String> {
        val base = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            pathColumn,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE,
        )
        if (kind == MediaKind.FILE || kind == MediaKind.UNKNOWN) {
            base.add(MediaStore.Files.FileColumns.MEDIA_TYPE)
        }
        return base.toTypedArray()
    }

    private fun readRow(
        cursor: android.database.Cursor,
        pathColumn: String,
        kind: MediaKind,
    ): MediaRow {
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
        val pathIndex = cursor.getColumnIndex(pathColumn)
        val addedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
        val modifiedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
        val mimeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
        val mediaTypeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
        return MediaRow(
            id = cursor.getLong(idIndex),
            name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else "",
            path = if (pathIndex >= 0) cursor.getString(pathIndex).orEmpty() else "",
            dateAdded = if (addedIndex >= 0) cursor.getLong(addedIndex) else 0L,
            dateModified = if (modifiedIndex >= 0) cursor.getLong(modifiedIndex) else 0L,
            mime = if (mimeIndex >= 0) cursor.getString(mimeIndex).orEmpty() else "",
            mediaType = if (mediaTypeIndex >= 0) cursor.getInt(mediaTypeIndex) else -1,
            kind = kind,
        )
    }

    private fun isScreenshot(name: String, path: String): Boolean {
        val haystack = "$name $path".lowercase()
        return listOf(
            "screenshot",
            "screen_shot",
            "screenshots",
            "screencapture",
            "screen-shot",
            "截屏",
            "截图",
            "屏幕截图",
        ).any { haystack.contains(it) }
    }

    private enum class MediaKind {
        IMAGE, VIDEO, AUDIO, FILE, UNKNOWN
    }

    private data class MediaRow(
        val id: Long,
        val name: String,
        val path: String,
        val dateAdded: Long,
        val dateModified: Long,
        val mime: String,
        val mediaType: Int,
        val kind: MediaKind,
    )
}
