package com.aetherquorion.cleansuperai.ui.tools

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.view.Gravity
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.ui.common.inlineAdHost
import com.aetherquorion.cleansuperai.ui.common.FeatureAccess
import com.aetherquorion.cleansuperai.ui.common.FeaturePermissionCoordinator
import com.aetherquorion.cleansuperai.ui.common.ParityUi
import com.aetherquorion.cleansuperai.ui.common.TopFilterUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkTrafficFragment : Fragment() {
    private data class Usage(val packageName: String, val label: String, val icon: Drawable, val mobile: Long, val wifi: Long) {
        val total get() = mobile + wifi
    }
    private var selectedRange = NetworkRange.entries.first()
    private lateinit var rangeTabs: List<TextView>
    private lateinit var status: TextView
    private lateinit var mobileTotal: TextView
    private lateinit var wifiTotal: TextView
    private lateinit var progress: ProgressBar
    private lateinit var list: LinearLayout
    private var asked = false
    private val access = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (hasAccess()) load() else showAccessRequired()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(16))
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.bg_primary))
        }
        root.addView(TopFilterUi.toolbar(ctx, getString(R.string.tool_network_traffic)) { parentFragmentManager.popBackStack() })
        root.addView(inlineAdHost(ctx))
        val bar = TopFilterUi.bar(ctx, 50).apply { setPadding(dp(3), dp(3), dp(3), dp(3)) }
        rangeTabs = listOf(getString(R.string.traffic_month), getString(R.string.traffic_30_days), getString(R.string.traffic_24_hours)).mapIndexed { index, label ->
            TopFilterUi.label(ctx, label).also { tab ->
                bar.addView(tab, LinearLayout.LayoutParams(0, -1, 1f))
                tab.setOnClickListener {
                    selectedRange = NetworkRange.entries[index]
                    updateRangeTabs()
                    if (hasAccess()) load() else requestAccess()
                }
            }
        }
        root.addView(bar)
        updateRangeTabs()
        val totals = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, dp(92))
            gravity = Gravity.CENTER
        }
        fun column(label: String): TextView {
            val value = TopFilterUi.label(ctx, "0 B").apply { textSize = 20f }
            totals.addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                addView(TextView(ctx).apply {
                    text = label
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                    gravity = Gravity.CENTER
                })
                addView(value)
            }, LinearLayout.LayoutParams(0, -1, 1f))
            return value
        }
        mobileTotal = column(getString(R.string.traffic_mobile))
        wifiTotal = column(getString(R.string.traffic_wifi))
        root.addView(totals)
        root.addView(TextView(ctx).apply {
            text = getString(R.string.traffic_apps)
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(-1, dp(42))
            gravity = Gravity.BOTTOM
        })
        status = ParityUi.status(ctx).apply { visibility = View.GONE }
        root.addView(status)
        progress = ParityUi.progress(ctx)
        root.addView(progress)
        list = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            addView(list)
        })
        return root
    }

    override fun onResume() {
        super.onResume()
        if (!asked) {
            asked = true
            if (hasAccess()) load() else requestAccess()
        } else if (hasAccess()) load()
    }

    private fun hasAccess() = FeaturePermissionCoordinator.hasAccess(requireContext(), FeatureAccess.USAGE)
    private fun requestAccess() {
        access.launch(FeaturePermissionCoordinator.settingsIntent(requireContext(), FeatureAccess.USAGE))
        startActivity(Intent(requireContext(), com.aetherquorion.cleansuperai.ui.common.StoragePermissionGuideActivity::class.java).apply {
            putExtra(com.aetherquorion.cleansuperai.ui.common.StoragePermissionGuideActivity.EXTRA_ACCESS, true)
        })
    }

    private fun showAccessRequired() {
        progress.visibility = View.GONE
        status.setText(R.string.traffic_usage_required)
        status.visibility = View.VISIBLE
        status.setOnClickListener { requestAccess() }
    }

    private fun load() {
        progress.visibility = View.VISIBLE
        status.text = getString(R.string.traffic_loading)
        val selected = selectedRange
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val usage = withContext(Dispatchers.IO) { query(context, selected) }
            progress.visibility = View.GONE
            list.removeAllViews()
            val mobile = usage.sumOf { it.mobile }
            val wifi = usage.sumOf { it.wifi }
            mobileTotal.text = Formatter.formatShortFileSize(context, mobile)
            wifiTotal.text = Formatter.formatShortFileSize(context, wifi)
            status.text = if (usage.isEmpty()) getString(R.string.traffic_empty) else ""
            status.visibility = if (usage.isEmpty()) View.VISIBLE else View.GONE
            val max = usage.maxOfOrNull { it.total }?.coerceAtLeast(1L) ?: 1L
            usage.forEach { item ->
                list.addView(LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, dp(12), 0, dp(12))
                    addView(LinearLayout(requireContext()).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        addView(ImageView(context).apply {
                            setImageDrawable(item.icon)
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            contentDescription = item.label
                        }, LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginEnd = dp(12) })
                        addView(TextView(requireContext()).apply {
                            text = item.label
                            textSize = 14f
                            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                            maxLines = 1
                        }, LinearLayout.LayoutParams(0, -2, 1f))
                        addView(TextView(requireContext()).apply {
                            text = Formatter.formatShortFileSize(context, item.total)
                            textSize = 12f
                            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        })
                    })
                    addView(ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
                        this.max = 1000
                        progress = ((item.total * 1000) / max).toInt().coerceAtLeast(2)
                        progressTintList = ContextCompat.getColorStateList(context, R.color.accent_primary)
                        progressBackgroundTintList = ContextCompat.getColorStateList(context, R.color.accent_primary_soft)
                    }, LinearLayout.LayoutParams(-1, dp(4)).apply { topMargin = dp(8) })
                    setOnClickListener {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", item.packageName, null)))
                    }
                })
            }
        }
    }

    private fun query(context: Context, selected: NetworkRange): List<Usage> {
        val now = System.currentTimeMillis()
        val start = selected.start(now)
        val mobile = bytesByUid(context, ConnectivityManager.TYPE_MOBILE, start, now)
        val wifi = bytesByUid(context, ConnectivityManager.TYPE_WIFI, start, now)
        val pm = context.packageManager
        return (mobile.keys + wifi.keys).distinct().mapNotNull { uid ->
            val name = pm.getPackagesForUid(uid)?.firstOrNull() ?: return@mapNotNull null
            val appInfo = runCatching { pm.getApplicationInfo(name, 0) }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: name
            val icon = appInfo?.let { runCatching { it.loadIcon(pm) }.getOrNull() } ?: pm.defaultActivityIcon
            Usage(name, label, icon, mobile[uid] ?: 0L, wifi[uid] ?: 0L)
        }.filter { it.total > 0 }.sortedByDescending { it.total }
    }

    private fun bytesByUid(context: Context, type: Int, start: Long, end: Long): Map<Int, Long> {
        val manager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val stats = runCatching { manager.querySummary(type, null, start, end) }.getOrNull() ?: return emptyMap()
        val totals = mutableMapOf<Int, Long>()
        stats.use {
            val bucket = NetworkStats.Bucket()
            while (it.hasNextBucket()) {
                it.getNextBucket(bucket)
                totals[bucket.uid] = (totals[bucket.uid] ?: 0L) + bucket.rxBytes + bucket.txBytes
            }
        }
        return totals
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun updateRangeTabs() {
        rangeTabs.forEachIndexed { index, tab -> TopFilterUi.selected(tab, NetworkRange.entries[index] == selectedRange) }
    }
}
