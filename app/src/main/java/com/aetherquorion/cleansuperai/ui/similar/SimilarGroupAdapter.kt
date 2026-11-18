package com.aetherquorion.cleansuperai.ui.similar

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
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

    fun refreshSelection() {
        attachedRecyclerView?.let { list ->
            for (index in 0 until list.childCount) {
                (list.getChildViewHolder(list.getChildAt(index)) as? SimilarGroupViewHolder)?.refreshSelection()
            }
        }
    }

    private var attachedRecyclerView: RecyclerView? = null
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedRecyclerView = recyclerView
    }
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        attachedRecyclerView = null
        super.onDetachedFromRecyclerView(recyclerView)
    }

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

    override fun onBindViewHolder(holder: SimilarGroupViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(SELECTION_PAYLOAD)) holder.refreshSelection()
        else super.onBindViewHolder(holder, position, payloads)
    }

    class SimilarGroupViewHolder(
        private val binding: ItemSimilarGroupBinding,
        private val selectedIds: Set<Long>,
        private val onPreview: (SimilarPhotoItem) -> Unit,
        private val onToggle: (SimilarPhotoItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        private val cornerPx = binding.root.resources.displayMetrics.density * 12f
        private val thumbnails = ThumbnailAdapter(selectedIds, onPreview, onToggle)

        init {
            binding.othersList.layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.HORIZONTAL, false)
            binding.othersList.setHasFixedSize(true)
            binding.othersList.adapter = thumbnails
        }

        fun bind(group: SimilarPhotoGroup, position: Int) {
            val context = binding.root.context
            val reclaimText = Formatter.formatFileSize(context, group.estimatedReclaimBytes)
            val keep = group.recommendedKeep

            binding.tvGroupTitle.text = context.getString(R.string.similar_group_number_format, position)
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
            thumbnails.submitList(others)
        }

        fun refreshSelection() = thumbnails.refreshSelection(binding.othersList)
    }

    private class ThumbnailAdapter(
        private val selectedIds: Set<Long>,
        private val onPreview: (SimilarPhotoItem) -> Unit,
        private val onToggle: (SimilarPhotoItem) -> Unit,
    ) : ListAdapter<SimilarPhotoItem, ThumbnailAdapter.Holder>(object : DiffUtil.ItemCallback<SimilarPhotoItem>() {
        override fun areItemsTheSame(oldItem: SimilarPhotoItem, newItem: SimilarPhotoItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SimilarPhotoItem, newItem: SimilarPhotoItem) = oldItem == newItem
    }) {
        fun refreshSelection(list: RecyclerView) {
            for (index in 0 until list.childCount) {
                val holder = list.getChildViewHolder(list.getChildAt(index)) as? Holder ?: continue
                val position = holder.bindingAdapterPosition
                if (position in 0 until itemCount) holder.bindSelection(getItem(position), selectedIds)
            }
        }

        override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
            if (payloads.contains(SELECTION_PAYLOAD)) holder.bindSelection(getItem(position), selectedIds)
            else super.onBindViewHolder(holder, position, payloads)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
            ItemSimilarThumbBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(getItem(position), selectedIds, onPreview, onToggle)
        }

        class Holder(private val binding: ItemSimilarThumbBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: SimilarPhotoItem, selectedIds: Set<Long>, onPreview: (SimilarPhotoItem) -> Unit,
                     onToggle: (SimilarPhotoItem) -> Unit) {
                binding.imageThumb.load(item.contentUri) {
                    crossfade(false)
                    placeholder(R.drawable.ic_media_placeholder)
                    error(R.drawable.ic_media_placeholder)
                    size(192)
                }
                bindSelection(item, selectedIds)
                binding.imageThumb.setOnClickListener { onPreview(item) }
                binding.btnSelect.setOnClickListener { onToggle(item) }
            }

            fun bindSelection(item: SimilarPhotoItem, selectedIds: Set<Long>) {
                binding.imageSelect.setBackgroundResource(
                    if (item.id in selectedIds) R.drawable.bg_select_checkbox_checked else R.drawable.bg_select_checkbox,
                )
                binding.imageSelect.setImageResource(if (item.id in selectedIds) R.drawable.ic_check else R.drawable.ic_circle_outline)
                binding.imageSelect.imageTintList = ContextCompat.getColorStateList(binding.root.context, R.color.white)
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

    private companion object { const val SELECTION_PAYLOAD = "selection" }
}
