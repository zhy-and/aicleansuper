package com.aetherquorion.cleansuperai.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.blankj.utilcode.util.LogUtils

/**
 * 锁屏 / 亮屏本地推送监听。
 *
 * - screen_lock：收到 [Intent.ACTION_SCREEN_OFF]（灭屏/锁屏）
 * - screen_on_after_lock：收到 [Intent.ACTION_USER_PRESENT] 后延迟 [UNLOCK_PUSH_DELAY_MS] 再展示
 *
 * 真正限频交给 LocalPushConfig 的 scene 冷却/日限。
 * 调试过滤 tag：ScreenStateWatcher / LocalEventPush / LocalPushConfig
 */
object ScreenStateWatcher {
    private const val TAG = "ScreenStateWatcher"
    private const val UNLOCK_PUSH_DELAY_MS = 2_000L

    @Volatile
    private var registered = false

    private val handler = Handler(Looper.getMainLooper())
    private var pendingUnlockPush: Runnable? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val action = intent?.action ?: return
            val appContext = context.applicationContext
            when (action) {
                Intent.ACTION_SCREEN_OFF -> {
                    cancelPendingUnlockPush("SCREEN_OFF")
                    LogUtils.i(TAG, "收到 SCREEN_OFF（锁屏/灭屏），尝试 screen_lock")
                    val shown = LocalEventPushHelper.show(
                        appContext,
                        LocalEventPushHelper.EventType.SCREEN_LOCK,
                    )
                    LogUtils.i(TAG, "screen_lock 场景尝试结果：shown=$shown")
                }
                Intent.ACTION_USER_PRESENT -> {
                    scheduleUnlockPush(appContext)
                }
                else -> LogUtils.w(TAG, "未处理的 action=$action")
            }
        }
    }

    fun start(context: Context) {
        if (registered) {
            LogUtils.i(TAG, "屏幕状态监听已注册，跳过重复 start")
            return
        }
        val appContext = context.applicationContext
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // 系统广播：NOT_EXPORTED 即可
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                appContext.registerReceiver(receiver, filter)
            }
            registered = true
            LogUtils.i(TAG, "屏幕状态监听注册成功：SCREEN_OFF / USER_PRESENT")
        }.onFailure {
            LogUtils.e(TAG, "屏幕状态监听注册失败：${it.javaClass.simpleName} ${it.message.orEmpty()}")
        }
    }

    private fun scheduleUnlockPush(appContext: Context) {
        cancelPendingUnlockPush("USER_PRESENT 重新计时")
        val task = Runnable {
            pendingUnlockPush = null
            LogUtils.i(TAG, "解锁后延迟 ${UNLOCK_PUSH_DELAY_MS}ms 到期，尝试 screen_on_after_lock")
            val shown = LocalEventPushHelper.show(
                appContext,
                LocalEventPushHelper.EventType.SCREEN_ON_AFTER_LOCK,
            )
            LogUtils.i(TAG, "screen_on_after_lock 场景尝试结果：shown=$shown")
        }
        pendingUnlockPush = task
        handler.postDelayed(task, UNLOCK_PUSH_DELAY_MS)
        LogUtils.i(TAG, "收到 USER_PRESENT（亮屏解锁打开），${UNLOCK_PUSH_DELAY_MS}ms 后尝试 screen_on_after_lock")
    }

    private fun cancelPendingUnlockPush(reason: String) {
        val pending = pendingUnlockPush ?: return
        handler.removeCallbacks(pending)
        pendingUnlockPush = null
        LogUtils.i(TAG, "取消待展示的 screen_on_after_lock：reason=$reason")
    }
}
