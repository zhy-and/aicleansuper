package com.example.cleansuperai.ui.clean.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.decode.VideoFrameDecoder
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.cleansuperai.R
import com.example.cleansuperai.databinding.ItemMediaCleanupBinding

class MediaCleanupAdapter(
    private val onPreviewClick: (DisplayMediaItem) -> Unit,
    private val onSelectionClick: (DisplayMediaItem) -> Unit = {},
) : ListAdapter<DisplayMediaItem, MediaCleanupAdapter.MediaCleanupViewHolder>(DiffCallback) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaCleanupViewHolder {
        val binding = ItemMediaCleanupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return MediaCleanupViewHolder(binding, onPreviewClick, onSelectionClick)
    }

    override fun onBindViewHolder(holder: MediaCleanupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long = getItem(position).stableId

    class MediaCleanupViewHolder(
        private val binding: ItemMediaCleanupBinding,
        private val onPreviewClick: (DisplayMediaItem) -> Unit,
        private val onSelectionClick: (DisplayMediaItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        private val cornerPx = binding.root.resources.displayMetrics.density * 12f

        fun bind(item: DisplayMediaItem) {
            binding.tvTitle.text = item.title
            binding.tvMetaPrimary.text = item.metaPrimary
            binding.tvMetaSecondary.text = item.metaSecondary
            binding.tvSize.text = item.sizeText
            binding.checkSelected.isVisible = item.isSelectable
            binding.checkSelected.isChecked = item.isSelected
            binding.tvVideoBadge.isVisible = item.isVideo

            binding.imageThumb.load(item.thumbnailUri) {
                crossfade(true)
                placeholder(R.drawable.ic_media_placeholder)
                error(R.drawable.ic_media_placeholder)
                size(168)
                transformations(RoundedCornersTransformation(cornerPx))
                if (item.isVideo) {
                    decoderFactory(VideoFrameDecoder.Factory())
                }
            }

            val context = binding.root.context
            val selectedStroke = ContextCompat.getColor(context, R.color.accent_primary)
            val defaultStroke = ContextCompat.getColor(context, R.color.stroke_light)
            val selectedCard = ContextCompat.getColor(context, R.color.accent_soft)
            val defaultCard = ContextCompat.getColor(context, R.color.surface_card)

            binding.cardRoot.strokeColor = if (item.isSelected) selectedStroke else defaultStroke
            binding.cardRoot.setCardBackgroundColor(if (item.isSelected) selectedCard else defaultCard)
            binding.imageThumb.setOnClickListener { onPreviewClick(item) }
            binding.cardRoot.setOnClickListener {
                if (item.isSelectable) {
                    onSelectionClick(item)
                } else {
                    onPreviewClick(item)
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<DisplayMediaItem>() {
        override fun areItemsTheSame(oldItem: DisplayMediaItem, newItem: DisplayMediaItem): Boolean {
            return oldItem.stableId == newItem.stableId
        }

        override fun areContentsTheSame(oldItem: DisplayMediaItem, newItem: DisplayMediaItem): Boolean {
            return oldItem == newItem
        }
    }
}
