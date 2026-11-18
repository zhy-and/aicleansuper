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
import com.aetherquorion.cleansuperai.ui.common.inlineAdHost
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
    private data class Candidate(val file: File, val kind: CacheKind, val size: Long,
        val name: String = file.name, val packageName: String? = null,
        val files: List<File> = listOf(file))
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
        if (hasAccess()) scan() else status.setText(R.string.cache_access_required)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(16))
            setBackgroundColor(ContextCompat.getColor(context, R.color.bg_primary))
        }
        root.addView(TopFilterUi.toolbar(context, getString(R.string.home_cache_cleanup)) { parentFragmentManager.popBackStack() })
        root.addView(inlineAdHost(context))
        root.addView(ImageView(context).apply {
            setImageResource(R.drawable.cache_bg)
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
            setText(R.string.cache_removable)
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
            setText(R.string.cache_clean_selected)
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
        status.setText(R.string.scan_button_running)
        progress.visibility = View.VISIBLE
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            candidates = withContext(Dispatchers.IO) {
                val ownFiles = sequenceOf(context.cacheDir, context.externalCacheDir)
                    .filterNotNull().filter(File::isDirectory)
                    .flatMap { it.walkTopDown().filter(File::isFile) }.distinctBy(File::getAbsolutePath).toList()
                val ownCache = if (ownFiles.isEmpty()) emptyList() else listOf(Candidate(
                    context.cacheDir, CacheKind.APP_CACHE, ownFiles.sumOf(File::length),
                    context.applicationInfo.loadLabel(context.packageManager).toString(),
                    context.packageName, ownFiles))
                val otherCaches = File(Environment.getExternalStorageDirectory(), "Android/data")
                    .listFiles().orEmpty().asSequence()
                    .filter(File::isDirectory)
                    .mapNotNull { appDir ->
                        val cacheDir = File(appDir, "cache")
                        if (!cacheDir.isDirectory || !cacheDir.canRead()) return@mapNotNull null
                        val appInfo = runCatching { context.packageManager.getApplicationInfo(appDir.name, 0) }.getOrNull()
                            ?: return@mapNotNull null
                        val files = cacheDir.walkTopDown().filter(File::isFile).toList()
                        if (files.isEmpty() || appDir.name == context.packageName) return@mapNotNull null
                        Candidate(cacheDir, CacheKind.APP_CACHE, files.sumOf(File::length),
                            appInfo.loadLabel(context.packageManager).toString(), appDir.name, files)
                    }
                val shared = Environment.getExternalStorageDirectory().walkTopDown()
                    .onEnter { it.canRead() && !it.name.startsWith('.') &&
                        it.absolutePath != File(Environment.getExternalStorageDirectory(), "Android/data").absolutePath }
                    .filter(File::isFile)
                    .mapNotNull { file -> classifyCacheCandidate(file.name)?.let { Candidate(file, it, file.length()) } }
                (ownCache.asSequence() + otherCaches + shared).distinctBy { it.file.absolutePath }
                    .sortedWith(compareBy<Candidate> { it.kind }.thenByDescending { it.size }).toList()
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
            CacheUiEntry(it.file.absolutePath, it.name, it.size, it.kind,
                it.file.absolutePath in selected, it.packageName)
        }, expanded)
        status.text = getString(R.string.cache_summary, candidates.size,
            Formatter.formatShortFileSize(requireContext(), candidates.sumOf { it.size }))
        removable.text = Formatter.formatShortFileSize(requireContext(), candidates.sumOf { it.size })
        delete.isEnabled = selected.isNotEmpty()
        delete.text = if (selected.isEmpty()) getString(R.string.cache_clean_selected) else
            getString(R.string.cache_clean_selected_summary,
                Formatter.formatShortFileSize(requireContext(), candidates.filter { it.file.absolutePath in selected }.sumOf { it.size }), selected.size)
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
        render()
    }

    private fun confirmDelete() {
        com.aetherquorion.cleansuperai.analytics.PathAnalytics.track(requireContext(),
            com.aetherquorion.cleansuperai.analytics.PathEvent.CACHE_CLEAN_CLICK)
        val targets = candidates.filter { it.file.absolutePath in selected }
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.cache_confirm_clean, targets.size))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.cache_clean_action) { _, _ ->
                ScanPendingPushState.clearScanDoneUncleaned(requireContext(), "cache_clean_clicked")
                viewLifecycleOwner.lifecycleScope.launch {
                    val deleted = withContext(Dispatchers.IO) {
                        LocalPushSelfChangeGuard.suppress(15_000L)
                        targets.map { target ->
                            target.files.forEach(File::delete)
                            target.file.absolutePath to target.files.filter(File::exists)
                        }
                    }
                    val remainingByPath = deleted.toMap()
                    val removed = targets.count { remainingByPath[it.file.absolutePath].isNullOrEmpty() }
                    val freedBytes = targets.sumOf { target ->
                        target.size - remainingByPath[target.file.absolutePath].orEmpty().sumOf(File::length)
                    }
                    candidates = candidates.mapNotNull { candidate ->
                        val remaining = remainingByPath[candidate.file.absolutePath] ?: return@mapNotNull candidate
                        if (remaining.isEmpty()) null else candidate.copy(files = remaining, size = remaining.sumOf(File::length))
                    }
                    selected.removeAll(targets.map { it.file.absolutePath }.toSet())
                    render()
                    status.text = getString(R.string.cache_clean_result, removed, targets.size)
                    if (freedBytes > 0L) CleanReminderState.markCleaned(requireContext())
                    com.aetherquorion.cleansuperai.analytics.PathAnalytics.track(requireContext(),
                        com.aetherquorion.cleansuperai.analytics.PathEvent.CACHE_RESULT_PAGE)
                    if (freedBytes > 0L) (activity as? MainActivity)?.openDetail(CleanupReportFragment.newInstance(
                        getString(R.string.home_cache_cleanup), CleanupResult(targets.size, removed, freedBytes)
                    ), "cleanup_report")
                }
            }.show()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
