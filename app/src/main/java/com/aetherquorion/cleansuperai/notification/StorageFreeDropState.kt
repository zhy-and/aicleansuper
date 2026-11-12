package com.aetherquorion.cleansuperai.notification

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.blankj.utilcode.util.LogUtils
import com.tencent.mmkv.MMKV

/**
 * 剩余空间相对上次基线增加或减少超过 30 MiB 时允许推送。
 *
 * 规则：
 * - 首次无基线：只写基线，不推
 * - 双向变化超过 30 MiB：可推；基线更新为当前（无论后续是否被频控拦住）
 * - 未达阈值：不推、不改基线（跨 tick 累计）
 */
object StorageFreeDropState {
    private const val TAG = "StorageFreeDrop"
    private const val KEY_BASELINE_FREE_BYTES = "local_push_storage_baseline_free_bytes"

    fun evaluateAndCommit(context: Context): Status {
        ensureMmkv(context)
        val stats = StatFs(Environment.getDataDirectory().path)
        val total = stats.totalBytes
        val used = total - stats.availableBytes
        if (total <= 0L) {
            LogUtils.w(TAG, "存储总量无效：used=$used total=$total，跳过")
            return Status(
                currentFreeBytes = 0L,
                baselineFreeBytes = null,
                changeBytes = 0L,
                shouldShow = false,
                reason = "total_invalid",
            )
        }
        val free = (total - used).coerceAtLeast(0L)
        val store = MMKV.defaultMMKV()
        if (!store.contains(KEY_BASELINE_FREE_BYTES)) {
            store.putLong(KEY_BASELINE_FREE_BYTES, free)
            LogUtils.i(TAG, "首次写入存储基线：freeBytes=$free，不推送")
            return Status(
                currentFreeBytes = free,
                baselineFreeBytes = null,
                changeBytes = 0L,
                shouldShow = false,
                reason = "seed_baseline",
            )
        }

        val baseline = store.getLong(KEY_BASELINE_FREE_BYTES, free)
        val change = free - baseline
        val shouldShow = LocalPushTriggerPolicy.storageChangedEnough(baseline, free)
        if (shouldShow) {
            store.putLong(KEY_BASELINE_FREE_BYTES, free)
            LogUtils.i(TAG, "存储变化达标：baseline=$baseline current=$free change=$change，刷新基线")
        } else {
            LogUtils.i(TAG, "存储变化未达标：baseline=$baseline current=$free change=$change，保留基线")
        }
        return Status(
            currentFreeBytes = free,
            baselineFreeBytes = baseline,
            changeBytes = change,
            shouldShow = shouldShow,
            reason = if (shouldShow) "change_reached" else "change_below_threshold",
        )
    }

    private fun ensureMmkv(context: Context) {
        runCatching { MMKV.defaultMMKV() }.onFailure {
            MMKV.initialize(context.applicationContext)
        }
    }

    data class Status(
        val currentFreeBytes: Long,
        val baselineFreeBytes: Long?,
        val changeBytes: Long,
        val shouldShow: Boolean,
        val reason: String,
    ) {
        fun describe(): String {
            return "currentBytes=$currentFreeBytes baselineBytes=${baselineFreeBytes ?: "none"} " +
                "changeBytes=$changeBytes shouldShow=$shouldShow reason=$reason"
        }
    }
}
