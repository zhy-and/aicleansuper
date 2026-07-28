package com.example.cleansuperai.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.example.cleansuperai.R
import kotlin.math.min

/**
 * 环形扫描进度指示器：底层轨道 + 上层渐变扫描弧，可旋转动画。
 * 支持 determinate（progress 0..1）与 indeterminate（持续旋转）两种模式。
 */
class CircularScanView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        alpha = 76
    }

    private val innerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val rect = RectF()

    private var stroke = 12f
    private var startColor = ContextCompat.getColor(context, R.color.accent_primary)
    private var endColor = ContextCompat.getColor(context, R.color.accent_secondary)
    private var trackColor = ContextCompat.getColor(context, R.color.surface_dark_secondary)

    private var progress = 0f
    private var indeterminate = false
    private var rotation = 0f

    private var rotateAnimator: ValueAnimator? = null

    init {
        attrs?.let { applyAttrs(it) }
        updatePaints()
    }

    private fun applyAttrs(a: AttributeSet) {
        val ta = context.obtainStyledAttributes(a, R.styleable.CircularScanView, 0, 0)
        try {
            stroke = ta.getDimension(R.styleable.CircularScanView_csvStrokeWidth, stroke)
            startColor = ta.getColor(R.styleable.CircularScanView_csvStartColor, startColor)
            endColor = ta.getColor(R.styleable.CircularScanView_csvEndColor, endColor)
            trackColor = ta.getColor(R.styleable.CircularScanView_csvTrackColor, trackColor)
            indeterminate = ta.getBoolean(R.styleable.CircularScanView_csvIndeterminate, false)
        } finally {
            ta.recycle()
        }
        updatePaints()
    }

    private fun updatePaints() {
        trackPaint.color = trackColor
        trackPaint.strokeWidth = stroke
        progressPaint.strokeWidth = stroke
        progressGlowPaint.strokeWidth = stroke + (stroke * 0.55f)
    }

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        indeterminate = false
        stopRotation()
        invalidate()
    }

    fun setIndeterminate(value: Boolean) {
        indeterminate = value
        if (value) startRotation() else stopRotation()
        invalidate()
    }

    private fun startRotation() {
        if (rotateAnimator?.isRunning == true) return
        rotateAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 1400
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                rotation = it.animatedValue as Float
                invalidate()
            }
        }
        rotateAnimator?.start()
    }

    private fun stopRotation() {
        rotateAnimator?.cancel()
        rotateAnimator = null
        rotation = 0f
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopRotation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = stroke / 2f
        rect.set(inset, inset, w - inset, h - inset)
        val sweepGradient = SweepGradient(
            w / 2f,
            h / 2f,
            intArrayOf(startColor, endColor, startColor),
            null
        )
        progressPaint.shader = sweepGradient
        progressGlowPaint.shader = sweepGradient
        innerGlowPaint.shader = RadialGradient(
            w / 2f,
            h / 2f,
            min(w, h) * 0.26f,
            intArrayOf(
                withAlpha(startColor, 0.16f),
                withAlpha(endColor, 0.08f),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(width / 2f, height / 2f, min(width, height) * 0.22f, innerGlowPaint)
        canvas.drawArc(rect, 0f, 360f, false, trackPaint)

        if (indeterminate) {
            canvas.save()
            canvas.rotate(rotation, width / 2f, height / 2f)
            canvas.drawArc(rect, -90f, 270f, false, progressGlowPaint)
            canvas.drawArc(rect, -90f, 270f, false, progressPaint)
            canvas.restore()
        } else {
            val sweep = 360f * progress
            canvas.drawArc(rect, -90f, sweep, false, progressGlowPaint)
            canvas.drawArc(rect, -90f, sweep, false, progressPaint)
        }
    }

    private fun withAlpha(color: Int, alphaFraction: Float): Int {
        val alpha = (alphaFraction.coerceIn(0f, 1f) * 255).toInt()
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
