package com.aetherquorion.cleansuperai

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity


import com.google.firebase.analytics.FirebaseAnalytics

class UninstallGuideActivity : AppCompatActivity() {
    private val flow = UninstallGuideFlow()
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = getColor(R.color.screen_bg)
        window.navigationBarColor = getColor(R.color.screen_bg)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.screen_bg))
            fitsSystemWindows = true
        }
        setContentView(root)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                trackReasonExit(UninstallGuideFlow.ExitAction.BACK)
                flow.back()
                openHome()
            }
        })
        if (savedInstanceState?.getBoolean(KEY_REASONS) == true) {
            flow.continueUninstall()
            flow.restoreReason(savedInstanceState.getString(KEY_SELECTED_REASON))
        }
        render()
        if (savedInstanceState == null && intent.action == ACTION_SHORTCUT_UNINSTALL) {
            FirebaseAnalytics.getInstance(this).logEvent(EVENT_SHORTCUT_UNINSTALL_CLICK, null)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_REASONS, flow.page == UninstallGuideFlow.Page.REASONS)
        outState.putString(KEY_SELECTED_REASON, flow.selectedReason.analyticsValue)
        super.onSaveInstanceState(outState)
    }

    private fun render() {
        root.removeAllViews()
        val back = TextView(this).apply {
            text = "‹"
            textSize = 38f
            setTextColor(getColor(R.color.text_primary))
            gravity = Gravity.CENTER_VERTICAL
            contentDescription = getString(R.string.uninstall_guide_back)
            setPadding(dp(26), 0, 0, 0)
            setOnClickListener {
                trackReasonExit(UninstallGuideFlow.ExitAction.BACK)
                flow.back()
                openHome()
            }
        }
        root.addView(back, LinearLayout.LayoutParams(-1, dp(64)))
        if (flow.page == UninstallGuideFlow.Page.RETAIN) renderRetain() else renderReasons()
    }

    private fun renderRetain() {
        val scroll = ScrollView(this).apply { isFillViewport = false }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(18), dp(24), dp(12))
        }
        body.addView(label(R.string.uninstall_guide_wait, 26f, true, R.color.text_primary, Gravity.CENTER).apply {
            setPadding(0, 0, 0, dp(12))
        }, LinearLayout.LayoutParams(-1, -2))
        body.addView(label(R.string.uninstall_guide_more_space, 18f, false, R.color.text_secondary, Gravity.CENTER).apply {
            text = getString(R.string.uninstall_guide_more_space, getString(R.string.app_name))
            setPadding(0, 0, 0, dp(28))
        }, LinearLayout.LayoutParams(-1, -2))
        body.addView(featureCard(R.drawable.ic_clean, R.string.uninstall_guide_smart_clean,
            R.string.uninstall_guide_scan_files, R.string.uninstall_guide_scan_now) { openHome("cache") },
            LinearLayout.LayoutParams(-1, -2))
        body.addView(featureCard(R.drawable.ic_uninstall_guide_apps, R.string.uninstall_guide_unused_apps,
            R.string.uninstall_guide_find_apps, R.string.uninstall_guide_view_apps) {
            openHome("apps")
            finish()
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(12), dp(28), dp(22))
        }
        footer.addView(primaryButton(getString(R.string.uninstall_guide_keep, getString(R.string.app_name))) { openHome() },
            LinearLayout.LayoutParams(-1, dp(58)))
        footer.addView(label(R.string.uninstall_guide_continue, 18f, true, R.color.text_secondary, Gravity.CENTER).apply {
            setOnClickListener { flow.continueUninstall(); render() }
        }, LinearLayout.LayoutParams(-1, dp(64)))
        root.addView(footer)
    }

    private fun renderReasons() {
        val reasons = intArrayOf(
            R.string.uninstall_reason_no_need, R.string.uninstall_reason_ads,
            R.string.uninstall_reason_space, R.string.uninstall_reason_missing_files,
            R.string.uninstall_reason_hard, R.string.uninstall_reason_slow,
            R.string.uninstall_reason_other,
        )
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(6), dp(28), dp(12))
        }
        body.addView(label(R.string.uninstall_guide_why, 24f, true, R.color.text_primary, Gravity.START).apply {
            text = getString(R.string.uninstall_guide_why, getString(R.string.app_name))
            setPadding(0, 0, 0, dp(24))
        })
        val radios = mutableListOf<RadioButton>()
        reasons.forEachIndexed { index, reason ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(22), 0, dp(14), 0)
                background = rounded(R.color.white, 12)
            }
            row.addView(label(reason, 16f, false, R.color.text_primary, Gravity.CENTER_VERTICAL),
                LinearLayout.LayoutParams(0, -1, 1f))
            val radio = RadioButton(this).apply {
                isChecked = UninstallGuideFlow.Reason.entries[index] == flow.selectedReason
                isClickable = false
                buttonTintList = ColorStateList.valueOf(getColor(R.color.tab_selected))
            }
            radios.add(radio)
            row.addView(radio)
            row.setOnClickListener {
                flow.selectReason(UninstallGuideFlow.Reason.entries[index])
                radios.forEachIndexed { i, button ->
                    button.isChecked = UninstallGuideFlow.Reason.entries[i] == flow.selectedReason
                }
            }
            body.addView(row, LinearLayout.LayoutParams(-1, dp(70)).apply { bottomMargin = dp(14) })
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), 0, dp(28), dp(22))
        }
        footer.addView(label(R.string.uninstall_guide_private, 12f, false, R.color.text_secondary, Gravity.CENTER),
            LinearLayout.LayoutParams(-1, dp(42)))
        footer.addView(primaryButton(getString(R.string.action_cancel)) {
            trackReasonExit(UninstallGuideFlow.ExitAction.CANCEL)
            flow.cancel()
            openHome()
        },
            LinearLayout.LayoutParams(-1, dp(58)))
        footer.addView(label(R.string.action_uninstall, 18f, true, R.color.text_secondary, Gravity.CENTER).apply {
            setOnClickListener {
                trackReasonExit(UninstallGuideFlow.ExitAction.UNINSTALL)
                FirebaseAnalytics.getInstance(this@UninstallGuideActivity).logEvent(
                    EVENT_UNINSTALL_CONFIRM_CLICK,
                    Bundle().apply { putString("reason", flow.selectedReason.analyticsValue) },
                )
                flow.uninstall()
                openAppSettings()
            }
        }, LinearLayout.LayoutParams(-1, dp(60)))
        root.addView(footer)
    }

    private fun featureCard(icon: Int, title: Int, subtitle: Int, action: Int, onClick: () -> Unit): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(18))
            background = rounded(R.color.white, 16)
        }
        card.addView(ImageView(this).apply {
            setImageResource(icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = rounded(R.color.soft_gray, 12)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }, LinearLayout.LayoutParams(dp(112), dp(112)))
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, 0, 0)
        }
        content.addView(label(title, 17f, true, R.color.text_primary, Gravity.START))
        content.addView(label(subtitle, 13f, false, R.color.text_secondary, Gravity.START).apply {
            setPadding(0, dp(7), 0, dp(14))
        })
        content.addView(primaryButton(getString(action), onClick), LinearLayout.LayoutParams(-1, dp(45)))
        card.addView(content, LinearLayout.LayoutParams(0, -2, 1f))
        return card
    }

    private fun label(text: Int, size: Float, bold: Boolean, color: Int, gravityValue: Int) =
        TextView(this).apply {
            setText(text)
            textSize = size
            gravity = gravityValue
            setTextColor(getColor(color))
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun primaryButton(textValue: String, onClick: () -> Unit) = TextView(this).apply {
        text = textValue
        textSize = 17f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(getColor(R.color.white))
        background = rounded(R.color.tab_selected, 12)
        setOnClickListener { onClick() }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(getColor(color))
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun trackReasonExit(action: UninstallGuideFlow.ExitAction) {
        val selection = flow.reasonExit(action) ?: return
        FirebaseAnalytics.getInstance(this).logEvent(
            EVENT_UNINSTALL_REASON_SELECTED,
            Bundle().apply {
                putString("reason", selection.reason)
                putString("exit_action", selection.exitAction)
            },
        )
    }

    private fun openHome(scanMode: String? = null) {
        val home = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (scanMode != null) putExtra(MainActivity.EXTRA_WIDGET_ROUTE, scanMode)
        }
        startActivity(home)
        finish()
    }

    private fun openAppSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)))
            finish()
        } catch (_: ActivityNotFoundException) {
            openHome()
        }
    }

    companion object {
        const val ACTION_SHORTCUT_UNINSTALL = "com.aetherquorion.cleansuperai.action.SHORTCUT_UNINSTALL"
        const val EVENT_SHORTCUT_UNINSTALL_CLICK = "shortcut_uninstall_click"
        const val EVENT_UNINSTALL_REASON_SELECTED = "uninstall_reason_selected"
        const val EVENT_UNINSTALL_CONFIRM_CLICK = "uninstall_confirm_click"
        private const val KEY_REASONS = "reasons_page"
        private const val KEY_SELECTED_REASON = "selected_reason"
    }
}
