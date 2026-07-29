package com.aetherquorion.cleansuperai.ui.contacts

import android.content.Intent
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.data.model.DuplicateContactGroup
import com.aetherquorion.cleansuperai.databinding.ItemContactGroupBinding

class DuplicateContactAdapter(
    private val onMergeClick: (DuplicateContactGroup) -> Unit,
    private val onViewClick: (DuplicateContactGroup) -> Unit,
) : ListAdapter<DuplicateContactGroup, DuplicateContactAdapter.ContactGroupViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactGroupViewHolder {
        val binding = ItemContactGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ContactGroupViewHolder(binding, onMergeClick, onViewClick)
    }

    override fun onBindViewHolder(holder: ContactGroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ContactGroupViewHolder(
        private val binding: ItemContactGroupBinding,
        private val onMergeClick: (DuplicateContactGroup) -> Unit,
        private val onViewClick: (DuplicateContactGroup) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(group: DuplicateContactGroup) {
            val context = binding.root.context
            binding.tvTitle.text = context.getString(
                R.string.contacts_group_title_format,
                group.primaryDisplayName,
                group.entries.size,
            )
            binding.tvNumber.text = context.getString(
                R.string.contacts_group_number_format,
                group.entries.firstOrNull()?.rawPhone ?: group.normalizedPhone,
            )
            binding.tvMembers.text = context.getString(
                R.string.contacts_group_members_format,
                group.entries.joinToString(" / ") { it.displayName.ifBlank { "未命名联系人" } },
            )
            binding.btnMerge.setOnClickListener { onMergeClick(group) }
            binding.btnView.setOnClickListener { onViewClick(group) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<DuplicateContactGroup>() {
        override fun areItemsTheSame(oldItem: DuplicateContactGroup, newItem: DuplicateContactGroup): Boolean {
            return oldItem.groupKey == newItem.groupKey
        }

        override fun areContentsTheSame(oldItem: DuplicateContactGroup, newItem: DuplicateContactGroup): Boolean {
            return oldItem == newItem
        }
    }
}
