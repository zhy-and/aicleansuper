package com.aetherquorion.cleansuperai

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import android.widget.TextView
import com.aetherquorion.cleansuperai.widget.CircularScanView

/** Displays elapsed startup wait time, capped until navigation completes. */
internal class LaunchProgress(
    private val ring: CircularScanView,
    private val label: TextView,
) {
    private var animator: ValueAnimator? = null

    fun start(durationMillis: Long) {
        stop()
        update(0)
        animator = ValueAnimator.ofInt(0, 99).apply {
            duration = durationMillis
            interpolator = LinearInterpolator()
            addUpdateListener { update(it.animatedValue as Int) }
            start()
        }
    }

    fun complete() {
        stop()
        update(100)
    }

    fun stop() {
        animator?.cancel()
        animator = null
    }

    private fun update(percent: Int) {
        ring.setProgress(percent / 100f)
        label.text = "$percent%"
    }
}
