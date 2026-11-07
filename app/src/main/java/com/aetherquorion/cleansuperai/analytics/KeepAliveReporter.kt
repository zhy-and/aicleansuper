package com.aetherquorion.cleansuperai.analytics

import android.content.Context
import android.os.Build
import com.aetherquorion.cleansuperai.notification.NotificationPermissionFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 10008 for keepalive and local notification display/click events. */
object KeepAliveReporter {
    fun report(context: Context, eventName: String, extra: Map<String, String> = emptyMap()) {
        val install = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        }.getOrDefault(0L)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fields = linkedMapOf(
            "trigger_route" to (extra["trigger_route"] ?: extra["route"].orEmpty()),
            "trigger_reason" to eventName,
            "outcome" to (extra["outcome"] ?: extra["result"].orEmpty()),
            "failure_reason" to extra["failure_reason"].orEmpty(),
            "started_routes" to extra["started_routes"].orEmpty(),
            "skipped_routes" to extra["skipped_routes"].orEmpty(),
            "notification_permission" to when {
                Build.VERSION.SDK_INT < 33 -> "系统无需授权"
                NotificationPermissionFlow.isGranted(context) -> "已授权"
                else -> "未授权"
            },
            "install_time" to if (install > 0) format.format(Date(install)) else "",
            "brand" to Build.BRAND.orEmpty(),
            "model" to Build.MODEL.orEmpty(),
        )
        CenterReporter.report(context, "10008", eventName, fields + extra)
    }
}
