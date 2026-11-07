package com.aetherquorion.cleansuperai.ui.common

enum class FeatureRoute {
    CACHE, LARGE_FILES, PHOTOS, VIDEOS, AUDIO, NETWORK_TRAFFIC,
    APP_MANAGER, BATTERY_INFO, DEVICE_INFO, VAULT
}

enum class FeatureAccess { NONE, ALL_FILES, USAGE }

object FeatureAccessPolicy {
    fun forFeature(route: FeatureRoute): FeatureAccess = when (route) {
        FeatureRoute.CACHE, FeatureRoute.LARGE_FILES, FeatureRoute.PHOTOS,
        FeatureRoute.VIDEOS, FeatureRoute.AUDIO, FeatureRoute.VAULT -> FeatureAccess.ALL_FILES
        FeatureRoute.NETWORK_TRAFFIC -> FeatureAccess.USAGE
        FeatureRoute.APP_MANAGER, FeatureRoute.BATTERY_INFO,
        FeatureRoute.DEVICE_INFO -> FeatureAccess.NONE
    }
}

class PendingFeatureRoute {
    private var route: FeatureRoute? = null

    fun set(value: FeatureRoute) { route = value }
    fun consume(): FeatureRoute? = route.also { route = null }
    fun peek(): FeatureRoute? = route
}
