package com.aetherquorion.cleansuperai.ui.flow

import android.os.Bundle
import android.text.format.Formatter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aetherquorion.cleansuperai.MainActivity
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.ui.common.inlineAdHost
import com.aetherquorion.cleansuperai.ui.cache.CacheCleanupFragment
import com.aetherquorion.cleansuperai.ui.common.ParityUi
import com.aetherquorion.cleansuperai.ui.largefiles.LargeFilesFragment
import com.aetherquorion.cleansuperai.ui.media.CategoryMediaFragment
import com.aetherquorion.cleansuperai.ui.tools.AppManagerFragment
import com.aetherquorion.cleansuperai.ui.tools.NetworkTrafficFragment
import com.aetherquorion.cleansuperai.ui.tools.SystemInfoFragment

class CleanupReportFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val context = requireContext()
        val infoOnly = arguments?.getBoolean(ARG_INFO_ONLY) == true
        val root = ScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(ContextCompat.getColor(context, R.color.bg_primary))
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(32))
        }
        root.addView(content)
        content.addView(ParityUi.back(context) { parentFragmentManager.popBackStack() })
        content.addView(inlineAdHost(context))
        content.addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_check_circle_filled)
            setBackgroundResource(R.drawable.bg_check_circle)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(dp(92), dp(92)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(26)
            }
            contentDescription = null
        })
        if (!infoOnly) content.addView(ParityUi.title(context, getString(R.string.cleanup_report_title)).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) }
        })
        if (!infoOnly) content.addView(ParityUi.subtitle(context, getString(R.string.cleanup_report_complete)).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) }
        })
        if (!infoOnly) content.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dp(20), dp(20), dp(20), dp(20))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) }
            addView(ParityUi.subtitle(context, requireArguments().getString(ARG_FEATURE).orEmpty()))
            val freedBytes = requireArguments().getLong(ARG_BYTES)
            if (freedBytes >= 0L) {
                addView(TextView(context).apply {
                    text = Formatter.formatShortFileSize(context, freedBytes)
                    textSize = 32f
                    setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
                })
                addView(ParityUi.subtitle(context, getString(R.string.cleanup_report_freed)))
            }
            addView(ParityUi.subtitle(context, getString(R.string.cleanup_report_count,
                requireArguments().getInt(ARG_DELETED), requireArguments().getInt(ARG_ATTEMPTED))))
        })
        content.addView(ParityUi.button(context, ParityUi.ButtonKind.PRIMARY).apply {
            setText(R.string.cleanup_report_other)
            layoutParams = LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(18) }
            setOnClickListener { (activity as? MainActivity)?.selectDestination(R.id.menu_home) }
        })
        content.addView(ParityUi.subtitle(context, getString(R.string.cleanup_report_explore)).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24); bottomMargin = dp(10) }
        })
        featureRow(content, R.drawable.ic_push_file, R.string.home_large_files) {
            openFeature(LargeFilesFragment(), "large_files")
        }
        featureRow(content, R.drawable.ic_push_clean, R.string.home_cache_cleanup) {
            openFeature(CacheCleanupFragment(), "cache_cleanup")
        }
        featureRow(content, R.drawable.ic_push_photo, R.string.home_picture_cleanup) {
            openFeature(CategoryMediaFragment.picture(), "picture_cleanup")
        }
        featureRow(content, R.drawable.ic_push_video, R.string.home_video_cleanup) {
            openFeature(CategoryMediaFragment.video(), "video_cleanup")
        }
        featureRow(content, R.drawable.ic_push_audio, R.string.home_audio_cleanup) {
            openFeature(CategoryMediaFragment.audio(), "audio_cleanup")
        }
        featureRow(content, R.drawable.ic_push_apps, R.string.tool_app_manager) {
            openFeature(AppManagerFragment(), "app_manager")
        }
        featureRow(content, R.drawable.ic_push_battery, R.string.home_battery_info) {
            openFeature(SystemInfoFragment.battery(), "battery_info")
        }
        featureRow(content, R.drawable.ic_push_network, R.string.home_network_traffic) {
            openFeature(NetworkTrafficFragment(), "network_traffic")
        }
        return root
    }

    private fun featureRow(parent: LinearLayout, icon: Int, title: Int, action: () -> Unit) {
        val context = requireContext()
        parent.addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(-1, dp(72)).apply { bottomMargin = dp(10) }
            addView(ImageView(context).apply {
                setImageResource(icon)
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
                contentDescription = null
            })
            addView(TextView(context).apply {
                setText(title)
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(14) }
            })
            addView(TextView(context).apply {
                setText(R.string.cleanup_report_go)
                setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
            })
            setOnClickListener { action() }
        })
    }

    private fun openFeature(fragment: Fragment, tag: String) {
        (activity as? MainActivity)?.openDetail(fragment, tag)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val ARG_FEATURE = "feature"
        private const val ARG_ATTEMPTED = "attempted"
        private const val ARG_DELETED = "deleted"
        private const val ARG_BYTES = "bytes"
        private const val ARG_INFO_ONLY = "info_only"
        fun infoOnly() = CleanupReportFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_INFO_ONLY, true) }
        }
        fun newInstance(feature: String, result: CleanupResult) = CleanupReportFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_FEATURE, feature)
                putInt(ARG_ATTEMPTED, result.attempted)
                putInt(ARG_DELETED, result.deleted)
                putLong(ARG_BYTES, result.freedBytes)
            }
        }
    }
}
