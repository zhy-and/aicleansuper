package com.aetherquorion.cleansuperai.config

import android.content.Context
import com.aetherquorion.cleansuperai.BuildConfig
import com.aetherquorion.cleansuperai.notification.LocalPushConfig
import com.aetherquorion.cleansuperai.notification.LocalPushSnapshotPolicy
import com.tencent.mmkv.MMKV
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.JsonParser
import java.util.concurrent.atomic.AtomicBoolean

/** Shared FRC bootstrap for Purgu's two independently consumed dual-segment documents. */
object SegmentedRemoteConfig {
    const val KEY_KEEPALIVE_CONFIG = "kp_config_v1"
    const val KEY_LOCAL_PUSH_CONFIG = "local_push_config_v1"
    const val KEY_SAMPLE_RATE = "ka_firebase_sample_rate"
    const val KEY_DEDUPE_SEC = "ka_firebase_dedupe_sec"
    enum class RefreshReason { COLD_START, HOT_START }
    data class Snapshot(val segment: UserSegmentHelper.Segment, val keepAliveJson: String, val localPushJson: String)

    private val initialized = AtomicBoolean(false)
    private val coldStarted = AtomicBoolean(false)
    private val hotInFlight = AtomicBoolean(false)
    @Volatile private var snapshot: Snapshot? = null
    /** Supplied by the keepalive and local push owners when their parsers are wired. */
    @Volatile var onSnapshotChanged: ((Snapshot) -> Unit)? = null

    private val organicKeepAlive = """{"kpa_boot_switch":"1","kpa_service_switch":"1","kpa_resume_switch":"1","kpa_alarm_switch":"0","kpa_job_switch":"0","kpa_fcm_switch":"0","kpa_home_switch":"0","kpa_delete_intent_switch":"0"}"""
    private val paidKeepAlive = """{"kpa_boot_switch":"1","kpa_service_switch":"1","kpa_resume_switch":"1","kpa_alarm_switch":"1","kpa_job_switch":"1","kpa_fcm_switch":"1","kpa_home_switch":"1","kpa_delete_intent_switch":"1"}"""
    fun defaultKeepAliveJson(): String = wrapper(organicKeepAlive, paidKeepAlive)
    fun defaultLocalPushJson(context: Context): String = LocalPushConfig.defaultConfigJson(context)
    private fun wrapper(organic: String, paid: String): String =
        """{"version":1,"organic":$organic,"paid":$paid}"""

    fun ensureInitialized(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600).build())
            setDefaultsAsync(mapOf(KEY_KEEPALIVE_CONFIG to defaultKeepAliveJson(),
                KEY_LOCAL_PUSH_CONFIG to defaultLocalPushJson(context), KEY_SAMPLE_RATE to 10L,
                KEY_DEDUPE_SEC to 60L))
        }
    }

    fun currentSnapshot(): Snapshot? = snapshot
    fun restoreSnapshot(context: Context) {
        val segment = UserSegmentHelper.currentSegment()
        val store = MMKV.defaultMMKV()
        val keepAlive = store.getString("frc_valid_keepalive_${segment.jsonKey}", null)
        val localPush = store.getString("frc_valid_local_push_${segment.jsonKey}", null)
        if (keepAlive != null && localPush != null) {
            snapshot = Snapshot(segment, keepAlive, localPush)
            onSnapshotChanged?.invoke(snapshot!!)
        }
    }
    fun applyForCurrentSegment(context: Context): Boolean {
        ensureInitialized(context)
        if (!UserSegmentHelper.isResolved()) return false
        val rc = FirebaseRemoteConfig.getInstance()
        val paid = UserSegmentHelper.preferPaid()
        val segment = UserSegmentHelper.currentSegment()
        val stored = MMKV.defaultMMKV()
        val next = Snapshot(UserSegmentHelper.currentSegment(),
            DualSegmentJson.extract(rc.getString(KEY_KEEPALIVE_CONFIG), paid)
                ?: stored.getString("frc_valid_keepalive_${segment.jsonKey}", null)
                ?: DualSegmentJson.extract(defaultKeepAliveJson(), paid)!!,
            DualSegmentJson.extract(rc.getString(KEY_LOCAL_PUSH_CONFIG), paid)
                ?: stored.getString("frc_valid_local_push_${segment.jsonKey}", null)
                ?: DualSegmentJson.extract(defaultLocalPushJson(context), paid)!!)
        // Invalid remote documents must not replace the last valid snapshot.
        val valid = runCatching {
            val keepAlive = JsonParser.parseString(next.keepAliveJson).asJsonObject
            keepAlive.has("kpa_service_switch") && keepAlive.has("kpa_fcm_switch") &&
                LocalPushSnapshotPolicy.isValid(next.localPushJson)
        }.getOrDefault(false)
        if (!valid) return false
        if (next == snapshot) return false
        snapshot = next
        MMKV.defaultMMKV().putString("frc_valid_keepalive_${next.segment.jsonKey}", next.keepAliveJson)
        MMKV.defaultMMKV().putString("frc_valid_local_push_${next.segment.jsonKey}", next.localPushJson)
        onSnapshotChanged?.invoke(next)
        return true
    }

    fun refreshIfNeeded(context: Context, reason: RefreshReason = RefreshReason.COLD_START) {
        ensureInitialized(context)
        val guard = if (reason == RefreshReason.COLD_START) coldStarted else hotInFlight
        if (!guard.compareAndSet(false, true)) return
        applyForCurrentSegment(context)
        FirebaseRemoteConfig.getInstance().fetchAndActivate()
            .addOnCompleteListener {
                if (reason == RefreshReason.HOT_START) hotInFlight.set(false)
                applyForCurrentSegment(context.applicationContext)
            }
    }
}
