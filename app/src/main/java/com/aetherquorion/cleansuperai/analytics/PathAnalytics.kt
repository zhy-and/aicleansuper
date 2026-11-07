package com.aetherquorion.cleansuperai.analytics

import android.content.Context
import android.os.Build
import android.os.Bundle
import com.aetherquorion.cleansuperai.ads.analysis.AnalysisDataUtil
import com.aetherquorion.cleansuperai.config.UserSegmentHelper
import com.blankj.utilcode.util.ActivityUtils
import com.google.firebase.analytics.FirebaseAnalytics
import com.tencent.mmkv.MMKV

object PathAnalytics {
    fun currentPage(): String = ActivityUtils.getTopActivity()?.javaClass?.simpleName.orEmpty()
    fun track(context: Context, event: PathEvent, extra: Map<String, String> = emptyMap()) {
        runCatching {
            val params = Bundle().apply {
                putString("event_name", event.label); putString("page", currentPage())
                putString("codp", AnalysisDataUtil.launchPhaseLabel())
                putString("user_segment", UserSegmentHelper.currentSegment().jsonKey)
                putString("sdk_int", Build.VERSION.SDK_INT.toString())
                extra.filterValues { it.isNotEmpty() }.forEach { (k, v) -> putString(k, v) }
            }
            FirebaseAnalytics.getInstance(context.applicationContext).logEvent(event.firebaseName, params)
        }
        CenterReporter.report(context, "10009", event.label, extra)
    }
    fun trackColdSplash(context: Context, event: PathEvent, extra: Map<String, String> = emptyMap()) {
        if (AnalysisDataUtil.interspaceStudyStatus) track(context, event, extra)
    }
    fun trackHomePageOnce(context: Context) {
        val store = MMKV.defaultMMKV()
        if (store.getBoolean("path_home_page_reported", false)) return
        store.putBoolean("path_home_page_reported", true)
        track(context, PathEvent.HOME_PAGE)
    }
    fun trackFeatureClick(context: Context, featureKey: String) {
        PathEvent.fromFeatureKey(featureKey)?.let { track(context, it) }
    }
}
