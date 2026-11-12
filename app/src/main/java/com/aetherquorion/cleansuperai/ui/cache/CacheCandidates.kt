package com.aetherquorion.cleansuperai.ui.cache

enum class CacheKind { APP_CACHE, OBSOLETE_APK, RESIDUAL }

fun classifyCacheCandidate(name: String): CacheKind? {
    val lower = name.lowercase()
    return when {
        lower.endsWith(".apk") -> CacheKind.OBSOLETE_APK
        lower.endsWith(".tmp") || lower.endsWith(".temp") || lower.endsWith(".log") ||
            lower.contains(".bak") || lower.startsWith("thumb") -> CacheKind.RESIDUAL
        else -> null
    }
}
