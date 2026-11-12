package com.aetherquorion.cleansuperai.config

import android.content.Context
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_REFER_INFO
import com.tencent.mmkv.MMKV
import java.util.concurrent.atomic.AtomicBoolean

object UserSegmentHelper {
    enum class Segment(val jsonKey: String) { ORGANIC("organic"), PAID("paid") }
    private const val LOCKED = "user_segment_locked"
    @Volatile private var resolved = false
    private val gateStarted = AtomicBoolean(false)
    @Volatile private var referFlowFinished = false

    fun isResolved(): Boolean = resolved
    fun currentSegment(): Segment {
        val store = MMKV.defaultMMKV()
        if (store.getString(LOCKED, "") == "paid") return Segment.PAID
        val refer = store.getString(DATA_CONSTANT_REFER_INFO, "").orEmpty()
        return if (refer.isEmpty() || refer.contains("organic")) Segment.ORGANIC else Segment.PAID
    }
    fun preferPaid(): Boolean = currentSegment() == Segment.PAID
    fun startGate(context: Context) {
        if (!gateStarted.compareAndSet(false, true)) return
        val app = context.applicationContext
        Thread {
            val deadline = android.os.SystemClock.elapsedRealtime() + 10_000L
            while (android.os.SystemClock.elapsedRealtime() < deadline) {
                if (MMKV.defaultMMKV().getString(DATA_CONSTANT_REFER_INFO, "").orEmpty().isNotEmpty() || referFlowFinished) break
                try { Thread.sleep(100) } catch (_: InterruptedException) { break }
            }
            resolve(app)
        }.start()
    }
    fun resolve(context: Context): Segment {
        MMKV.initialize(context.applicationContext)
        val next = currentSegment()
        MMKV.defaultMMKV().putString(LOCKED, next.jsonKey)
        resolved = true
        SegmentedRemoteConfig.applyForCurrentSegment(context)
        return next
    }
    fun onReferInfoUpdated(context: Context) {
        if (currentSegment() == Segment.PAID) resolve(context)
    }
    fun notifyReferFlowFinished(context: Context) {
        referFlowFinished = true
        if (!resolved) resolve(context)
    }
}
