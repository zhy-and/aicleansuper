package com.aetherquorion.cleansuperai.notification

import android.content.Context
import com.blankj.utilcode.util.LogUtils
import com.tencent.mmkv.MMKV

/**
 * 扫描相关延迟推送的待触发标记：
 * - [scan_done_uncleaned]：首页 cache 扫描完成进入结果页且尚未点清理
 * - [scan_interrupted]：任意清理扫描进度页在完成前离开
 *
 * 真正展示仍由退后台 + scene.delay_sec 调度，频控走 LocalPushConfig。
 */
object ScanPendingPushState {
    private const val TAG = "ScanPendingPushState"
    private const val KEY_SCAN_DONE_UNCLEANED = "lp_pending_scan_done_uncleaned"
    private const val KEY_SCAN_INTERRUPTED = "lp_pending_scan_interrupted"

    fun markScanDoneUncleaned(context: Context) {
        ensureMmkv(context)
        MMKV.defaultMMKV().putBoolean(KEY_SCAN_DONE_UNCLEANED, true)
        LogUtils.i(TAG, "标记待推送：scan_done_uncleaned")
    }

    fun clearScanDoneUncleaned(context: Context, reason: String) {
        ensureMmkv(context)
        if (!MMKV.defaultMMKV().getBoolean(KEY_SCAN_DONE_UNCLEANED, false)) return
        MMKV.defaultMMKV().putBoolean(KEY_SCAN_DONE_UNCLEANED, false)
        LogUtils.i(TAG, "清除待推送：scan_done_uncleaned reason=$reason")
    }

    fun hasScanDoneUncleaned(context: Context): Boolean {
        ensureMmkv(context)
        return MMKV.defaultMMKV().getBoolean(KEY_SCAN_DONE_UNCLEANED, false)
    }

    fun markScanInterrupted(context: Context) {
        ensureMmkv(context)
        MMKV.defaultMMKV().putBoolean(KEY_SCAN_INTERRUPTED, true)
        LogUtils.i(TAG, "标记待推送：scan_interrupted")
    }

    fun clearScanInterrupted(context: Context, reason: String) {
        ensureMmkv(context)
        if (!MMKV.defaultMMKV().getBoolean(KEY_SCAN_INTERRUPTED, false)) return
        MMKV.defaultMMKV().putBoolean(KEY_SCAN_INTERRUPTED, false)
        LogUtils.i(TAG, "清除待推送：scan_interrupted reason=$reason")
    }

    fun hasScanInterrupted(context: Context): Boolean {
        ensureMmkv(context)
        return MMKV.defaultMMKV().getBoolean(KEY_SCAN_INTERRUPTED, false)
    }

    private fun ensureMmkv(context: Context) {
        runCatching { MMKV.defaultMMKV() }.onFailure {
            MMKV.initialize(context.applicationContext)
        }
    }
}
