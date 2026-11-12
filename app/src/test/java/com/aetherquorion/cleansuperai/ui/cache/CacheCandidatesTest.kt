package com.aetherquorion.cleansuperai.ui.cache

import org.junit.Assert.assertEquals
import org.junit.Test

class CacheCandidatesTest {
    @Test fun classifiesApkAndResidualNames() {
        assertEquals(CacheKind.OBSOLETE_APK, classifyCacheCandidate("installer.apk"))
        assertEquals(CacheKind.RESIDUAL, classifyCacheCandidate("download.tmp"))
        assertEquals(CacheKind.RESIDUAL, classifyCacheCandidate("debug.log"))
        assertEquals(null, classifyCacheCandidate("photo.jpg"))
    }
}
