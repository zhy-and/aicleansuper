package com.aetherquorion.cleansuperai.ui.vault

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.ui.common.TopFilterUi

/** PIN setup and unlock screen using the same six-dot keypad structure as Purgu. */
class VaultPinScreen(context: Context, private val onComplete: (String) -> Unit) : LinearLayout(context) {
    private val pin = StringBuilder()
    private val dots = mutableListOf<View>()
    private val hint = TextView(context)
    private val subtitle = TextView(context)
    private val error = TextView(context)
    private val biometric = TextView(context)
    private val back = TopFilterUi.toolbar(context, "Private vault") {}

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.bg_primary))
        setPadding(dp(20), dp(12), dp(20), dp(24))
        addView(back)
        addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_vault_lock_purgu)
            layoutParams = LayoutParams(dp(72), dp(72)).apply {
                gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(36)
            }
        })
        addView(hint.apply {
            gravity = Gravity.CENTER
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            layoutParams = LayoutParams(-1, -2).apply { topMargin = dp(16) }
        })
        addView(subtitle.apply {
            gravity = Gravity.CENTER
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            layoutParams = LayoutParams(-1, -2).apply { topMargin = dp(8) }
        })
        addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(-1, dp(42)).apply { topMargin = dp(20) }
            repeat(6) {
                val dot = View(context)
                dots += dot
                addView(dot, LayoutParams(dp(14), dp(14)).apply {
                    leftMargin = dp(8); rightMargin = dp(8)
                })
            }
        })
        addView(error.apply {
            gravity = Gravity.CENTER
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.danger))
            layoutParams = LayoutParams(-1, dp(28))
        })
        addView(biometric.apply {
            gravity = Gravity.CENTER
            text = "Use biometrics"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
            visibility = GONE
            layoutParams = LayoutParams(-1, dp(48))
        })
        addView(View(context), LayoutParams(1, 0, 1f))
        val keypad = GridLayout(context).apply { columnCount = 3; rowCount = 4 }
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫").forEach { key ->
            keypad.addView(TextView(context).apply {
                text = key
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                if (key.isNotEmpty()) {
                    setBackgroundResource(android.R.drawable.list_selector_background)
                    setOnClickListener { if (key == "⌫") deleteDigit() else appendDigit(key) }
                }
            }, GridLayout.LayoutParams().apply {
                width = 0
                height = dp(64)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            })
        }
        addView(keypad, LayoutParams(-1, dp(256)).apply {
            leftMargin = dp(20); rightMargin = dp(20)
        })
        refreshDots()
    }

    fun setBackClick(onClick: () -> Unit) { back.getChildAt(0).setOnClickListener { onClick() } }

    fun setBiometricClick(onClick: (() -> Unit)?) {
        biometric.visibility = if (onClick == null) GONE else VISIBLE
        biometric.setOnClickListener(if (onClick == null) null else View.OnClickListener { onClick() })
    }

    fun showStep(title: String, description: String, errorMessage: String? = null) {
        hint.text = title
        subtitle.text = description
        error.text = errorMessage.orEmpty()
        pin.clear()
        refreshDots()
    }

    private fun appendDigit(digit: String) {
        if (pin.length == 6) return
        error.text = ""
        pin.append(digit)
        refreshDots()
        if (pin.length == 6) onComplete(pin.toString())
    }

    private fun deleteDigit() {
        if (pin.isEmpty()) return
        pin.deleteCharAt(pin.length - 1)
        error.text = ""
        refreshDots()
    }

    private fun refreshDots() {
        dots.forEachIndexed { index, view ->
            view.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(context,
                    if (index < pin.length) R.color.accent_primary else R.color.surface_card))
                setStroke(dp(1), ContextCompat.getColor(context, R.color.accent_primary))
            }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
