package com.aetherquorion.cleansuperai.ui.largefiles

import android.app.AlertDialog
import android.content.Intent
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
import android.widget.PopupWindow
import android.view.Gravity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LargeFilesFragment : Fragment() {
    private var entries = emptyList<LargeFileEntry>()
    private val selected = mutableSetOf<String>()
    private var loaded = false
    private var accessRequested = false
    private lateinit var list: RecyclerView
    private val listAdapter = LargeFileListAdapter(::showDetails, ::toggleEntry)
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private val filterSelections = intArrayOf(0, 0, 0)
    private val filterOptions = listOf(
        LargeFileType.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) },
        listOf("10 MB", "50 MB", "100 MB", "500 MB", "1 GB"),
        listOf("All time", "1 week", "1 month", "3 months", "6 months", "1 year"),
    )
    private lateinit var delete: Button
    private lateinit var selectAll: Button
    private val accessLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (hasAccess()) scan() else status.text = "All files access is required"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(16))
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.bg_primary))
        }
        root.addView(TopFilterUi.toolbar(ctx, "Large files") { parentFragmentManager.popBackStack() })
        val filters = TopFilterUi.bar(ctx, 48)
        listOf("Type", "Size", "Time").forEachIndexed { index, title ->
            val tab = TopFilterUi.label(ctx, "${filterOptions[index][0]}  ▾")
            filters.addView(tab, LinearLayout.LayoutParams(0, -1, 1f))
            tab.contentDescription = "$title filter: ${filterOptions[index][0]}"
            tab.setOnClickListener {
                val menu = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(12), dp(16), dp(16))
                    background = TopFilterUi.surface(ctx)
                }
                menu.addView(TextView(ctx).apply {
                    text = title
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                })
                val popup = PopupWindow(menu, filters.width, -2, true).apply {
                    isOutsideTouchable = true
                    elevation = dp(12).toFloat()
                    setBackgroundDrawable(TopFilterUi.surface(ctx))
                }
                filterOptions[index].forEachIndexed { choice, option ->
                    menu.addView(TopFilterUi.label(ctx, if (choice == filterSelections[index]) "$option  ✓" else option).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(12), 0, dp(12), 0)
                        setTextColor(ContextCompat.getColor(ctx, if (choice == filterSelections[index]) R.color.accent_primary else R.color.text_primary))
                        setOnClickListener {
                        filterSelections[index] = choice
                        tab.text = "${filterOptions[index][choice]}  ▾"
                        tab.contentDescription = "$title filter: ${filterOptions[index][choice]}"
                        popup.dismiss()
                        render()
                        }
                    })
                }
                popup.showAsDropDown(filters)
            }
        }
        root.addView(filters)
        val summary = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, dp(48))
        }
        status = ParityUi.status(ctx).apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        summary.addView(status)
        selectAll = ParityUi.button(ctx).apply {
            text = "Select all"
            textSize = 13f
            minHeight = dp(34)
            setPadding(dp(12), 0, dp(12), 0)
            layoutParams = LinearLayout.LayoutParams(-2, dp(34))
            setOnClickListener { selectAllShown() }
        }
        summary.addView(selectAll)
        root.addView(summary)
        progress = ParityUi.progress(ctx)
        root.addView(progress)
        list = RecyclerView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            layoutManager = LinearLayoutManager(ctx)
            adapter = listAdapter
            clipToPadding = false
            setPadding(dp(8), 0, dp(8), dp(12))
        }
        root.addView(list)
        delete = ParityUi.button(ctx, ParityUi.ButtonKind.PRIMARY).apply {
            text = "Delete selected"
            layoutParams = LinearLayout.LayoutParams(-1, dp(64)).apply {
                leftMargin = dp(8); rightMargin = dp(8); bottomMargin = dp(12)
            }
            setOnClickListener { confirmDelete() }
        }
        root.addView(delete)
        return root
    }

    override fun onResume() {
        super.onResume()
        if (hasAccess() && !loaded) scan()
        else if (!hasAccess() && !accessRequested) {
            accessRequested = true
            status.text = "All files access is required"
            accessLauncher.launch(FeaturePermissionCoordinator.settingsIntent(requireContext(), FeatureAccess.ALL_FILES))
        }
    }

    private fun hasAccess() = FeaturePermissionCoordinator.hasAccess(requireContext(), FeatureAccess.ALL_FILES)

    private fun scan() {
        if (loaded || !isAdded) return
        loaded = true
        progress.visibility = View.VISIBLE
        status.text = "Scanning files…"
        viewLifecycleOwner.lifecycleScope.launch {
            entries = withContext(Dispatchers.IO) {
                Environment.getExternalStorageDirectory().walkTopDown()
                    .onEnter { it.canRead() && !it.name.startsWith('.') }
                    .filter { it.isFile && it.length() >= 10L * 1024 * 1024 }
                    .map { LargeFileEntry(it.absolutePath, it.name, it.length(), it.lastModified()) }
                    .sortedByDescending { it.sizeBytes }
                    .toList()
            }
            selected.addAll(entries.map { it.path })
            progress.visibility = View.GONE
            render()
        }
    }

    private fun render() {
        if (!::list.isInitialized) return
        val sizes = longArrayOf(10, 50, 100, 500, 1024).map { it * 1024 * 1024 }
        val days = longArrayOf(0, 7, 30, 90, 180, 365)
        val now = System.currentTimeMillis()
        val visible = entries.filter {
            it.matches(LargeFileType.entries[filterSelections[0]], sizes[filterSelections[1]],
                days[filterSelections[2]].takeIf { day -> day > 0 }?.times(24 * 60 * 60 * 1000), now)
        }
        status.text = "${visible.size} files · ${Formatter.formatShortFileSize(requireContext(), visible.sumOf { it.sizeBytes })}"
        selectAll.text = if (visible.isNotEmpty() && visible.all { it.path in selected }) "Deselect all" else "Select all"
        listAdapter.submit(visible, selected)
        delete.isEnabled = selected.isNotEmpty()
        delete.text = "Delete ${Formatter.formatShortFileSize(requireContext(), entries.filter { it.path in selected }.sumOf { it.sizeBytes })} (${selected.size})"
    }

    private fun toggleEntry(item: LargeFileEntry) {
        if (!selected.add(item.path)) selected.remove(item.path)
        listAdapter.selectionChanged(item.path)
        delete.isEnabled = selected.isNotEmpty()
        selectAll.text = if (listAdapterVisiblePaths().all { it in selected }) "Deselect all shown" else "Select all shown"
    }

    private fun listAdapterVisiblePaths(): List<String> {
        val sizes = longArrayOf(10, 50, 100, 500, 1024).map { it * 1024 * 1024 }
        val days = longArrayOf(0, 7, 30, 90, 180, 365)
        val now = System.currentTimeMillis()
        return entries.asSequence().filter {
            it.matches(LargeFileType.entries[filterSelections[0]], sizes[filterSelections[1]],
                days[filterSelections[2]].takeIf { day -> day > 0 }?.times(24 * 60 * 60 * 1000), now)
        }.map { it.path }.toList()
    }

    private fun selectAllShown() {
        val sizes = longArrayOf(10, 50, 100, 500, 1024).map { it * 1024 * 1024 }
        val days = longArrayOf(0, 7, 30, 90, 180, 365)
        val now = System.currentTimeMillis()
        val visiblePaths = entries.filter {
            it.matches(LargeFileType.entries[filterSelections[0]], sizes[filterSelections[1]],
                days[filterSelections[2]].takeIf { day -> day > 0 }?.times(24 * 60 * 60 * 1000), now)
        }.map { it.path }
        if (visiblePaths.all { it in selected }) selected.removeAll(visiblePaths.toSet())
        else selected.addAll(visiblePaths)
        render()
    }

    private fun showDetails(item: LargeFileEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle(item.name)
            .setMessage("${item.type} · ${Formatter.formatShortFileSize(requireContext(), item.sizeBytes)}\n${item.path}")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Open") { _, _ ->
                val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.files", File(item.path))
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, requireContext().contentResolver.getType(uri) ?: "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching { startActivity(intent) }
            }.show()
    }

    private fun confirmDelete() {
        val targets = entries.filter { it.path in selected }
        if (targets.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setMessage("Delete ${targets.size} selected files?")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val deleted = withContext(Dispatchers.IO) { targets.filter { File(it.path).delete() } }
                    val paths = deleted.map { it.path }.toSet()
                    entries = entries.filterNot { it.path in paths }
                    selected.removeAll(paths)
                    render()
                    status.text = "Deleted ${deleted.size} of ${targets.size} files"
                    if (deleted.isNotEmpty()) {
                        (activity as? MainActivity)?.openDetail(CleanupReportFragment.newInstance(
                            "Large files", CleanupResult(targets.size, deleted.size, deleted.sumOf { it.sizeBytes })
                        ), "cleanup_report")
                    }
                }
            }.show()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
