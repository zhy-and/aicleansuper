package com.aetherquorion.cleansuperai.notification

/** Produces dynamic package-event copy only when a human-readable app label is available. */
object PackageEventNotificationCopy {
    enum class Event {
        INSTALL,
        UNINSTALL,
        UPDATE,
    }

    data class Copy(
        val event: Event,
        val text: String,
        val appNameRange: IntRange,
    )

    fun resolve(
        event: Event,
        appLabel: String?,
        packageName: String = "",
        originalTitle: String,
    ): Copy? {
        val label = appLabel?.trim().orEmpty()
        if (label.isEmpty() || label.equals(packageName, ignoreCase = true)) return null
        return Copy(
            event = event,
            text = "$label $originalTitle",
            appNameRange = 0 until label.length,
        )
    }
}
