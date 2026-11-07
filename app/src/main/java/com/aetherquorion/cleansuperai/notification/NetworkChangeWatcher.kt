package com.aetherquorion.cleansuperai.notification

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import com.blankj.utilcode.util.LogUtils

/**
 * 网络变化本地推送监听。
 *
 * 约定：任意网络状态变化都可能触发 network_change（Wi-Fi / 蜂窝 / 断网 / 重连等）。
 * - 本地约 1.5s 去抖，把连串回调合并成一次尝试
 * - 真正限频交给 LocalPushConfig 的 scene/global/bucket 冷却与日限
 *
 * 调试：过滤 log tag = NetworkChangeWatcher / LocalEventPush / LocalPushConfig
 */
object NetworkChangeWatcher {
    private const val TAG = "NetworkChangeWatcher"
    private const val DEBOUNCE_MS = 1_500L

    @Volatile
    private var registered = false

    private val handler = Handler(Looper.getMainLooper())
    private var pendingNotify: Runnable? = null

    /** 用于判断“是否真的变了”，避免同状态重复回调刷屏 */
    @Volatile
    private var lastFingerprint: String? = null

    private var appContextRef: Context? = null

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            LogUtils.i(TAG, "NetworkCallback.onAvailable：network=$network")
            scheduleEvaluate("onAvailable")
        }

        override fun onLost(network: Network) {
            LogUtils.i(TAG, "NetworkCallback.onLost：network=$network")
            scheduleEvaluate("onLost")
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            LogUtils.i(
                TAG,
                "NetworkCallback.onCapabilitiesChanged：network=$network " +
                    "wifi=${networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)} " +
                    "cellular=${networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)} " +
                    "ethernet=${networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)} " +
                    "validated=${networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}",
            )
            scheduleEvaluate("onCapabilitiesChanged")
        }
    }

    fun start(context: Context) {
        if (registered) {
            LogUtils.i(TAG, "网络监听已注册，跳过重复 start")
            return
        }
        val appContext = context.applicationContext
        appContextRef = appContext
        val cm = appContext.getSystemService(ConnectivityManager::class.java)
        if (cm == null) {
            LogUtils.e(TAG, "无法获取 ConnectivityManager，网络场景不可用")
            return
        }
        // 种子：记录当前指纹，避免刚注册就因首次回调误推
        lastFingerprint = currentFingerprint(cm)
        LogUtils.i(TAG, "网络监听种子指纹：$lastFingerprint")

        val request = NetworkRequest.Builder().build()
        runCatching {
            cm.registerNetworkCallback(request, callback)
            registered = true
            LogUtils.i(TAG, "网络监听注册成功：registerNetworkCallback debounceMs=$DEBOUNCE_MS")
        }.onFailure {
            LogUtils.e(TAG, "网络监听注册失败：${it.javaClass.simpleName} ${it.message.orEmpty()}")
        }
    }

    private fun scheduleEvaluate(reason: String) {
        val appContext = appContextRef ?: return
        pendingNotify?.let { handler.removeCallbacks(it) }
        val task = Runnable {
            pendingNotify = null
            evaluateAndMaybeShow(appContext, reason)
        }
        pendingNotify = task
        LogUtils.i(TAG, "网络变化已进入去抖队列：reason=$reason delayMs=$DEBOUNCE_MS")
        handler.postDelayed(task, DEBOUNCE_MS)
    }

    private fun evaluateAndMaybeShow(context: Context, reason: String) {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        val fingerprint = currentFingerprint(cm)
        val previous = lastFingerprint
        if (fingerprint == previous) {
            LogUtils.i(
                TAG,
                "去抖后指纹未变化，跳过 network_change：reason=$reason fingerprint=$fingerprint",
            )
            return
        }
        LogUtils.i(
            TAG,
            "确认网络状态变化：reason=$reason previous=$previous current=$fingerprint，尝试 network_change",
        )
        lastFingerprint = fingerprint
        val shown = LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.NETWORK_CHANGE)
        LogUtils.i(TAG, "network_change 场景尝试结果：shown=$shown fingerprint=$fingerprint")
    }

    /**
     * 用「是否有网 + 主要传输类型」做粗指纹。
     * 调试时从日志就能看出 Wi-Fi/蜂窝/无网切换。
     */
    private fun currentFingerprint(cm: ConnectivityManager): String {
        val active = cm.activeNetwork
        if (active == null) {
            return "none"
        }
        val caps = cm.getNetworkCapabilities(active) ?: return "unknown"
        val transports = buildList {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("wifi")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("cellular")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("ethernet")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) add("bt")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("vpn")
        }
        val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val internet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        return "net=${transports.joinToString("+").ifEmpty { "other" }};internet=$internet;validated=$validated"
    }
}
