package com.aetherquorion.cleansuperai.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.blankj.utilcode.util.LogUtils
import com.tencent.mmkv.MMKV

/**
 * 电量 / 充电相关本地推送监听。
 *
 * 触发场景（均为边沿，不是“持续满足条件就狂推”）：
 * 1. battery_below_50：电量从 ≥50 掉到 <50
 * 2. battery_below_20：电量从 ≥20 掉到 <20
 * 3. charging：未插电 → 插电
 * 4. charging_disconnect：插电 → 未插电（充电断电）
 * 5. charging_to_80：本次充电会话中电量恰好等于 80% 时推一次；拔电后重置
 * 6. charging_full：本次充电会话中电量第一次到 100%；拔电后重置，允许下次再提醒
 *
 * 调试日志统一 tag = BatteryStatusWatcher，配合 LocalEventPush / LocalPushConfig 一起看。
 */
object BatteryStatusWatcher {
    private const val TAG = "BatteryStatusWatcher"

    /** 客户端写死阈值（首版不进 FRC，方便调试时直接改这里） */
    private const val THRESHOLD_50 = 50
    private const val THRESHOLD_20 = 20
    private const val THRESHOLD_80 = 80
    private const val THRESHOLD_FULL = 100

    private const val KEY_LAST_LEVEL = "local_push_battery_last_level"
    private const val KEY_LAST_PLUGGED = "local_push_battery_last_plugged"
    /** 本次插电会话是否已推过「充电至 80%」；拔电后清 0 */
    private const val KEY_80_NOTIFIED_SESSION = "local_push_battery_80_notified_session"
    /** 本次插电会话是否已推过「充满」；拔电后清 0 */
    private const val KEY_FULL_NOTIFIED_SESSION = "local_push_battery_full_notified_session"
    private const val UNSET_LEVEL = -1

    @Volatile
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            handleBatteryChanged(context.applicationContext, intent, fromStickySeed = false)
        }
    }

    fun start(context: Context) {
        if (registered) {
            LogUtils.i(TAG, "电量监听已注册，跳过重复 start")
            return
        }
        val appContext = context.applicationContext
        ensureMmkv(appContext)
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        runCatching {
            // sticky：registerReceiver 返回值就是当前电量快照，用来初始化 last_level / last_plugged，避免一启动就误触发边沿
            val sticky = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                appContext.registerReceiver(receiver, filter)
            }
            registered = true
            LogUtils.i(TAG, "电量监听动态注册成功：ACTION_BATTERY_CHANGED")
            if (sticky != null) {
                LogUtils.i(TAG, "使用 sticky BATTERY_CHANGED 做初始种子，不触发推送")
                handleBatteryChanged(appContext, sticky, fromStickySeed = true)
            } else {
                LogUtils.w(TAG, "未拿到 sticky BATTERY_CHANGED，等待后续广播再初始化状态")
            }
        }.onFailure {
            LogUtils.e(TAG, "电量监听注册失败：${it.javaClass.simpleName} ${it.message.orEmpty()}")
        }
    }

    private fun handleBatteryChanged(context: Context, intent: Intent, fromStickySeed: Boolean) {
        val level = readBatteryPercent(intent)
        val plugged = isPlugged(intent)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val store = MMKV.defaultMMKV()
        val lastLevel = store.getInt(KEY_LAST_LEVEL, UNSET_LEVEL)
        val lastPlugged = store.contains(KEY_LAST_PLUGGED) && store.getBoolean(KEY_LAST_PLUGGED, false)
        val hasLastPlugged = store.contains(KEY_LAST_PLUGGED)
        val eightyNotified = store.getBoolean(KEY_80_NOTIFIED_SESSION, false)
        val fullNotified = store.getBoolean(KEY_FULL_NOTIFIED_SESSION, false)

        LogUtils.i(
            TAG,
            "收到电量广播：seed=$fromStickySeed level=$level% plugged=$plugged status=$status " +
                "lastLevel=$lastLevel lastPlugged=${if (hasLastPlugged) lastPlugged else "unset"} " +
                "eightyNotifiedSession=$eightyNotified fullNotifiedSession=$fullNotified",
        )

        if (fromStickySeed || lastLevel == UNSET_LEVEL || !hasLastPlugged) {
            // 首次：只记账，不推送（防止冷启动时把“当前已低于 50”当成穿越）
            persistState(store, level, plugged)
            if (!plugged) {
                store.putBoolean(KEY_80_NOTIFIED_SESSION, false)
                store.putBoolean(KEY_FULL_NOTIFIED_SESSION, false)
            }
            LogUtils.i(
                TAG,
                "电量状态已种子化，本次不触发场景：level=$level plugged=$plugged " +
                    "reason=${if (fromStickySeed) "sticky_seed" else "missing_history"}",
            )
            return
        }

        // 1) 插电 / 断电边沿
        if (!lastPlugged && plugged) {
            LogUtils.i(TAG, "检测到开始充电边沿：lastPlugged=$lastPlugged -> plugged=$plugged，尝试 charging")
            val shown = LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.CHARGING)
            LogUtils.i(TAG, "charging 场景尝试结果：shown=$shown")
            // 新充电会话：允许再次「80% / 充满」提醒
            store.putBoolean(KEY_80_NOTIFIED_SESSION, false)
            store.putBoolean(KEY_FULL_NOTIFIED_SESSION, false)
        } else if (lastPlugged && !plugged) {
            LogUtils.i(TAG, "检测到充电断电边沿：lastPlugged=$lastPlugged -> plugged=$plugged，尝试 charging_disconnect")
            val shown = LocalEventPushHelper.show(
                context,
                LocalEventPushHelper.EventType.CHARGING_DISCONNECT,
            )
            LogUtils.i(TAG, "charging_disconnect 场景尝试结果：shown=$shown")
            LogUtils.i(TAG, "拔电后清空 80%/满电会话标记，允许下次充电再推")
            store.putBoolean(KEY_80_NOTIFIED_SESSION, false)
            store.putBoolean(KEY_FULL_NOTIFIED_SESSION, false)
        }

        // 2) 低电边沿：先判更紧急的 20%，再判 50%
        if (lastLevel >= THRESHOLD_20 && level < THRESHOLD_20) {
            LogUtils.i(
                TAG,
                "检测到电量向下穿越 20%：lastLevel=$lastLevel -> level=$level，尝试 battery_below_20",
            )
            val shown20 = LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.BATTERY_BELOW_20)
            LogUtils.i(TAG, "battery_below_20 场景尝试结果：shown=$shown20")
            if (!shown20 && lastLevel >= THRESHOLD_50 && level < THRESHOLD_50) {
                // 同一次大跌同时穿越 50 和 20：20 被频控挡住时，仍可尝试 50
                LogUtils.i(TAG, "20% 未展示且同时穿越了 50%，继续尝试 battery_below_50")
                val shown50 = LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.BATTERY_BELOW_50)
                LogUtils.i(TAG, "battery_below_50 场景尝试结果：shown=$shown50")
            }
        } else if (lastLevel >= THRESHOLD_50 && level < THRESHOLD_50) {
            LogUtils.i(
                TAG,
                "检测到电量向下穿越 50%：lastLevel=$lastLevel -> level=$level，尝试 battery_below_50",
            )
            val shown50 = LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.BATTERY_BELOW_50)
            LogUtils.i(TAG, "battery_below_50 场景尝试结果：shown=$shown50")
        } else {
            LogUtils.i(
                TAG,
                "未形成低电向下穿越：lastLevel=$lastLevel level=$level " +
                    "threshold50=$THRESHOLD_50 threshold20=$THRESHOLD_20",
            )
        }

        // 3) 充电至 80%：充电中且电量恰好 == 80，本会话只推一次
        val eightyNotifiedNow = store.getBoolean(KEY_80_NOTIFIED_SESSION, false)
        if (plugged && level == THRESHOLD_80 && !eightyNotifiedNow) {
            LogUtils.i(
                TAG,
                "检测到充电中电量恰好 $THRESHOLD_80%：level=$level eightyNotifiedSession=$eightyNotifiedNow，尝试 charging_to_80",
            )
            val shown80 = LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.CHARGING_TO_80)
            LogUtils.i(TAG, "charging_to_80 场景尝试结果：shown=$shown80")
            if (shown80) {
                store.putBoolean(KEY_80_NOTIFIED_SESSION, true)
                LogUtils.i(TAG, "已标记本次充电会话 80% 已提醒，拔电前不再重复 charging_to_80")
            }
        } else if (plugged && level == THRESHOLD_80) {
            LogUtils.i(TAG, "电量恰为 80% 但本会话已提醒过，跳过 charging_to_80：eightyNotifiedSession=$eightyNotifiedNow")
        }

        // 4) 充满：充电中且第一次到 100%
        val fullNotifiedNow = store.getBoolean(KEY_FULL_NOTIFIED_SESSION, false)
        if (plugged && level >= THRESHOLD_FULL && !fullNotifiedNow) {
            LogUtils.i(
                TAG,
                "检测到充电中首次到达 $THRESHOLD_FULL%：level=$level fullNotifiedSession=$fullNotifiedNow，尝试 charging_full",
            )
            val shownFull = LocalEventPushHelper.show(context, LocalEventPushHelper.EventType.CHARGING_FULL)
            LogUtils.i(TAG, "charging_full 场景尝试结果：shown=$shownFull")
            if (shownFull) {
                store.putBoolean(KEY_FULL_NOTIFIED_SESSION, true)
                LogUtils.i(TAG, "已标记本次充电会话满电已提醒，拔电前不再重复 charging_full")
            }
        } else if (plugged && level >= THRESHOLD_FULL) {
            LogUtils.i(TAG, "已满电但本会话已提醒过，跳过 charging_full：fullNotifiedSession=$fullNotifiedNow")
        }

        persistState(store, level, plugged)
        LogUtils.i(TAG, "电量状态已更新：level=$level plugged=$plugged")
    }

    private fun persistState(store: MMKV, level: Int, plugged: Boolean) {
        if (level in 0..100) {
            store.putInt(KEY_LAST_LEVEL, level)
        }
        store.putBoolean(KEY_LAST_PLUGGED, plugged)
    }

    private fun readBatteryPercent(intent: Intent): Int {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) {
            LogUtils.w(TAG, "电量字段异常：level=$level scale=$scale，回退为 -1")
            return UNSET_LEVEL
        }
        return ((level * 100f) / scale).toInt().coerceIn(0, 100)
    }

    private fun isPlugged(intent: Intent): Boolean {
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        // 0 = 未插电；AC/USB/无线任一非 0 都算充电中
        return plugged != 0
    }

    private fun ensureMmkv(context: Context) {
        runCatching { MMKV.defaultMMKV() }.onFailure {
            MMKV.initialize(context.applicationContext)
        }
    }
}
