package com.aetherquorion.cleansuperai.ui.cache

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.databinding.ItemAudioCleanupBinding
import com.aetherquorion.cleansuperai.databinding.ItemMediaSectionBinding

data class CacheUiEntry(val path: String, val name: String, val size: Long, val kind: CacheKind, val selected: Boolean)

class CacheListAdapter(
    private val onGroupExpand: (CacheKind) -> Unit,
    private val onGroupSelect: (CacheKind) -> Unit,
    private val onEntrySelect: (String) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private sealed interface Row {
        data class Header(val kind: CacheKind, val entries: List<CacheUiEntry>, val expanded: Boolean) : Row
        data class File(val entry: CacheUiEntry) : Row
    }
    private var rows: List<Row> = emptyList()

    fun submit(entries: List<CacheUiEntry>, expanded: Set<CacheKind>) {
        rows = buildList {
            CacheKind.entries.forEach { kind ->
                val group = entries.filter { it.kind == kind }
                if (group.isNotEmpty()) {
                    add(Row.Header(kind, group, kind in expanded))
                    if (kind in expanded) group.forEach { add(Row.File(it)) }
                }
            }
        }
        notifyDataSetChanged()
    }

    fun selectionChanged(path: String, selected: Boolean) {
        val fileIndex = rows.indexOfFirst { row -> row is Row.File && row.entry.path == path }
        if (fileIndex < 0) return
        val old = rows[fileIndex] as Row.File
        rows = rows.toMutableList().also { it[fileIndex] = Row.File(old.entry.copy(selected = selected)) }
        notifyItemChanged(fileIndex)
        val headerIndex = rows.indexOfLast { it is Row.Header && it.kind == old.entry.kind }
        if (headerIndex >= 0) {
            val header = rows[headerIndex] as Row.Header
            rows = rows.toMutableList().also { list ->
                list[headerIndex] = header.copy(entries = header.entries.map { entry ->
                    if (entry.path == path) entry.copy(selected = selected) else entry
                })
            }
            notifyItemChanged(headerIndex)
        }
    }

    override fun getItemCount() = rows.size
    override fun getItemViewType(position: Int) = if (rows[position] is Row.Header) 0 else 1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) HeaderHolder(ItemMediaSectionBinding.inflate(inflater, parent, false))
        else FileHolder(ItemAudioCleanupBinding.inflate(inflater, parent, false))
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as HeaderHolder).bind(row)
            is Row.File -> (holder as FileHolder).bind(row.entry)
        }
    }

    private inner class HeaderHolder(private val binding: ItemMediaSectionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: Row.Header) {
            val context = binding.root.context
            binding.tvSectionTitle.text = context.getString(when (row.kind) {
                CacheKind.APP_CACHE -> R.string.cache_group_app
                CacheKind.OBSOLETE_APK -> R.string.cache_group_apk
                CacheKind.RESIDUAL -> R.string.cache_group_residual
            }) + if (row.expanded) "  ▾" else "  ▸"
            binding.tvSectionSummary.text = context.getString(R.string.media_group_summary,
                row.entries.size, Formatter.formatShortFileSize(context, row.entries.sumOf { it.size }))
            binding.btnSectionSelect.setText(if (row.entries.all { it.selected })
                R.string.clear_selected_action else R.string.select_all_action)
            binding.sectionRoot.setOnClickListener { onGroupExpand(row.kind) }
            binding.btnSectionSelect.setOnClickListener { onGroupSelect(row.kind) }
        }
    }

    private inner class FileHolder(private val binding: ItemAudioCleanupBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: CacheUiEntry) {
            val context = binding.root.context
            binding.fileIcon.setImageResource(when (entry.kind) {
                CacheKind.APP_CACHE -> R.drawable.ic_clean
                CacheKind.OBSOLETE_APK -> R.drawable.ic_android
                CacheKind.RESIDUAL -> R.drawable.ic_list
            })
            binding.tvTitle.text = entry.name
            binding.tvMeta.text = Formatter.formatShortFileSize(context, entry.size)
            binding.imageSelect.setBackgroundResource(if (entry.selected)
                R.drawable.bg_select_checkbox_checked else R.drawable.bg_select_checkbox)
            binding.imageSelect.setImageResource(if (entry.selected) R.drawable.ic_check else R.drawable.ic_circle_outline)
            binding.imageSelect.imageTintList = ContextCompat.getColorStateList(context, R.color.white)
            binding.cardRoot.setOnClickListener { onEntrySelect(entry.path) }
            binding.btnSelect.setOnClickListener { onEntrySelect(entry.path) }
        }
    }
}
