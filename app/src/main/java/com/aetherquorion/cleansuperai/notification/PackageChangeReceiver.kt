package com.aetherquorion.cleansuperai.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.SystemClock
import com.blankj.utilcode.util.LogUtils

/**
 * 应用安装 / 卸载 / 更新 本地推送。
 *
 * - PACKAGE_ADDED 且非 REPLACING → app_install
 * - PACKAGE_FULLY_REMOVED 且非 REPLACING → app_uninstall
 * - PACKAGE_REPLACED（其他 App 更新完成，排除本包）→ app_update
 *
 * 注意：很多 ROM 上 Manifest 静态注册对 PACKAGE_* 不可靠，Bootstrap 里会再动态注册一份。
 */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val packageUri = intent.data ?: return
        val changedPackage = packageUri.schemeSpecificPart ?: return
        if (changedPackage == context.packageName) {
            LogUtils.i("PackageChangeReceiver", "忽略本包变化：action=$action pkg=$changedPackage")
            return
        }

        val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        LogUtils.i(
            "PackageChangeReceiver",
            "收到包变化：action=$action pkg=$changedPackage replacing=$replacing",
        )

        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                // 更新流程里也会有 PACKAGE_ADDED(replacing=true)，不能当新安装
                if (replacing) {
                    LogUtils.i("PackageChangeReceiver", "PACKAGE_ADDED 带 EXTRA_REPLACING，跳过 install（留给 REPLACED）")
                    return
                }
                if (!allowOnce("install", changedPackage)) return
                val appLabel = PackageEventAppLabelStore.resolveInstalledLabel(context, changedPackage)
                LogUtils.i("PackageChangeReceiver", "触发 app_install：pkg=$changedPackage")
                LocalEventPushHelper.show(
                    context,
                    LocalEventPushHelper.EventType.APP_INSTALL,
                    appLabel = appLabel,
                )
            }
            Intent.ACTION_PACKAGE_FULLY_REMOVED -> {
                if (replacing) {
                    LogUtils.i("PackageChangeReceiver", "PACKAGE_FULLY_REMOVED 带 EXTRA_REPLACING，跳过 uninstall")
                    return
                }
                if (!allowOnce("uninstall", changedPackage)) return
                val appLabel = PackageEventAppLabelStore.labelForUninstall(context, changedPackage)
                LogUtils.i("PackageChangeReceiver", "触发 app_uninstall：pkg=$changedPackage")
                LocalEventPushHelper.show(
                    context,
                    LocalEventPushHelper.EventType.APP_UNINSTALL,
                    appLabel = appLabel,
                )
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                // 其他 App 更新完成
                if (!allowOnce("update", changedPackage)) return
                val appLabel = PackageEventAppLabelStore.resolveInstalledLabel(context, changedPackage)
                LogUtils.i("PackageChangeReceiver", "触发 app_update：pkg=$changedPackage")
                LocalEventPushHelper.show(
                    context,
                    LocalEventPushHelper.EventType.APP_UPDATE,
                    appLabel = appLabel,
                )
            }
            else -> {
                LogUtils.w("PackageChangeReceiver", "未处理的 action=$action")
            }
        }
    }

    companion object {
        /** 合并 Manifest + 动态双注册造成的同一事件重复投递 */
        private const val MERGE_WINDOW_MS = 2_000L

        @Volatile
        private var dynamicRegistered = false
        private val lastAcceptedAt = HashMap<String, Long>()

        @Synchronized
        private fun allowOnce(kind: String, packageName: String): Boolean {
            val key = "$kind:$packageName"
            val now = SystemClock.elapsedRealtime()
            val last = lastAcceptedAt[key] ?: 0L
            if (now - last < MERGE_WINDOW_MS) {
                LogUtils.w("PackageChangeReceiver", "合并重复投递：$key 距上次 ${now - last}ms < ${MERGE_WINDOW_MS}ms")
                return false
            }
            lastAcceptedAt[key] = now
            return true
        }

        fun registerDynamically(context: Context) {
            if (dynamicRegistered) {
                LogUtils.i("PackageChangeReceiver", "动态注册已完成，跳过")
                return
            }
            val appContext = context.applicationContext
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
                priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            }
            val receiver = PackageChangeReceiver()
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    appContext.registerReceiver(receiver, filter)
                }
                dynamicRegistered = true
                LogUtils.i(
                    "PackageChangeReceiver",
                    "动态注册成功：PACKAGE_ADDED / PACKAGE_FULLY_REMOVED / PACKAGE_REPLACED",
                )
            }.onFailure {
                LogUtils.e("PackageChangeReceiver", "动态注册失败：${it.message}")
            }
        }
    }
}
