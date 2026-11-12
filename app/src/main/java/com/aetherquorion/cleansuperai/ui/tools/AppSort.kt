package com.aetherquorion.cleansuperai.ui.tools

enum class AppSort { NAME, INSTALLED, SIZE, LAST_USED }

fun sortApps(apps: List<InstalledAppItem>, sort: AppSort): List<InstalledAppItem> = when (sort) {
    AppSort.NAME -> apps.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    AppSort.INSTALLED -> apps.sortedByDescending { it.installedAt }
    AppSort.SIZE -> apps.sortedByDescending { it.sizeBytes }
    AppSort.LAST_USED -> apps.sortedByDescending { it.lastUsedAt }
}
