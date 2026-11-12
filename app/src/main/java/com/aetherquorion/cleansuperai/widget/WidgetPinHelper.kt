package com.aetherquorion.cleansuperai.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.analytics.KeepAliveReporter
import com.aetherquorion.cleansuperai.analytics.PathAnalytics
import com.aetherquorion.cleansuperai.analytics.PathEvent

object WidgetPinHelper {
    const val EXTRA_FROM_KEY = "from_key"
    fun hasPinnedWidget(context: Context): Boolean =
        AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, CleanWidgetProvider::class.java)).isNotEmpty()

    fun requestPin(context: Context, fromKey: String): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !manager.isRequestPinAppWidgetSupported) {
            Toast.makeText(context, R.string.widget_pin_unsupported, Toast.LENGTH_SHORT).show()
            return false
        }
        if (hasPinnedWidget(context)) {
            Toast.makeText(context, R.string.widget_already_added, Toast.LENGTH_SHORT).show()
            return false
        }
        val callback = PendingIntent.getBroadcast(context, fromKey.hashCode(),
            Intent(context, AppWidgetPinReceiver::class.java)
                .setAction(AppWidgetPinReceiver.ACTION_PIN_SUCCESS)
                .putExtra(EXTRA_FROM_KEY, fromKey),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return manager.requestPinAppWidget(
            ComponentName(context, CleanWidgetProvider::class.java), null, callback)
    }
}

class AppWidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_PIN_SUCCESS) return
        val from = intent.getStringExtra(WidgetPinHelper.EXTRA_FROM_KEY).orEmpty()
        PathAnalytics.track(context, PathEvent.WIDGET_ADD_SUCCESS,
            mapOf("from_key" to from, "from_name" to "首页提示"))
        KeepAliveReporter.report(context, "桌面小组件",
            mapOf("route" to "桌面小组件", "result" to "成功", "started_routes" to "桌面小组件"))
    }
    companion object {
        const val ACTION_PIN_SUCCESS = "com.aetherquorion.cleansuperai.action.WIDGET_PIN_SUCCESS"
    }
}
