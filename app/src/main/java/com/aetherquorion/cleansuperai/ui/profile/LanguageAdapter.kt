package com.aetherquorion.cleansuperai.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.databinding.ItemLanguageOptionBinding

class LanguageAdapter(
    private val onClick: (AppLanguage) -> Unit,
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var items: List<AppLanguage> = emptyList()
    private var selectedTag: String? = null

    fun submitList(items: List<AppLanguage>, selectedTag: String?) {
        this.items = items
        this.selectedTag = selectedTag
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return LanguageViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(items[position], items[position].tag == selectedTag)
    }

    override fun getItemCount(): Int = items.size

    class LanguageViewHolder(
        private val binding: ItemLanguageOptionBinding,
        private val onClick: (AppLanguage) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppLanguage, selected: Boolean) {
            val context = binding.root.context
            val displayLocale = AppLanguage.effectiveLocale()
            if (item.isFollowSystem) {
                binding.tvTitle.setText(R.string.language_follow_system_title)
                binding.tvSubtitle.text = context.getString(
                    R.string.language_follow_system_subtitle_format,
                    AppLanguage.displayName(AppLanguage.systemLocale()),
                )
            } else {
                binding.tvTitle.text = item.title()
                binding.tvSubtitle.text = item.subtitle(displayLocale)
            }

            binding.tvSubtitle.isVisible = !binding.tvSubtitle.text.isNullOrBlank()
            binding.ivSelected.isVisible = selected
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
