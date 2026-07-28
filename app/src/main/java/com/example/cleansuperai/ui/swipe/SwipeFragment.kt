package com.example.cleansuperai.ui.swipe

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cleansuperai.MainActivity
import com.example.cleansuperai.R
import com.example.cleansuperai.databinding.FragmentSwipeBinding
import com.example.cleansuperai.databinding.ItemPhotoMonthBinding
import com.example.cleansuperai.ui.cleaner.PhotoMonthGrouper
import com.example.cleansuperai.ui.cleaner.PhotoRecord
import com.example.cleansuperai.ui.common.MediaPermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MediaPhoto(
    val id: Long,
    val uri: Uri,
    val dateModifiedMs: Long,
)

data class MediaMonth(
    val label: String,
    val photos: List<MediaPhoto>,
)

class SwipeFragment : Fragment() {
    private var _binding: FragmentSwipeBinding? = null
    private val binding get() = _binding!!
    private val adapter = PhotoMonthAdapter(::openMonth)
    private lateinit var reviewStore: SwipeReviewStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSwipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reviewStore = SwipeReviewStore(requireContext())
        binding.btnPremium.isVisible = false
        binding.btnSettings.setOnClickListener { (activity as? MainActivity)?.openProfile() }
        binding.recyclerMonths.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMonths.adapter = adapter
        loadPhotos()
    }

    override fun onResume() {
        super.onResume()
        if (view != null) {
            loadPhotos()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadPhotos() {
        if (!MediaPermissionHelper.hasPermissions(requireContext())) {
            binding.progressLoading.isVisible = false
            binding.tvEmpty.isVisible = true
            binding.tvEmpty.setText(R.string.swipe_grant_access_hint)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val photos = withContext(Dispatchers.IO) { queryPhotos() }
            reviewStore.pruneToExisting(photos.map(MediaPhoto::uri))
            val reviewableUris = reviewStore.filterReviewable(photos.map(MediaPhoto::uri))
                .mapTo(mutableSetOf(), Uri::toString)
            val reviewablePhotos = photos.filter { photo -> photo.uri.toString() in reviewableUris }
            val byId = reviewablePhotos.associateBy(MediaPhoto::id)
            val months = PhotoMonthGrouper.group(
                reviewablePhotos.map { PhotoRecord(it.id, it.dateModifiedMs) },
            ).map { group ->
                MediaMonth(group.label, group.photos.mapNotNull { byId[it.id] })
            }
            binding.progressLoading.isVisible = false
            binding.tvEmpty.isVisible = months.isEmpty()
            adapter.submit(months)
        }
    }

    private fun queryPhotos(): List<MediaPhoto> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
        )
        val photos = mutableListOf<MediaPhoto>()
        requireContext().contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val taken = if (cursor.isNull(takenColumn)) 0L else cursor.getLong(takenColumn)
                val modified = cursor.getLong(modifiedColumn) * 1000L
                photos += MediaPhoto(
                    id = id,
                    uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                    dateModifiedMs = taken.takeIf { it > 0 } ?: modified,
                )
            }
        }
        return photos
    }

    private fun openMonth(month: MediaMonth) {
        parentFragmentManager.commit {
            replace(R.id.fragmentContainer, PhotoDetailFragment.newInstance(month))
            addToBackStack("photo_detail")
        }
    }
}

private class PhotoMonthAdapter(
    private val onClick: (MediaMonth) -> Unit,
) : RecyclerView.Adapter<PhotoMonthAdapter.MonthViewHolder>() {
    private var items: List<MediaMonth> = emptyList()

    fun submit(newItems: List<MediaMonth>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val binding = ItemPhotoMonthBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MonthViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class MonthViewHolder(
        private val binding: ItemPhotoMonthBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MediaMonth) {
            binding.tvMonth.text = item.label
            binding.tvCount.text = binding.root.context.getString(
                R.string.month_photo_count_format,
                item.photos.size,
            )
            binding.imageCover.setImageURI(item.photos.firstOrNull()?.uri)
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
