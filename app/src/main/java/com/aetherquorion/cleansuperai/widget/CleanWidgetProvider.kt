package com.aetherquorion.cleansuperai.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import com.aetherquorion.cleansuperai.MainActivity
import com.aetherquorion.cleansuperai.R

class CleanWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_clean).apply {
                val free = StatFs(Environment.getExternalStorageDirectory().path).availableBytes
                val formatted = Formatter.formatShortFileSize(context, free).split(" ", limit = 2)
                setTextViewText(R.id.widgetSizeValue, formatted.firstOrNull().orEmpty())
                setTextViewText(R.id.widgetSizeUnit, formatted.getOrElse(1) { "" })
                setOnClickPendingIntent(R.id.widgetCleanAction, action(context, id, "cache"))
                setOnClickPendingIntent(R.id.widgetLargeAction, action(context, id + 10000, "large"))
                setOnClickPendingIntent(R.id.widgetScreenshotAction, action(context, id + 20000, "screenshots"))
            }
            manager.updateAppWidget(id, views)
        }
    }

    private fun action(context: Context, id: Int, route: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_WIDGET_ROUTE, route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
