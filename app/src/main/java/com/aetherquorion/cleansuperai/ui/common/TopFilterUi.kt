package com.aetherquorion.cleansuperai.ui.common

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View
import androidx.core.content.ContextCompat
import com.aetherquorion.cleansuperai.R

/** Compact filter controls shared by the feature pages. */
object TopFilterUi {
    private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()

    fun surface(context: Context, radius: Int = 12): GradientDrawable = GradientDrawable().apply {
        setColor(ContextCompat.getColor(context, R.color.surface_card))
        cornerRadius = dp(context, radius).toFloat()
        setStroke(dp(context, 1), ContextCompat.getColor(context, R.color.stroke_light))
    }

    fun bar(context: Context, height: Int): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = surface(context)
        layoutParams = LinearLayout.LayoutParams(-1, dp(context, height)).apply { topMargin = dp(context, 14) }
    }

    fun label(context: Context, textValue: String): TextView = TextView(context).apply {
        text = textValue
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        minHeight = dp(context, 48)
    }

    fun selected(tab: TextView, selected: Boolean) {
        val context = tab.context
        tab.setTextColor(ContextCompat.getColor(context, if (selected) R.color.accent_primary else R.color.text_primary))
        tab.background = if (selected) GradientDrawable().apply {
            setColor(ContextCompat.getColor(context, R.color.accent_primary_soft))
            cornerRadius = dp(context, 10).toFloat()
        } else null
        tab.isSelected = selected
    }

    fun toolbar(context: Context, title: String, onBack: () -> Unit): LinearLayout = LinearLayout(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(-1, dp(context, 56))
        addView(ParityUi.back(context, onBack), LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)))
        addView(label(context, title).apply {
            textSize = 16f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(0, -1, 1f))
        addView(View(context), LinearLayout.LayoutParams(dp(context, 44), 1))
    }
}
