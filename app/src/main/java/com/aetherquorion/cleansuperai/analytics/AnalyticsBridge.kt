package com.aetherquorion.cleansuperai.analytics

import android.content.Context
import android.os.Build
import android.os.Bundle
import com.aetherquorion.cleansuperai.BuildConfig
import com.aetherquorion.cleansuperai.config.SegmentedRemoteConfig
import com.aetherquorion.cleansuperai.config.UserSegmentHelper
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.tencent.mmkv.MMKV
import java.util.concurrent.ThreadLocalRandom

object AnalyticsBridge {
    const val EVENT_KA_OUTCOME = "ka_outcome"
    const val EVENT_KA_SERVICE = "ka_service"
    const val EVENT_KA_ALIVE_GAP = "ka_alive_gap"
    const val EVENT_LP_FUNNEL = "lp_funnel"
    private const val BUCKET = "ka_firebase_sample_bucket"
    private const val LAST_SUCCESS = "ka_firebase_last_success_at"

    fun ensureInitialized(context: Context) {
        MMKV.initialize(context.applicationContext)
        val store = MMKV.defaultMMKV()
        if (!store.containsKey(BUCKET)) store.putInt(BUCKET, ThreadLocalRandom.current().nextInt(100))
        SegmentedRemoteConfig.ensureInitialized(context)
        refreshUserProperties(context)
    }
    fun refreshUserProperties(context: Context) {
        val fa = FirebaseAnalytics.getInstance(context.applicationContext)
        val store = MMKV.defaultMMKV()
        fa.setUserProperty("oem_brand", Build.BRAND.take(36))
        fa.setUserProperty("ka_sample_bucket", store.getInt(BUCKET, 0).toString())
        fa.setUserProperty("user_segment", UserSegmentHelper.currentSegment().jsonKey)
        fa.setUserProperty("notif_permission", if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
            "denied" else "granted")
    }
    fun logKaOutcome(context: Context, route: String, reason: String, result: String,
        detail: Map<String, String> = emptyMap()) {
        ensureInitialized(context)
        val success = result == "成功" || result.equals("success", true)
        val sampled = success && reason in setOf("quick_job", "heartbeat_job", "alarm", "app_foreground", "home_key", "manual", "remote_config")
        val rate = FirebaseRemoteConfig.getInstance().getLong(SegmentedRemoteConfig.KEY_SAMPLE_RATE).let { if (it <= 0) 10 else it.coerceIn(1, 100) }
        if (sampled && !BuildConfig.DEBUG && MMKV.defaultMMKV().getInt(BUCKET, 0) >= rate) return
        log(context, EVENT_KA_OUTCOME, listOf(route, reason, result), mapOf(
            "trigger_route" to route, "trigger_reason" to reason, "outcome" to result,
            "fail_reason" to detail["failure_reason"].orEmpty(),
            "started_routes" to detail["started_routes"].orEmpty(),
            "skipped_routes" to detail["skipped_routes"].orEmpty(),
            "sampled" to if (sampled && !BuildConfig.DEBUG) "是" else "否"))
        if (success) {
            val now = System.currentTimeMillis()
            val last = MMKV.defaultMMKV().getLong(LAST_SUCCESS, 0)
            MMKV.defaultMMKV().putLong(LAST_SUCCESS, now)
            if (last > 0) log(context, EVENT_KA_ALIVE_GAP, listOf(route, reason),
                mapOf("gap_sec" to ((now - last) / 1000).toString(), "trigger_route" to route, "trigger_reason" to reason))
        }
    }
    fun logKaService(context: Context, reason: String, stage: String, result: String) {
        log(context, EVENT_KA_SERVICE, listOf(reason, stage, result),
            mapOf("trigger_reason" to reason, "stage" to stage, "outcome" to result, "sampled" to "否"))
    }
    fun logLpFunnel(context: Context, step: String, scene: String, sceneZh: String,
        bucket: String = "", skipReason: String = "", notifyId: String = "", router: String = "",
        fromHot: String = "", slotId: String = "", triggerSource: String = "",
        scheduledAtMs: Long = 0, actualAtMs: Long = 0) {
        log(context, EVENT_LP_FUNNEL, listOf(step, scene, notifyId, skipReason, slotId), mapOf(
            "step" to step, "scene" to scene, "scene_zh" to sceneZh, "bucket" to bucket,
            "skip_reason" to skipReason, "notify_id" to notifyId, "router" to router,
            "from_hot" to fromHot, "slot_id" to slotId, "trigger_source" to triggerSource,
            "scheduled_at_ms" to scheduledAtMs.toString(), "actual_at_ms" to actualAtMs.toString(), "sampled" to "否"))
    }
    private fun log(context: Context, name: String, parts: List<String>, values: Map<String, String>) {
        runCatching {
            ensureInitialized(context)
            val store = MMKV.defaultMMKV()
            val key = "ka_firebase_dedupe_${name}_${parts.joinToString("|").hashCode()}"
            val now = System.currentTimeMillis()
            val sec = FirebaseRemoteConfig.getInstance().getLong(SegmentedRemoteConfig.KEY_DEDUPE_SEC).let { if (it <= 0) 60 else it }
            if (now - store.getLong(key, 0) < sec * 1000) return
            store.putLong(key, now)
            val bundle = Bundle().apply {
                putString("sdk_int", Build.VERSION.SDK_INT.toString())
                putString("user_segment", UserSegmentHelper.currentSegment().jsonKey)
                values.forEach { (k, v) -> if (v.isNotEmpty()) putString(k, v.take(100)) }
            }
            FirebaseAnalytics.getInstance(context.applicationContext).logEvent(name, bundle)
        }
    }
}
