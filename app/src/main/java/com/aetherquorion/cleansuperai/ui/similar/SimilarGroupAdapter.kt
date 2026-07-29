package com.aetherquorion.cleansuperai.ui.similar

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.data.model.SimilarPhotoGroup
import com.aetherquorion.cleansuperai.data.model.SimilarPhotoItem
import com.aetherquorion.cleansuperai.databinding.ItemSimilarGroupBinding
import com.aetherquorion.cleansuperai.databinding.ItemSimilarThumbBinding

class SimilarGroupAdapter(
    private val selectedIds: Set<Long>,
    private val onPreview: (SimilarPhotoItem) -> Unit,
    private val onToggle: (SimilarPhotoItem) -> Unit,
) : ListAdapter<SimilarPhotoGroup, SimilarGroupAdapter.SimilarGroupViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimilarGroupViewHolder {
        val binding = ItemSimilarGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return SimilarGroupViewHolder(binding, selectedIds, onPreview, onToggle)
    }

    override fun onBindViewHolder(holder: SimilarGroupViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    class SimilarGroupViewHolder(
        private val binding: ItemSimilarGroupBinding,
        private val selectedIds: Set<Long>,
        private val onPreview: (SimilarPhotoItem) -> Unit,
        private val onToggle: (SimilarPhotoItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        private val cornerPx = binding.root.resources.displayMetrics.density * 12f

        fun bind(group: SimilarPhotoGroup, position: Int) {
            val context = binding.root.context
            val reclaimText = Formatter.formatFileSize(context, group.estimatedReclaimBytes)
            val keep = group.recommendedKeep

            binding.tvGroupTitle.text = context.getString(
                R.string.similar_group_card_title_format,
                position,
                group.bucketName,
            )
            binding.tvGroupSubtitle.text = context.getString(
                R.string.similar_group_card_subtitle_format,
                group.items.size,
                reclaimText,
            )
            val keepMeta = if (keep.durationMs > 0L) {
                val seconds = (keep.durationMs / 1000L).coerceAtLeast(0L)
                val minutes = seconds / 60L
                val remSeconds = seconds % 60L
                val durationText = String.format("%d:%02d", minutes, remSeconds)
                "$durationText · ${Formatter.formatFileSize(context, keep.sizeBytes)}"
            } else {
                "${keep.width}×${keep.height} · ${Formatter.formatFileSize(context, keep.sizeBytes)}"
            }
            binding.tvKeepValue.text = context.getString(
                R.string.similar_keep_recommend_format,
                keep.displayName,
                keepMeta,
            )
            binding.imageKeep.load(keep.contentUri) {
                crossfade(true)
                placeholder(R.drawable.ic_media_placeholder)
                error(R.drawable.ic_media_placeholder)
                size(192)
                transformations(RoundedCornersTransformation(cornerPx))
            }
            binding.imageKeep.setOnClickListener { onPreview(keep) }
            binding.keepRow.setOnClickListener { onPreview(keep) }

            val others = group.items.filterNot { it.id == group.recommendedKeepId }
            binding.othersContainer.removeAllViews()
            val inflater = LayoutInflater.from(context)
            others.forEach { item ->
                val thumbBinding = ItemSimilarThumbBinding.inflate(
                    inflater,
                    binding.othersContainer,
                    false,
                )
                thumbBinding.imageThumb.load(item.contentUri) {
                    crossfade(true)
                    placeholder(R.drawable.ic_media_placeholder)
                    error(R.drawable.ic_media_placeholder)
                    size(192)
                    transformations(RoundedCornersTransformation(cornerPx))
                }
                val selected = selectedIds.contains(item.id)
                if (selected) {
                    thumbBinding.imageSelect.setBackgroundResource(R.drawable.bg_select_checkbox_checked)
                    thumbBinding.imageSelect.setImageResource(R.drawable.ic_check)
                } else {
                    thumbBinding.imageSelect.setBackgroundResource(R.drawable.bg_select_checkbox)
                    thumbBinding.imageSelect.setImageResource(R.drawable.ic_circle_outline)
                }
                thumbBinding.imageSelect.imageTintList =
                    ContextCompat.getColorStateList(context, R.color.white)
                thumbBinding.imageThumb.setOnClickListener { onPreview(item) }
                thumbBinding.btnSelect.setOnClickListener { onToggle(item) }
                binding.othersContainer.addView(thumbBinding.root)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SimilarPhotoGroup>() {
        override fun areItemsTheSame(oldItem: SimilarPhotoGroup, newItem: SimilarPhotoGroup): Boolean {
            return oldItem.groupId == newItem.groupId
        }

        override fun areContentsTheSame(oldItem: SimilarPhotoGroup, newItem: SimilarPhotoGroup): Boolean {
            return oldItem == newItem
        }
    }
}
