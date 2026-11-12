package com.aetherquorion.cleansuperai.ui.home

object HomeStorageGate {
    fun requiresAllFiles(tag: String): Boolean = tag !in setOf(
        "battery_info", "device_info", "contacts_cleanup", "network_traffic"
    )
}
