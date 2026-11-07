package com.aetherquorion.cleansuperai.ui.largefiles

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.databinding.ItemAudioCleanupBinding

class LargeFileListAdapter(
    private val onOpen: (LargeFileEntry) -> Unit,
    private val onSelect: (LargeFileEntry) -> Unit,
) : RecyclerView.Adapter<LargeFileListAdapter.Holder>() {
    private var entries = emptyList<LargeFileEntry>()
    private var selected = emptySet<String>()

    fun submit(value: List<LargeFileEntry>, selectedPaths: Set<String>) {
        entries = value
        selected = selectedPaths
        notifyDataSetChanged()
    }
    fun selectionChanged(path: String) {
        val index = entries.indexOfFirst { it.path == path }
        if (index >= 0) notifyItemChanged(index)
    }
    override fun getItemCount() = entries.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemAudioCleanupBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(entries[position])

    inner class Holder(private val binding: ItemAudioCleanupBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LargeFileEntry) {
            val context = binding.root.context
            binding.fileIcon.setImageResource(when (item.type) {
                LargeFileType.IMAGE -> R.drawable.ic_image
                LargeFileType.VIDEO -> R.drawable.ic_video_library
                LargeFileType.AUDIO -> R.drawable.ic_volume
                LargeFileType.APK -> R.drawable.ic_android
                LargeFileType.ARCHIVE -> R.drawable.ic_save
                LargeFileType.DOCUMENT -> R.drawable.ic_list
                else -> R.drawable.ic_clean
            })
            binding.tvTitle.text = item.name
            binding.tvMeta.text = Formatter.formatShortFileSize(context, item.sizeBytes)
            val checked = item.path in selected
            binding.imageSelect.setBackgroundResource(if (checked)
                R.drawable.bg_select_checkbox_checked else R.drawable.bg_select_checkbox)
            binding.imageSelect.setImageResource(if (checked) R.drawable.ic_check else R.drawable.ic_circle_outline)
            binding.imageSelect.imageTintList = ContextCompat.getColorStateList(context, R.color.white)
            binding.cardRoot.setOnClickListener { onOpen(item) }
            binding.btnSelect.setOnClickListener { onSelect(item) }
        }
    }
}
