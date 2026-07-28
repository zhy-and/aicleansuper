package com.example.cleansuperai.ui.swipe

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cleansuperai.R
import com.example.cleansuperai.databinding.FragmentPhotoDetailBinding
import com.example.cleansuperai.databinding.ItemTrashBinPhotoBinding
import kotlin.math.abs

class PhotoDetailFragment : Fragment() {
    private var _binding: FragmentPhotoDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var monthLabel: String
    private lateinit var reviewStore: SwipeReviewStore
    private var photoUris: MutableList<Uri> = mutableListOf()
    private var monthPhotoUriStrings: Set<String> = emptySet()
    private var pendingDeleteUris: List<Uri> = emptyList()

    private val trashAdapter = TrashBinAdapter(::restoreTrashItem)
    private var dragStartX = 0f
    private var dragActive = false

    private val deleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                clearReviewBin()
                Toast.makeText(requireContext(), R.string.selected_photos_deleted, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        monthLabel = requireArguments().getString(ARG_MONTH).orEmpty()
        reviewStore = SwipeReviewStore(requireContext())
        val monthUris = PhotoSelectionStore.take()
        monthPhotoUriStrings = monthUris.mapTo(mutableSetOf(), Uri::toString)
        photoUris = reviewStore.filterReviewable(monthUris).toMutableList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPhotoDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnShare.setOnClickListener { shareCurrent() }
        binding.btnKeep.setOnClickListener { animateSwipe(commitToBin = false) }
        binding.btnMoveToBin.setOnClickListener { animateSwipe(commitToBin = true) }
        binding.btnOpenTrash.setOnClickListener { openTrashBin() }
        binding.btnCloseTrash.setOnClickListener { closeTrashBin() }
        binding.trashScrim.setOnClickListener { closeTrashBin() }
        binding.btnClearPending.setOnClickListener { deletePendingPhotos() }

        binding.recyclerTrashBin.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerTrashBin.adapter = trashAdapter
        attachSwipeGesture()
        render()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun attachSwipeGesture() {
        binding.cardPhoto.setOnTouchListener { _, event ->
            if (photoUris.isEmpty()) return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartX = event.rawX
                    dragActive = true
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!dragActive) return@setOnTouchListener false
                    val deltaX = event.rawX - dragStartX
                    updateDragState(deltaX)
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                -> {
                    if (!dragActive) return@setOnTouchListener false
                    dragActive = false
                    val deltaX = event.rawX - dragStartX
                    finishDrag(deltaX)
                    true
                }

                else -> false
            }
        }
    }

    private fun updateDragState(deltaX: Float) {
        val width = binding.cardPhoto.width.coerceAtLeast(1)
        val normalized = (abs(deltaX) / width).coerceIn(0f, 1f)
        binding.cardPhoto.translationX = deltaX
        binding.cardPhoto.rotation = deltaX / 30f
        binding.cardPhoto.scaleX = 1f - (normalized * 0.04f)
        binding.cardPhoto.scaleY = 1f - (normalized * 0.04f)
        binding.btnMoveToBin.alpha = if (deltaX < 0f) 0.55f + (normalized * 0.45f) else 0.55f
        binding.btnKeep.alpha = if (deltaX > 0f) 0.55f + (normalized * 0.45f) else 0.55f
    }

    private fun finishDrag(deltaX: Float) {
        val threshold = binding.cardPhoto.width * 0.20f
        when {
            deltaX <= -threshold -> animateSwipe(commitToBin = true)
            deltaX >= threshold -> animateSwipe(commitToBin = false)
            else -> resetCardPosition()
        }
    }

    private fun animateSwipe(commitToBin: Boolean) {
        if (photoUris.isEmpty()) return
        val direction = if (commitToBin) -1f else 1f
        val targetX = binding.photoStage.width * direction
        binding.cardPhoto.animate()
            .translationX(targetX)
            .rotation(18f * direction)
            .alpha(0.2f)
            .setDuration(180L)
            .withEndAction {
                reviewCurrent(commitToBin)
                binding.cardPhoto.alpha = 1f
                resetCardPosition(immediate = true)
            }
            .start()
    }

    private fun resetCardPosition(immediate: Boolean = false) {
        val animator = binding.cardPhoto.animate()
            .translationX(0f)
            .rotation(0f)
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
        if (immediate) {
            animator.setDuration(0L)
        } else {
            animator.setDuration(180L)
        }
        animator.start()
        binding.btnMoveToBin.alpha = 1f
        binding.btnKeep.alpha = 1f
    }

    private fun currentUri(): Uri? = photoUris.firstOrNull()

    private fun reviewCurrent(commitToBin: Boolean) {
        val uri = currentUri() ?: return
        if (commitToBin) {
            reviewStore.moveToTrash(uri)
        } else {
            reviewStore.markKept(uri)
        }
        photoUris.removeAt(0)
        render()
    }

    private fun render() {
        val currentUri = currentUri()
        binding.tvTitle.text = monthLabel.ifEmpty { getString(R.string.feature_swipe_title) }
        binding.emptyState.isVisible = currentUri == null
        binding.cardPhoto.isVisible = currentUri != null
        binding.imagePhoto.isVisible = currentUri != null
        binding.btnKeep.isVisible = currentUri != null
        binding.btnMoveToBin.isVisible = currentUri != null
        binding.btnShare.isVisible = currentUri != null
        if (currentUri != null) {
            binding.imagePhoto.setImageURI(currentUri)
        } else {
            binding.imagePhoto.setImageDrawable(null)
        }
        renderTrashBinSummary()
    }

    private fun renderTrashBinSummary() {
        val entries = loadTrashEntries()
        val count = entries.size
        binding.tvTrashCount.text = count.toString()
        binding.tvPending.text = getString(
            if (count == 1) R.string.swipe_trash_label_singular else R.string.swipe_trash_label_plural,
        )
        binding.btnOpenTrash.isEnabled = entries.isNotEmpty()
        binding.btnClearPending.isEnabled = entries.isNotEmpty()
        binding.tvTrashTitle.text = if (entries.isEmpty()) {
            getString(R.string.trash_bin_title_empty)
        } else {
            getString(
                R.string.trash_bin_title_format,
                Formatter.formatShortFileSize(requireContext(), entries.sumOf { it.sizeBytes }),
            )
        }
        binding.tvTrashEmpty.isVisible = entries.isEmpty()
        trashAdapter.submit(entries)
    }

    private fun openTrashBin() {
        renderTrashBinSummary()
        binding.trashOverlay.isVisible = true
    }

    private fun closeTrashBin() {
        binding.trashOverlay.isVisible = false
    }

    private fun shareCurrent() {
        val uri = currentUri() ?: return
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                getString(R.string.share_photo),
            ),
        )
    }

    private fun restoreTrashItem(item: TrashPhotoItem) {
        reviewStore.restoreFromTrash(item.uri)
        if (item.uri.toString() in monthPhotoUriStrings && item.uri !in photoUris) {
            photoUris.add(0, item.uri)
        }
        pendingDeleteUris = pendingDeleteUris.filterNot { it == item.uri }
        render()
    }

    private fun deletePendingPhotos() {
        val entries = loadTrashEntries()
        if (entries.isEmpty()) return
        pendingDeleteUris = entries.map(TrashPhotoItem::uri)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = MediaStore.createDeleteRequest(requireContext().contentResolver, pendingDeleteUris)
            deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        } else {
            val deleted = pendingDeleteUris.sumOf { uri ->
                runCatching { requireContext().contentResolver.delete(uri, null, null) }.getOrDefault(0)
            }
            if (deleted > 0) {
                clearReviewBin()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.photos_deleted_format, deleted),
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                Toast.makeText(requireContext(), R.string.delete_photos_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearReviewBin() {
        reviewStore.clearDeleted(pendingDeleteUris)
        pendingDeleteUris = emptyList()
        closeTrashBin()
        renderTrashBinSummary()
    }

    private fun loadTrashEntries(): List<TrashPhotoItem> {
        return reviewStore.loadTrashUris()
            .map { uri -> TrashPhotoItem(uri, querySizeBytes(uri)) }
            .sortedByDescending(TrashPhotoItem::sizeBytes)
    }

    private fun querySizeBytes(uri: Uri): Long {
        requireContext().contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
                return cursor.getLong(sizeIndex)
            }
        }
        return runCatching {
            requireContext().contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length.coerceAtLeast(0L)
            } ?: 0L
        }.getOrDefault(0L)
    }

    companion object {
        private const val ARG_MONTH = "month"

        fun newInstance(month: MediaMonth) = PhotoDetailFragment().apply {
            PhotoSelectionStore.set(month.photos.map(MediaPhoto::uri))
            arguments = Bundle().apply {
                putString(ARG_MONTH, month.label)
            }
        }
    }
}

private data class TrashPhotoItem(
    val uri: Uri,
    val sizeBytes: Long,
)

private class TrashBinAdapter(
    private val onRestore: (TrashPhotoItem) -> Unit,
) : RecyclerView.Adapter<TrashBinAdapter.TrashViewHolder>() {
    private var items: List<TrashPhotoItem> = emptyList()

    fun submit(newItems: List<TrashPhotoItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashViewHolder {
        val binding = ItemTrashBinPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrashViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrashViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TrashViewHolder(
        private val binding: ItemTrashBinPhotoBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TrashPhotoItem) {
            binding.imageThumb.setImageURI(item.uri)
            binding.tvSize.text = Formatter.formatShortFileSize(binding.root.context, item.sizeBytes)
            binding.btnRestore.setOnClickListener { onRestore(item) }
            binding.root.setOnClickListener { onRestore(item) }
        }
    }
}

private object PhotoSelectionStore {
    private var selectedUris: List<Uri> = emptyList()

    fun set(uris: List<Uri>) {
        selectedUris = uris
    }

    fun take(): List<Uri> = selectedUris
}
