package com.aetherquorion.cleansuperai.analytics

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean

/** Persisted 30-minute process heartbeat, matching Purgu's 10019 route. */
object ProcessHeartbeatTracker {
    private const val JOB_ID = 20_849_619
    private const val INTERVAL_MS = 30L * 60L * 1000L
    private val started = AtomicBoolean(false)
    fun start(context: Context) {
        if (started.compareAndSet(false, true)) scheduleNext(context)
    }
    fun reportAndScheduleNext(context: Context) {
        ProcessHeartbeatReporter.reportProcessHeartbeat(context)
        scheduleNext(context)
    }
    private fun scheduleNext(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        scheduler.schedule(JobInfo.Builder(JOB_ID,
            ComponentName(context, ProcessHeartbeatJobService::class.java))
            .setMinimumLatency(INTERVAL_MS)
            .setOverrideDeadline(INTERVAL_MS + 60_000L)
            .setPersisted(true)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).build())
    }
}

class ProcessHeartbeatJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        ProcessHeartbeatTracker.reportAndScheduleNext(this)
        return false
    }
    override fun onStopJob(params: JobParameters?): Boolean = true
}
