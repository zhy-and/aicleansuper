package com.aetherquorion.cleansuperai.ui.tools

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSortTest {
    private val alpha = InstalledAppItem("Alpha", "a", null, 30, 100, 500)
    private val beta = InstalledAppItem("Beta", "b", null, 10, 200, 400)

    @Test fun sortsByNameAscending() {
        assertEquals(listOf("a", "b"), sortApps(listOf(beta, alpha), AppSort.NAME).map { it.packageName })
    }

    @Test fun sortsBySizeDescending() {
        assertEquals(listOf("a", "b"), sortApps(listOf(beta, alpha), AppSort.SIZE).map { it.packageName })
    }

    @Test fun sortsByInstallAndLastUseDescending() {
        assertEquals(listOf("b", "a"), sortApps(listOf(alpha, beta), AppSort.INSTALLED).map { it.packageName })
        assertEquals(listOf("a", "b"), sortApps(listOf(beta, alpha), AppSort.LAST_USED).map { it.packageName })
    }
}
