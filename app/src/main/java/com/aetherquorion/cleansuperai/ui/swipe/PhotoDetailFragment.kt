package com.aetherquorion.cleansuperai.ui.swipe

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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.databinding.FragmentPhotoDetailBinding
import com.aetherquorion.cleansuperai.databinding.ItemTrashBinPhotoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class PhotoDetailFragment : Fragment() {
    private var _binding: FragmentPhotoDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var monthLabel: String
    private lateinit var reviewStore: SwipeReviewStore
    private var photoUris: MutableList<Uri> = mutableListOf()
    private var monthPhotoUriStrings: Set<String> = emptySet()
    private var pendingDeleteUris: List<Uri> = emptyList()
    private var trashEntries: List<TrashPhotoItem> = emptyList()
    private var trashLoadJob: Job? = null

    private val trashAdapter = TrashBinAdapter(::restoreTrashItem)
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragActive = false
    private var photoTransitioning = false

    private val deleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val report = com.aetherquorion.cleansuperai.ui.flow.CleanupResult(
                    pendingDeleteUris.size, pendingDeleteUris.size,
                    trashEntries.filter { it.uri in pendingDeleteUris }.sumOf { it.sizeBytes })
                clearReviewBin()
                Toast.makeText(requireContext(), R.string.selected_photos_deleted, Toast.LENGTH_SHORT).show()
                showCleanupReport(report)
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
        binding.btnRestoreAll.setOnClickListener { restoreAllTrashItems() }
        binding.tvTrashTitle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { dragStartY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    binding.trashSheet.translationY = (event.rawY - dragStartY).coerceAtLeast(0f)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (binding.trashSheet.translationY > binding.trashSheet.height * 0.15f) closeTrashBin()
                    else binding.trashSheet.animate().translationY(0f).setDuration(180L).start()
                    true
                }
                else -> false
            }
        }

        binding.recyclerTrashBin.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerTrashBin.setHasFixedSize(true)
        binding.recyclerTrashBin.itemAnimator = null
        binding.recyclerTrashBin.adapter = trashAdapter
        attachSwipeGesture()
        render()
        loadTrashBinSummaryAsync()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        trashLoadJob?.cancel()
        _binding = null
    }

    private fun attachSwipeGesture() {
        binding.cardPhoto.setOnTouchListener { _, event ->
            if (photoUris.isEmpty() || photoTransitioning) return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    binding.cardPhoto.animate().cancel()
                    binding.cardPhoto.setLayerType(View.LAYER_TYPE_HARDWARE, null)
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
        if (photoUris.isEmpty() || photoTransitioning) return
        photoTransitioning = true
        val direction = if (commitToBin) -1f else 1f
        val targetX = binding.photoStage.width * direction
        binding.cardPhoto.animate()
            .translationX(targetX)
            .rotation(18f * direction)
            .alpha(0.2f)
            .setDuration(180L)
            .withEndAction {
                binding.cardPhoto.alpha = 0f
                binding.cardPhoto.translationX = 0f
                binding.cardPhoto.rotation = 0f
                binding.cardPhoto.scaleX = 1f
                binding.cardPhoto.scaleY = 1f
                binding.cardPhoto.setLayerType(View.LAYER_TYPE_NONE, null)
                binding.btnMoveToBin.alpha = 1f
                binding.btnKeep.alpha = 1f
                reviewCurrent(commitToBin, revealPhotoOnLoad = true)
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
            .withEndAction {
                binding.cardPhoto.setLayerType(View.LAYER_TYPE_NONE, null)
            }
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

    private fun reviewCurrent(commitToBin: Boolean, revealPhotoOnLoad: Boolean = false) {
        val uri = currentUri() ?: return
        if (commitToBin) {
            reviewStore.moveToTrash(uri)
        } else {
            reviewStore.markKept(uri)
        }
        photoUris.removeAt(0)
        if (commitToBin) {
            addPendingTrashEntry(uri)
            loadTrashBinSummaryAsync()
        }
        render(revealPhotoOnLoad)
    }

    private fun render(revealPhotoOnLoad: Boolean = false) {
        val currentUri = currentUri()
        binding.tvTitle.text = monthLabel.ifEmpty { getString(R.string.feature_swipe_title) }
        binding.emptyState.isVisible = currentUri == null
        binding.cardPhoto.isVisible = currentUri != null
        binding.imagePhoto.isVisible = currentUri != null
        binding.btnKeep.isVisible = currentUri != null
        binding.btnMoveToBin.isVisible = currentUri != null
        binding.btnShare.isVisible = currentUri != null
        if (currentUri != null) {
            binding.imagePhoto.load(currentUri) {
                error(R.drawable.ic_media_placeholder)
                crossfade(false)
                size(1280)
                if (revealPhotoOnLoad) {
                    listener(
                        onSuccess = { _, _ -> finishPhotoTransition() },
                        onError = { _, _ -> finishPhotoTransition() },
                    )
                }
            }
        } else {
            binding.imagePhoto.setImageDrawable(null)
            if (revealPhotoOnLoad) finishPhotoTransition()
        }
        renderTrashBinSummary()
    }

    private fun finishPhotoTransition() {
        _binding?.cardPhoto?.alpha = 1f
        photoTransitioning = false
    }

    private fun renderTrashBinSummary(entries: List<TrashPhotoItem> = trashEntries) {
        val count = entries.size
        binding.tvTrashCount.text = count.toString()
        binding.tvPending.text = getString(
            if (count == 1) R.string.swipe_trash_label_singular else R.string.swipe_trash_label_plural,
        )
        binding.btnOpenTrash.isEnabled = entries.isNotEmpty()
        binding.btnClearPending.isEnabled = entries.isNotEmpty()
        binding.btnRestoreAll.isEnabled = entries.isNotEmpty()
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

    private fun addPendingTrashEntry(uri: Uri) {
        if (trashEntries.any { it.uri == uri }) return
        trashEntries = listOf(TrashPhotoItem(uri, 0L)) + trashEntries
        renderTrashBinSummary()
    }

    private fun loadTrashBinSummaryAsync() {
        trashLoadJob?.cancel()
        trashLoadJob = viewLifecycleOwner.lifecycleScope.launch {
            val entries = withContext(Dispatchers.IO) { loadTrashEntries() }
            trashEntries = entries
            renderTrashBinSummary()
        }
    }

    private fun openTrashBin() {
        binding.trashSheet.translationY = 0f
        renderTrashBinSummary()
        loadTrashBinSummaryAsync()
        binding.trashOverlay.isVisible = true
    }

    private fun closeTrashBin() {
        binding.trashSheet.translationY = 0f
        binding.trashOverlay.isVisible = false
    }

    private fun restoreAllTrashItems() {
        trashEntries.toList().forEach(::restoreTrashItem)
        closeTrashBin()
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
        trashEntries = trashEntries.filterNot { it.uri == item.uri }
        if (item.uri.toString() in monthPhotoUriStrings && item.uri !in photoUris) {
            photoUris.add(0, item.uri)
        }
        pendingDeleteUris = pendingDeleteUris.filterNot { it == item.uri }
        render()
    }

    private fun deletePendingPhotos() {
        val entries = trashEntries
        if (entries.isEmpty()) return
        pendingDeleteUris = entries.map(TrashPhotoItem::uri)
        val attemptedBytes = entries.sumOf { it.sizeBytes }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = MediaStore.createDeleteRequest(requireContext().contentResolver, pendingDeleteUris)
            deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        } else {
            viewLifecycleOwner.lifecycleScope.launch {
                val deleted = withContext(Dispatchers.IO) {
                    pendingDeleteUris.sumOf { uri ->
                        runCatching {
                            requireContext().contentResolver.delete(uri, null, null)
                        }.getOrDefault(0)
                    }
                }
                if (deleted > 0) {
                    clearReviewBin()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.photos_deleted_format, deleted),
                        Toast.LENGTH_SHORT,
                    ).show()
                    showCleanupReport(com.aetherquorion.cleansuperai.ui.flow.CleanupResult(
                        entries.size, deleted, if (deleted == entries.size) attemptedBytes else 0L))
                } else {
                    Toast.makeText(requireContext(), R.string.delete_photos_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCleanupReport(result: com.aetherquorion.cleansuperai.ui.flow.CleanupResult) {
        (activity as? com.aetherquorion.cleansuperai.MainActivity)?.openDetail(
            com.aetherquorion.cleansuperai.ui.flow.CleanupReportFragment.newInstance(
                getString(R.string.home_picture_cleanup), result), "cleanup_report")
    }

    private fun clearReviewBin() {
        val deletedUris = pendingDeleteUris.toSet()
        reviewStore.clearDeleted(deletedUris)
        trashEntries = trashEntries.filterNot { it.uri in deletedUris }
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
) : ListAdapter<TrashPhotoItem, TrashBinAdapter.TrashViewHolder>(DiffCallback) {

    init {
        setHasStableIds(true)
    }

    fun submit(newItems: List<TrashPhotoItem>) = submitList(newItems)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashViewHolder {
        val binding = ItemTrashBinPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrashViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrashViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long = getItem(position).uri.toString().hashCode().toLong()

    inner class TrashViewHolder(
        private val binding: ItemTrashBinPhotoBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TrashPhotoItem) {
            binding.imageThumb.load(item.uri) {
                placeholder(R.drawable.ic_media_placeholder)
                error(R.drawable.ic_media_placeholder)
                crossfade(false)
                size(360)
            }
            binding.tvSize.text = Formatter.formatShortFileSize(binding.root.context, item.sizeBytes)
            binding.btnRestore.setOnClickListener { onRestore(item) }
            binding.root.setOnClickListener { onRestore(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<TrashPhotoItem>() {
        override fun areItemsTheSame(oldItem: TrashPhotoItem, newItem: TrashPhotoItem): Boolean {
            return oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(oldItem: TrashPhotoItem, newItem: TrashPhotoItem): Boolean {
            return oldItem == newItem
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
