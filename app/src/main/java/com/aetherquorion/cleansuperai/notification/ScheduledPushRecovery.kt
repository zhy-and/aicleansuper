package com.aetherquorion.cleansuperai.notification

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.aetherquorion.cleansuperai.analytics.AnalyticsBridge
import com.tencent.mmkv.MMKV

/** Alarm delivery and periodic recovery use one durable slot ledger. */
object ScheduledPushDispatcher {
    fun onAlarm(context: Context, sceneKey: String, index: Int, timeLabel: String) {
        val scene = LocalPushConfig.current(context).scenes[sceneKey] ?: return
        if (!scene.enabled || scene.times.getOrNull(index)?.trim() != timeLabel) return
        val slot = ScheduledPushSlots.dueSlots(mapOf(sceneKey to scene.times), System.currentTimeMillis())
            .firstOrNull { it.index == index && it.timeLabel == timeLabel } ?: return
        submit(context, slot, "alarm")
    }

    fun recover(context: Context) {
        val config = LocalPushConfig.current(context)
        if (!config.enabled) return
        val times = config.scenes.filterValues { it.enabled && it.times.isNotEmpty() }
            .mapValues { it.value.times }
        ScheduledPushSlots.dueSlots(times, System.currentTimeMillis()).forEach {
            submit(context, it, "recovery")
        }
    }

    @Synchronized private fun submit(context: Context, slot: ScheduledPushSlot, source: String) {
        val store = MMKV.defaultMMKV() ?: return
        val key = "lp_last_submitted_slot_${slot.sceneKey}_${slot.index}"
        if (store.decodeString(key) == slot.id) return
        if (source == "recovery") AnalyticsBridge.logLpFunnel(context, "补发", slot.sceneKey,
            slot.sceneKey, slotId = slot.id, triggerSource = source,
            scheduledAtMs = slot.scheduledAtMillis, actualAtMs = System.currentTimeMillis())
        if (LocalEventPushHelper.show(context, slot.sceneKey)) store.encode(key, slot.id)
    }
}

object ScheduledPushRecoveryJob {
    private const val JOB_ID = 20_849_621
    fun ensureScheduled(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        if (scheduler.getPendingJob(JOB_ID) != null) return
        scheduler.schedule(JobInfo.Builder(JOB_ID,
            ComponentName(context, ScheduledPushRecoveryJobService::class.java))
            .setPeriodic(15L * 60L * 1000L).setPersisted(true).build())
    }
}

class ScheduledPushRecoveryJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        ScheduledPushDispatcher.recover(this)
        return false
    }
    override fun onStopJob(params: JobParameters?): Boolean = true
}
