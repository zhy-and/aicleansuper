package com.aetherquorion.cleansuperai.ui.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.databinding.ItemAudioCleanupBinding
import com.aetherquorion.cleansuperai.ui.clean.detail.DisplayMediaItem

class AudioCleanupAdapter(
    private val onPlay: (DisplayMediaItem) -> Unit,
    private val onSelect: (DisplayMediaItem) -> Unit,
) : ListAdapter<DisplayMediaItem, AudioCleanupAdapter.Holder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemAudioCleanupBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val binding: ItemAudioCleanupBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DisplayMediaItem) {
            binding.tvTitle.text = item.title
            binding.tvMeta.text = listOf(item.metaPrimary, item.sizeText).filter { it.isNotBlank() }.joinToString(" · ")
            binding.imageSelect.setBackgroundResource(if (item.isSelected)
                R.drawable.bg_select_checkbox_checked else R.drawable.bg_select_checkbox)
            binding.imageSelect.setImageResource(if (item.isSelected) R.drawable.ic_check else R.drawable.ic_circle_outline)
            binding.imageSelect.imageTintList =
                ContextCompat.getColorStateList(binding.root.context, R.color.white)
            binding.cardRoot.setOnClickListener { onPlay(item) }
            binding.btnSelect.setOnClickListener { onSelect(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<DisplayMediaItem>() {
        override fun areItemsTheSame(oldItem: DisplayMediaItem, newItem: DisplayMediaItem) =
            oldItem.stableId == newItem.stableId
        override fun areContentsTheSame(oldItem: DisplayMediaItem, newItem: DisplayMediaItem) =
            oldItem == newItem
    }
}
