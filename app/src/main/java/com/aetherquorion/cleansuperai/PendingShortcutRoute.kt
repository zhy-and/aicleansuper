package com.aetherquorion.cleansuperai

import android.content.Context
import android.content.Intent

/** Keeps a shortcut target until the hot-start screen and any launch ad have closed. */
object PendingShortcutRoute {
    private var route: String? = null

    @Synchronized fun remember(value: String?) {
        if (!value.isNullOrBlank()) route = value
    }

    @Synchronized fun consume(): String? = route.also { route = null }

    fun dispatch(context: Context): Boolean {
        val target = consume() ?: return false
        context.startActivity(Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_WIDGET_ROUTE, target)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        return true
    }
}
