package com.aetherquorion.cleansuperai.notification

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.blankj.utilcode.util.LogUtils

/**
 * 系统 MediaStore 删除 / 清空回收站监听。
 *
 * - image/video/audio/file：快照 ID 集合，减少项视为单条删除
 * - trash_emptied：API 30+ 监听 IS_TRASHED=1 图片集合减少至少一张
 * - 本 App 删除窗口内 [LocalPushSelfChangeGuard] 压制
 */
object MediaStoreDeleteWatcher {
    private const val TAG = "MediaStoreDeleteWatcher"
    private const val DEBOUNCE_MS = 900L

    @Volatile
    private var registered = false

    private val handler = Handler(Looper.getMainLooper())
    private var pendingCheck: Runnable? = null

    private var imageIds: Set<Long> = emptySet()
    private var videoIds: Set<Long> = emptySet()
    private var audioIds: Set<Long> = emptySet()
    private var fileIds: Set<Long> = emptySet()
    private var trashedImageIds: Set<Long> = emptySet()

    fun start(context: Context) {
        if (registered) {
            LogUtils.i(TAG, "MediaStore 删除监听已注册，跳过")
            return
        }
        val appContext = context.applicationContext
        seed(appContext)
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                schedule(appContext)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                schedule(appContext)
            }
        }
        val cr = appContext.contentResolver
        runCatching {
            cr.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
            cr.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
            cr.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
            cr.registerContentObserver(MediaStore.Files.getContentUri("external"), true, observer)
            registered = true
            LogUtils.i(TAG, "MediaStore 删除/回收站监听注册成功")
        }.onFailure {
            LogUtils.e(TAG, "注册失败：${it.message}")
        }
    }

    private fun schedule(context: Context) {
        pendingCheck?.let { handler.removeCallbacks(it) }
        val task = Runnable {
            pendingCheck = null
            Thread {
                evaluate(context)
            }.start()
        }
        pendingCheck = task
        handler.postDelayed(task, DEBOUNCE_MS)
    }

    private fun seed(context: Context) {
        imageIds = queryIds(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        videoIds = queryIds(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        audioIds = queryIds(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        fileIds = queryFileIds(context)
        trashedImageIds = queryTrashedImageIds(context)
        LogUtils.i(
            TAG,
            "种子快照：images=${imageIds.size} videos=${videoIds.size} audios=${audioIds.size} " +
                "files=${fileIds.size} trashedImages=${trashedImageIds.size}",
        )
    }

    private fun evaluate(context: Context) {
        if (LocalPushSelfChangeGuard.isSuppressed()) {
            LogUtils.i(TAG, "本 App 自删除压制窗口内，只刷新快照不推送")
            seed(context)
            return
        }

        val nextImages = queryIds(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val nextVideos = queryIds(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        val nextAudios = queryIds(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        val nextFiles = queryFileIds(context)
        val nextTrashed = queryTrashedImageIds(context)

        val removedTrashed = trashedImageIds - nextTrashed
        val trashDrop = trashedImageIds.size - nextTrashed.size
        val trashEmptied = LocalPushTriggerPolicy.trashedImagesDecreased(
            trashedImageIds.size, nextTrashed.size,
        )

        if (trashEmptied) {
            LogUtils.i(
                TAG,
                "检测到清空回收站：prev=${trashedImageIds.size} next=${nextTrashed.size} drop=$trashDrop",
            )
            LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.TRASH_EMPTIED)
        }

        val removedImages = imageIds - nextImages
        val removedVideos = videoIds - nextVideos
        val removedAudios = audioIds - nextAudios
        val removedFiles = fileIds - nextFiles

        // 回收站清空时，对应永久删除不重复打 image_deleted
        val pureImageDeletes = if (trashEmptied) removedImages - removedTrashed else removedImages

        when {
            pureImageDeletes.size == 1 -> {
                LogUtils.i(TAG, "检测到单图删除：id=${pureImageDeletes.first()}")
                LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.IMAGE_DELETED)
            }
            pureImageDeletes.size > 1 && !trashEmptied -> {
                LogUtils.i(TAG, "批量图片删除 ${pureImageDeletes.size}，按单场景限频只尝试一次")
                LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.IMAGE_DELETED)
            }
        }

        when {
            removedVideos.size == 1 -> {
                LogUtils.i(TAG, "检测到单视频删除：id=${removedVideos.first()}")
                LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.VIDEO_DELETED)
            }
            removedVideos.size > 1 -> {
                LogUtils.i(TAG, "批量视频删除 ${removedVideos.size}，尝试一次 video_deleted")
                LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.VIDEO_DELETED)
            }
        }

        when {
            removedAudios.size == 1 -> {
                LogUtils.i(TAG, "检测到单音频删除：id=${removedAudios.first()}")
                LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.AUDIO_DELETED)
            }
            removedAudios.size > 1 -> {
                LogUtils.i(TAG, "批量音频删除 ${removedAudios.size}，尝试一次 audio_deleted")
                LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.AUDIO_DELETED)
            }
        }

        when {
            removedFiles.size == 1 -> {
                LogUtils.i(TAG, "检测到单文件删除：id=${removedFiles.first()}")
                LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.FILE_DELETED)
            }
            removedFiles.size > 1 -> {
                LogUtils.i(TAG, "批量文件删除 ${removedFiles.size}，尝试一次 file_deleted")
                LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.FILE_DELETED)
            }
        }

        imageIds = nextImages
        videoIds = nextVideos
        audioIds = nextAudios
        fileIds = nextFiles
        trashedImageIds = nextTrashed
    }

    private fun queryIds(context: Context, uri: Uri): Set<Long> {
        return runCatching {
            val ids = LinkedHashSet<Long>()
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns._ID),
                null,
                null,
                null,
            )?.use { cursor ->
                val idx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                while (cursor.moveToNext()) {
                    ids += cursor.getLong(idx)
                }
            }
            ids
        }.onFailure {
            LogUtils.w(TAG, "queryIds 失败 uri=$uri：${it.message}")
        }.getOrDefault(emptySet())
    }

    private fun queryFileIds(context: Context): Set<Long> {
        return runCatching {
            val ids = LinkedHashSet<Long>()
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                arrayOf(MediaStore.Files.FileColumns._ID),
                "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?",
                arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString()),
                null,
            )?.use { cursor ->
                val idx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                while (cursor.moveToNext()) {
                    ids += cursor.getLong(idx)
                }
            }
            ids
        }.onFailure {
            LogUtils.w(TAG, "queryFileIds 失败：${it.message}")
        }.getOrDefault(emptySet())
    }

    private fun queryTrashedImageIds(context: Context): Set<Long> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptySet()
        return runCatching {
            val ids = LinkedHashSet<Long>()
            val projection = arrayOf(MediaStore.Images.Media._ID)
            // MediaStore 默认过滤 IS_TRASHED 项；必须显式 MATCH_ONLY/INCLUDE，否则快照恒为空、清空永不触发
            val args = Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                putString(
                    android.content.ContentResolver.QUERY_ARG_SQL_SELECTION,
                    "${MediaStore.MediaColumns.IS_TRASHED}=1",
                )
            }
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                args,
                null,
            )?.use { cursor ->
                val idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    ids += cursor.getLong(idx)
                }
            }
            ids
        }.onFailure {
            LogUtils.w(TAG, "queryTrashedImageIds 失败：${it.message}")
        }.getOrDefault(emptySet())
    }
}
