package com.aetherquorion.cleansuperai.ui.common

import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ProgressBar
import android.view.Gravity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.ContextCompat
import com.aetherquorion.cleansuperai.R

object ParityUi {
    enum class ButtonKind { PRIMARY, SECONDARY, DANGER }

    fun button(context: Context, kind: ButtonKind = ButtonKind.SECONDARY): AppCompatButton =
        AppCompatButton(context).apply {
            val backgroundRes = when (kind) {
                ButtonKind.PRIMARY -> R.drawable.bg_button_primary
                ButtonKind.SECONDARY -> R.drawable.bg_button_secondary
                ButtonKind.DANGER -> R.drawable.bg_button_danger
            }
            background = ContextCompat.getDrawable(context, backgroundRes)
            backgroundTintList = null
            setTextColor(ContextCompat.getColor(context, when (kind) {
                ButtonKind.PRIMARY -> R.color.white
                ButtonKind.SECONDARY -> R.color.text_primary
                ButtonKind.DANGER -> R.color.danger
            }))
            isAllCaps = false
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            minHeight = dp(context, 48)
            setPadding(dp(context, 16), 0, dp(context, 16), 0)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 48)).apply {
                topMargin = dp(context, 8)
            }
        }

    fun back(context: Context, onClick: () -> Unit): ImageView = ImageView(context).apply {
        setImageResource(R.drawable.ic_arrow_back)
        imageTintList = ContextCompat.getColorStateList(context, R.color.text_primary)
        setBackgroundResource(R.drawable.bg_icon_button)
        contentDescription = context.getString(R.string.detail_back)
        scaleType = ImageView.ScaleType.CENTER
        layoutParams = LinearLayout.LayoutParams(dp(context, 44), dp(context, 44))
        setOnClickListener { onClick() }
    }

    fun title(context: Context, label: String): TextView = TextView(context).apply {
        text = label
        textSize = 26f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(context, 14) }
    }

    fun subtitle(context: Context, label: String): TextView = TextView(context).apply {
        text = label
        textSize = 14f
        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(context, 6) }
    }

    fun checkbox(context: Context): AppCompatCheckBox = AppCompatCheckBox(context).apply {
        setBackgroundResource(R.drawable.bg_button_secondary)
        buttonTintList = ContextCompat.getColorStateList(context, R.color.accent_primary)
        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        textSize = 14f
        minHeight = dp(context, 56)
        setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(context, 8) }
    }

    fun status(context: Context): TextView = TextView(context).apply {
        textSize = 14f
        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(context, 14)
            bottomMargin = dp(context, 8)
        }
    }

    fun progress(context: Context): ProgressBar = ProgressBar(context).apply {
        layoutParams = LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
    }

    private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()
}
