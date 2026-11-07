package com.aetherquorion.cleansuperai.notification

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.blankj.utilcode.util.LogUtils

object ScreenshotCaptureWatcher {
    private const val DEBOUNCE_MS = 800L
    private const val RECENT_WINDOW_SEC = 90L

    @Volatile
    private var registered = false
    private var pendingUri: Uri? = null
    private var pendingCheck: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    fun start(context: Context) {
        if (registered) return
        val appContext = context.applicationContext
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                scheduleCheck(appContext, null)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                scheduleCheck(appContext, uri)
            }
        }
        runCatching {
            appContext.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer,
            )
            registered = true
            LogUtils.i("ScreenshotCaptureWatcher", "registered")
        }.onFailure {
            LogUtils.e("ScreenshotCaptureWatcher", it.message)
        }
    }

    private fun scheduleCheck(context: Context, uri: Uri?) {
        if (uri != null) pendingUri = uri
        pendingCheck?.let { handler.removeCallbacks(it) }
        val task = Runnable {
            val target = pendingUri
            pendingUri = null
            checkScreenshot(context, target)
        }
        pendingCheck = task
        handler.postDelayed(task, DEBOUNCE_MS)
    }

    private fun checkScreenshot(context: Context, changedUri: Uri?) {
        val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.RELATIVE_PATH
        } else {
            MediaStore.Images.Media.DATA
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            pathColumn,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
        )

        val resolved = resolveLatestImage(context, changedUri, projection, pathColumn)
        if (resolved == null) {
            LogUtils.w("ScreenshotCaptureWatcher", "no image row (permission or empty)")
            return
        }
        val (id, name, path, dateAdded, dateModified) = resolved
        val nowSec = System.currentTimeMillis() / 1000
        val newest = maxOf(dateAdded, dateModified)
        if (nowSec - newest > RECENT_WINDOW_SEC) {
            LogUtils.w(
                "ScreenshotCaptureWatcher",
                "stale image id=$id age=${nowSec - newest}s name=$name path=$path",
            )
            return
        }
        if (!isScreenshot(name, path)) {
            LogUtils.w("ScreenshotCaptureWatcher", "not screenshot name=$name path=$path")
            return
        }
        LocalEventPushHelper.show(
            context,
            LocalEventPushHelper.EventType.SCREENSHOT,
        )
    }

    private fun resolveLatestImage(
        context: Context,
        changedUri: Uri?,
        projection: Array<String>,
        pathColumn: String,
    ): ImageRow? {
        if (changedUri != null && changedUri != MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
            queryImage(context, changedUri, projection, pathColumn)?.let { return it }
        }
        val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        return runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sort,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                readRow(cursor, pathColumn)
            }
        }.onFailure {
            LogUtils.e("ScreenshotCaptureWatcher", it.message)
        }.getOrNull()
    }

    private fun queryImage(
        context: Context,
        uri: Uri,
        projection: Array<String>,
        pathColumn: String,
    ): ImageRow? {
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                readRow(cursor, pathColumn)
            }
        }.getOrNull()
    }

    private fun readRow(cursor: android.database.Cursor, pathColumn: String): ImageRow {
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
        val pathIndex = cursor.getColumnIndex(pathColumn)
        val addedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
        val modifiedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
        return ImageRow(
            id = cursor.getLong(idIndex),
            name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else "",
            path = if (pathIndex >= 0) cursor.getString(pathIndex).orEmpty() else "",
            dateAdded = if (addedIndex >= 0) cursor.getLong(addedIndex) else 0L,
            dateModified = if (modifiedIndex >= 0) cursor.getLong(modifiedIndex) else 0L,
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

    private data class ImageRow(
        val id: Long,
        val name: String,
        val path: String,
        val dateAdded: Long,
        val dateModified: Long,
    )
}
