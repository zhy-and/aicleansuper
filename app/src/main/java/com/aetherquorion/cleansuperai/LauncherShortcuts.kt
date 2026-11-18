package com.aetherquorion.cleansuperai

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import java.util.concurrent.Executors

internal object LauncherShortcuts {
    private val publisher = Executors.newSingleThreadExecutor()

    fun refresh(context: Context) {
        if (Build.VERSION.SDK_INT < 25) return
        val app = context.applicationContext
        val launcherActivity = ComponentName(app, SplashActivity::class.java)

        fun shortcut(id: String, labelId: Int, iconId: Int, intent: Intent, rank: Int): ShortcutInfo {
            val label = context.getString(labelId)
            // A previously pinned manifest shortcut can reserve its old ID as immutable.
            return ShortcutInfo.Builder(app, "launcher_$id")
                .setActivity(launcherActivity)
                .setShortLabel(label)
                .setLongLabel(label)
                .setIcon(Icon.createWithResource(app, iconId))
                .setIntent(intent)
                .setRank(rank)
                .build()
        }

        fun route(destination: String) = Intent(app, HotActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(MainActivity.EXTRA_WIDGET_ROUTE, destination)

        val requested = listOf(
            shortcut("large_files", R.string.notif_large_file, R.drawable.ic_placeholder_folder, route("large"), 0),
            shortcut("cache", R.string.notif_cache, R.drawable.ic_placeholder_broom, route("cache"), 1),
            shortcut("apps", R.string.notif_apps, R.drawable.ic_notif_apps, route("apps"), 2),
            shortcut(
                "uninstall", R.string.shortcut_uninstall, R.drawable.ic_shortcut_uninstall,
                Intent(app, UninstallGuideActivity::class.java).setAction(UninstallGuideActivity.ACTION_SHORTCUT_UNINSTALL),
                3,
            ),
        )
        publisher.execute {
            val manager = app.getSystemService(ShortcutManager::class.java) ?: return@execute
            try {
                val shortcuts = requested.take(manager.maxShortcutCountPerActivity)
                val published = manager.dynamicShortcuts.associateBy { it.id }
                val labelsMatch = published.size == shortcuts.size && shortcuts.all {
                    val current = published[it.id]
                    current != null && current.shortLabel == it.shortLabel && current.longLabel == it.longLabel
                }
                if (!labelsMatch && !manager.setDynamicShortcuts(shortcuts)) {
                    Log.d("LauncherShortcuts", "Shortcut refresh deferred by Android")
                }
            } catch (e: IllegalStateException) {
                Log.d("LauncherShortcuts", "Shortcut refresh unavailable", e)
            }
        }
    }
}
