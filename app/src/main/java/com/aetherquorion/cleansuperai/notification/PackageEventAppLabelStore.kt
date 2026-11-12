package com.aetherquorion.cleansuperai.notification

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.tencent.mmkv.MMKV

/** Stores labels observed while an app is still installed so uninstall events can use them later. */
object PackageEventAppLabelStore {
    private const val KEY_PREFIX = "package_event_label:"

    @Volatile
    private var launcherLabelsCached = false

    fun resolveInstalledLabel(context: Context, packageName: String): String? {
        val packageManager = context.packageManager
        val label = runCatching {
            packageManager.getApplicationInfo(packageName, 0)
                .loadLabel(packageManager)
                .toString()
        }.getOrNull()
        return label.takeUsableLabel(packageName)?.also { remember(context, packageName, it) }
    }

    fun labelForUninstall(context: Context, packageName: String): String? {
        return store(context).getString(KEY_PREFIX + packageName, null).takeUsableLabel(packageName)
    }

    /** Seeds ordinary launcher-app labels once per process so later uninstall broadcasts can name them. */
    fun cacheVisibleLauncherLabels(context: Context) {
        if (launcherLabelsCached) return
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        runCatching {
            packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL).forEach { resolved ->
                val appInfo = resolved.activityInfo.applicationInfo ?: return@forEach
                val packageName = appInfo.packageName
                appInfo.loadLabel(packageManager).toString()
                    .takeUsableLabel(packageName)
                    ?.let { remember(context, packageName, it) }
            }
            launcherLabelsCached = true
        }
    }

    private fun remember(context: Context, packageName: String, label: String) {
        store(context).putString(KEY_PREFIX + packageName, label)
    }

    private fun store(context: Context): MMKV {
        return runCatching { MMKV.defaultMMKV() }.getOrElse {
            MMKV.initialize(context.applicationContext)
            MMKV.defaultMMKV()
        }
    }

    private fun String?.takeUsableLabel(packageName: String): String? {
        return this?.trim()?.takeIf { it.isNotEmpty() && !it.equals(packageName, ignoreCase = true) }
    }
}
