package com.aetherquorion.cleansuperai.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.tencent.mmkv.MMKV

object NotificationPermissionFlow {
    enum class TipAction { REQUEST, SETTINGS }

    fun shouldRequest(sdkInt: Int, granted: Boolean): Boolean =
        sdkInt >= Build.VERSION_CODES.TIRAMISU && !granted

    fun tipAction(denials: Int): TipAction =
        if (denials >= 2) TipAction.SETTINGS else TipAction.REQUEST

    fun isGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun deniedCount(): Int = MMKV.defaultMMKV().getInt("notification_permission_denied_count", 0)

    fun recordResult(granted: Boolean) {
        val store = MMKV.defaultMMKV()
        if (granted) store.removeValueForKey("notification_permission_denied_count")
        else store.putInt("notification_permission_denied_count", deniedCount() + 1)
    }
}
