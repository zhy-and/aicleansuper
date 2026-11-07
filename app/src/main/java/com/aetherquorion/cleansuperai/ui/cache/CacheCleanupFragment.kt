package com.aetherquorion.cleansuperai.ui.cache

import android.app.AlertDialog
import android.os.Bundle
import android.os.Environment
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.view.Gravity
import android.graphics.Typeface
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.MainActivity
import com.aetherquorion.cleansuperai.ui.flow.CleanupResult
import com.aetherquorion.cleansuperai.ui.flow.CleanupReportFragment
import com.aetherquorion.cleansuperai.ui.common.FeatureAccess
import com.aetherquorion.cleansuperai.ui.common.FeaturePermissionCoordinator
import com.aetherquorion.cleansuperai.ui.common.ParityUi
import com.aetherquorion.cleansuperai.ui.common.TopFilterUi
import com.aetherquorion.cleansuperai.notification.CleanReminderState
import com.aetherquorion.cleansuperai.notification.ScanPendingPushState
import com.aetherquorion.cleansuperai.notification.LocalPushSelfChangeGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CacheCleanupFragment : Fragment() {
    override fun onDestroyView() {
        if (::progress.isInitialized && progress.visibility == View.VISIBLE) {
            ScanPendingPushState.markScanInterrupted(requireContext())
        }
        super.onDestroyView()
    }
    private data class Candidate(val file: File, val kind: CacheKind, val size: Long)
    private var candidates = emptyList<Candidate>()
    private val selected = mutableSetOf<String>()
    private var scanned = false
    private var asked = false
    private lateinit var status: TextView
    private lateinit var removable: TextView
    private lateinit var progress: ProgressBar
    private lateinit var list: RecyclerView
    private val expanded = CacheKind.entries.toMutableSet()
    private val listAdapter = CacheListAdapter(::toggleGroup, ::toggleGroupSelection, ::toggleEntry)
    private lateinit var delete: Button
    private val access = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (hasAccess()) scan() else status.text = "All files access is required"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(16))
            setBackgroundColor(ContextCompat.getColor(context, R.color.bg_primary))
        }
        root.addView(TopFilterUi.toolbar(context, "Cache cleanup") { parentFragmentManager.popBackStack() })
        root.addView(ImageView(context).apply {
            setImageResource(R.drawable.purgu_cache_bg)
            layoutParams = LinearLayout.LayoutParams(dp(112), dp(112)).apply {
                gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(38)
            }
        })
        removable = TextView(context).apply {
            text = "0 B"
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
        }
        root.addView(removable)
        root.addView(TextView(context).apply {
            text = "Removable cache"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        })
        status = ParityUi.status(context).apply { visibility = View.GONE }
        root.addView(status)
        progress = ParityUi.progress(context)
        root.addView(progress)
        list = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
            clipToPadding = false
            setPadding(0, dp(20), 0, dp(12))
        }
        root.addView(list)
        delete = ParityUi.button(context, ParityUi.ButtonKind.PRIMARY).apply {
            text = "Clean selected"
            layoutParams = LinearLayout.LayoutParams(-1, dp(64)).apply {
                leftMargin = dp(28); rightMargin = dp(28); bottomMargin = dp(12)
            }
            setOnClickListener { confirmDelete() }
        }
        root.addView(delete)
        return root
    }

    override fun onResume() {
        super.onResume()
        when {
            hasAccess() && !scanned -> scan()
            !hasAccess() && !asked -> {
                asked = true
                access.launch(FeaturePermissionCoordinator.settingsIntent(requireContext(), FeatureAccess.ALL_FILES))
            }
        }
    }

    private fun hasAccess() = FeaturePermissionCoordinator.hasAccess(requireContext(), FeatureAccess.ALL_FILES)

    private fun scan() {
        if (scanned) return
        scanned = true
        status.text = "Scanning…"
        progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            candidates = withContext(Dispatchers.IO) {
                val ownCache = sequenceOf(requireContext().cacheDir, requireContext().externalCacheDir)
                    .filterNotNull().filter { it.exists() }
                    .flatMap { it.walkTopDown().filter(File::isFile) }
                    .map { Candidate(it, CacheKind.APP_CACHE, it.length()) }
                val otherCaches = File(Environment.getExternalStorageDirectory(), "Android/data")
                    .listFiles().orEmpty().asSequence()
                    .map { File(it, "cache") }
                    .filter { it.isDirectory && it.canRead() }
                    .flatMap { it.walkTopDown().filter(File::isFile) }
                    .map { Candidate(it, CacheKind.APP_CACHE, it.length()) }
                val shared = Environment.getExternalStorageDirectory().walkTopDown()
                    .onEnter { it.canRead() && !it.name.startsWith('.') }
                    .filter(File::isFile)
                    .mapNotNull { file -> classifyCacheCandidate(file.name)?.let { Candidate(file, it, file.length()) } }
                (ownCache + otherCaches + shared).distinctBy { it.file.absolutePath }.toList()
            }
            progress.visibility = View.GONE
            selected.addAll(candidates.filter { it.kind == CacheKind.APP_CACHE }.map { it.file.absolutePath })
            render()
            ScanPendingPushState.markScanDoneUncleaned(requireContext())
            ScanPendingPushState.clearScanInterrupted(requireContext(), "scan_completed")
        }
    }

    private fun render() {
        listAdapter.submit(candidates.map {
            CacheUiEntry(it.file.absolutePath, it.file.name, it.size, it.kind,
                it.file.absolutePath in selected)
        }, expanded)
        status.text = getString(R.string.cache_summary, candidates.size,
            Formatter.formatShortFileSize(requireContext(), candidates.sumOf { it.size }))
        removable.text = Formatter.formatShortFileSize(requireContext(), candidates.sumOf { it.file.length() })
        delete.isEnabled = selected.isNotEmpty()
        delete.text = if (selected.isEmpty()) "Clean selected" else "Clean ${Formatter.formatShortFileSize(requireContext(), candidates.filter { it.file.absolutePath in selected }.sumOf { it.file.length() })} (${selected.size})"
    }

    private fun toggleGroup(kind: CacheKind) {
        if (!expanded.add(kind)) expanded.remove(kind)
        render()
    }

    private fun toggleGroupSelection(kind: CacheKind) {
        val group = candidates.filter { it.kind == kind }.map { it.file.absolutePath }
        if (group.all { it in selected }) selected.removeAll(group.toSet()) else selected.addAll(group)
        render()
    }

    private fun toggleEntry(path: String) {
        if (!selected.add(path)) selected.remove(path)
        listAdapter.selectionChanged(path, path in selected)
        delete.isEnabled = selected.isNotEmpty()
    }

    private fun confirmDelete() {
        com.aetherquorion.cleansuperai.analytics.PathAnalytics.track(requireContext(),
            com.aetherquorion.cleansuperai.analytics.PathEvent.CACHE_CLEAN_CLICK)
        val targets = candidates.filter { it.file.absolutePath in selected }
        val originalSizes = targets.associate { it.file.absolutePath to it.size }
        AlertDialog.Builder(requireContext())
            .setMessage("Clean ${targets.size} selected files?")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Clean") { _, _ ->
                ScanPendingPushState.clearScanDoneUncleaned(requireContext(), "cache_clean_clicked")
                viewLifecycleOwner.lifecycleScope.launch {
                    val deleted = withContext(Dispatchers.IO) {
                        LocalPushSelfChangeGuard.suppress(15_000L)
                        targets.filter { it.file.delete() }
                    }
                    val paths = deleted.map { it.file.absolutePath }.toSet()
                    candidates = candidates.filterNot { it.file.absolutePath in paths }
                    selected.removeAll(paths)
                    render()
                    status.text = "Cleaned ${deleted.size} of ${targets.size} files"
                    if (deleted.isNotEmpty()) CleanReminderState.markCleaned(requireContext())
                    com.aetherquorion.cleansuperai.analytics.PathAnalytics.track(requireContext(),
                        com.aetherquorion.cleansuperai.analytics.PathEvent.CACHE_RESULT_PAGE)
                    (activity as? MainActivity)?.openDetail(CleanupReportFragment.newInstance(
                        "Cache cleanup", CleanupResult(targets.size, deleted.size,
                            deleted.sumOf { originalSizes[it.file.absolutePath] ?: 0L })
                    ), "cleanup_report")
                }
            }.show()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
