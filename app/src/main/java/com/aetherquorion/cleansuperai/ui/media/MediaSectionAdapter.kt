package com.aetherquorion.cleansuperai.ui.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aetherquorion.cleansuperai.databinding.ItemMediaSectionBinding

class MediaSectionAdapter(
    private val onToggle: () -> Unit,
    private val onSelect: () -> Unit,
) : RecyclerView.Adapter<MediaSectionAdapter.Holder>() {
    var title = ""
    var summary = ""
    var selectLabel = ""
    var visible = false
    var expanded = true

    fun refresh() = notifyDataSetChanged()
    override fun getItemCount() = if (visible) 1 else 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemMediaSectionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind()

    inner class Holder(private val binding: ItemMediaSectionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.tvSectionTitle.text = title + if (expanded) "  ▾" else "  ▸"
            binding.tvSectionSummary.text = summary
            binding.btnSectionSelect.text = selectLabel
            binding.sectionRoot.setOnClickListener { onToggle() }
            binding.btnSectionSelect.setOnClickListener { onSelect() }
        }
    }
}
