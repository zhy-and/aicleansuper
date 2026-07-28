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
        private val cornerPx = binding.root.resources.displayMetrics.density * 16f

        fun bind(item: DisplayMediaItem) {
            binding.tvTitle.text = item.title
            binding.tvSize.text = item.sizeText
            binding.btnSelect.isVisible = item.isSelectable
            binding.tvVideoBadge.isVisible = item.isVideo
            binding.selectionScrim.isVisible = item.isSelected

            if (item.isSelected) {
                binding.imageSelect.setBackgroundResource(R.drawable.bg_select_checkbox_checked)
                binding.imageSelect.setImageResource(R.drawable.ic_check)
                binding.imageSelect.imageTintList =
                    ContextCompat.getColorStateList(binding.root.context, R.color.white)
            } else {
                binding.imageSelect.setBackgroundResource(R.drawable.bg_select_checkbox)
                binding.imageSelect.setImageResource(R.drawable.ic_circle_outline)
                binding.imageSelect.imageTintList =
                    ContextCompat.getColorStateList(binding.root.context, R.color.white)
            }

            binding.imageThumb.load(item.thumbnailUri) {
                crossfade(true)
                placeholder(R.drawable.ic_media_placeholder)
                error(R.drawable.ic_media_placeholder)
                size(480)
                transformations(RoundedCornersTransformation(cornerPx))
                if (item.isVideo) {
                    decoderFactory(VideoFrameDecoder.Factory())
                }
            }

            binding.imageThumb.setOnClickListener { onPreviewClick(item) }
            binding.btnSelect.setOnClickListener { onSelectionClick(item) }
            binding.cardRoot.setOnClickListener(null)
            binding.cardRoot.isClickable = false
            binding.cardRoot.isFocusable = false
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
