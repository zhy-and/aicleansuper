package com.aetherquorion.cleansuperai.notification

import android.app.Notification
import android.app.NotificationManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.blankj.utilcode.util.LogUtils

/** In-process, same-ID updates. Never enters logical-show accounting or analytics. */
internal object LocalPushRepeatSubmitter {
    private val handler = Handler(Looper.getMainLooper())
    private val lock = Any()
    private val pending = HashMap<Int, Entry>()
    private var latestPostedId: Int? = null
    private var latestDeliveryId: String? = null
    private var trackingForeground = false

    private class Entry(
        val id: Int,
        val deliveryId: String,
        val manager: NotificationManager,
        val notification: Notification,
        val plan: LocalPushRepeatPolicy.Plan,
        val canSubmit: () -> Boolean,
    ) {
        val cycle = LocalPushRepeatCycle(deliveryId, plan.extraCount)
        lateinit var runnable: Runnable
    }

    fun trackForeground() {
        if (trackingForeground) return
        trackingForeground = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { cancelAll() }
        })
    }

    fun schedule(
        id: Int,
        deliveryId: String,
        manager: NotificationManager,
        notification: Notification,
        plan: LocalPushRepeatPolicy.Plan,
        canSubmit: () -> Boolean,
    ) {
        if (plan.extraCount <= 0) return
        synchronized(lock) {
            if (latestPostedId != id || latestDeliveryId != deliveryId) return
            cancelLocked(id)
            val entry = Entry(id, deliveryId, manager, notification, plan, canSubmit)
            entry.runnable = Runnable { runEntry(entry) }
            pending[id] = entry
            handler.postDelayed(entry.runnable, plan.intervalMs)
        }
    }

    /** A successful newer notification owns the repeat slot; older notifications stay in the shade. */
    fun postFirst(id: Int, deliveryId: String, post: () -> Unit) = synchronized(lock) {
        post()
        cancelAllLocked()
        latestPostedId = id
        latestDeliveryId = deliveryId
    }

    fun pendingRepeatId(): Int? = synchronized(lock) { pending.keys.firstOrNull() }

    fun cancel(id: Int) = synchronized(lock) {
        cancelLocked(id)
        if (latestPostedId == id) {
            latestPostedId = null
            latestDeliveryId = null
        }
    }

    fun cancelAll() = synchronized(lock) {
        cancelAllLocked()
        latestPostedId = null
        latestDeliveryId = null
    }

    private fun cancelAllLocked() {
        for (id in pending.keys.toList()) cancelLocked(id)
    }

    private fun cancelLocked(id: Int) {
        pending.remove(id)?.let {
            it.cycle.cancel()
            handler.removeCallbacks(it.runnable)
        }
    }

    private fun runEntry(entry: Entry) {
        synchronized(lock) {
            if (pending[entry.id] !== entry) return
            if (runCatching { entry.canSubmit() }.getOrDefault(false).not()) {
                cancelLocked(entry.id)
                return
            }
            val index = entry.cycle.claim(entry.deliveryId) ?: run {
                cancelLocked(entry.id)
                return
            }
            val result = runCatching { entry.manager.notify(entry.id, entry.notification) }
            if (result.isFailure) {
                LogUtils.w("LocalPushRepeat", "重复提交失败 id=${entry.id} index=$index error=${result.exceptionOrNull()?.message}")
                cancelLocked(entry.id)
                return
            }
            LogUtils.i("LocalPushRepeat", "重复提交成功 id=${entry.id} index=$index/${entry.plan.extraCount}")
            if (index < entry.plan.extraCount) handler.postDelayed(entry.runnable, entry.plan.intervalMs)
            else pending.remove(entry.id)
        }
    }
}
