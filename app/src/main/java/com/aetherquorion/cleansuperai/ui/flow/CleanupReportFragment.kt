package com.aetherquorion.cleansuperai.ui.flow

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aetherquorion.cleansuperai.MainActivity
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.ui.clean.CleanCenterFragment
import com.aetherquorion.cleansuperai.ui.common.ParityUi

class CleanupReportFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            setBackgroundColor(ContextCompat.getColor(context, R.color.bg_primary))
        }
        root.addView(ParityUi.back(context) { parentFragmentManager.popBackStack() })
        root.addView(ParityUi.title(context, getString(R.string.cleanup_report_title)))
        root.addView(ParityUi.subtitle(context, requireArguments().getString(ARG_FEATURE).orEmpty()))
        root.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dp(20), dp(24), dp(20), dp(24))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(28); bottomMargin = dp(20) }
            addView(TextView(context).apply {
                text = Formatter.formatShortFileSize(context, requireArguments().getLong(ARG_BYTES))
                textSize = 32f
                setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
            })
            addView(ParityUi.subtitle(context, getString(R.string.cleanup_report_freed)))
            addView(ParityUi.subtitle(context, getString(R.string.cleanup_report_count,
                requireArguments().getInt(ARG_DELETED), requireArguments().getInt(ARG_ATTEMPTED))))
        })
        root.addView(ParityUi.button(context, ParityUi.ButtonKind.PRIMARY).apply {
            setText(R.string.cleanup_report_other)
            setOnClickListener { (activity as? MainActivity)?.openDetail(CleanCenterFragment(), "clean_center") }
        })
        root.addView(ParityUi.button(context).apply {
            setText(R.string.cleanup_report_done)
            setOnClickListener { parentFragmentManager.popBackStack() }
        })
        return root
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val ARG_FEATURE = "feature"
        private const val ARG_ATTEMPTED = "attempted"
        private const val ARG_DELETED = "deleted"
        private const val ARG_BYTES = "bytes"
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
