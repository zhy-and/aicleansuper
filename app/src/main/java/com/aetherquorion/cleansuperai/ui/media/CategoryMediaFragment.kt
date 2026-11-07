package com.aetherquorion.cleansuperai.ui.media

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ConcatAdapter
import com.aetherquorion.cleansuperai.MainActivity
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.databinding.FragmentMediaListBinding
import com.aetherquorion.cleansuperai.ui.clean.detail.DisplayMediaItem
import com.aetherquorion.cleansuperai.ui.clean.detail.MediaCleanupAdapter
import com.aetherquorion.cleansuperai.ui.common.FeatureAccess
import com.aetherquorion.cleansuperai.ui.common.FeaturePermissionCoordinator
import com.aetherquorion.cleansuperai.ui.common.MediaActions
import com.aetherquorion.cleansuperai.ui.common.MediaPermissionHelper
import com.aetherquorion.cleansuperai.ui.flow.CleanupReportFragment
import com.aetherquorion.cleansuperai.ui.flow.CleanupResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

class CategoryMediaFragment : Fragment() {
    private data class MediaItem(val id: Long, val uri: Uri, val name: String, val size: Long, val modifiedAt: Long)

    private var _binding: FragmentMediaListBinding? = null
    private val binding get() = _binding!!
    private var items = emptyList<MediaItem>()
    private val selectedIds = linkedSetOf<Long>()
    private var loaded = false
    private var loading = false
    private var asked = false
    private var pendingTargets = emptyList<MediaItem>()
    private var recentVisibleCount = 0
    private val recentHeader = MediaSectionAdapter({ toggleSection(true) }, { selectSection(true) })
    private val olderHeader = MediaSectionAdapter({ toggleSection(false) }, { selectSection(false) })
    private val recentMedia = MediaCleanupAdapter(::openPreview, ::toggleSelection)
    private val olderMedia = MediaCleanupAdapter(::openPreview, ::toggleSelection)
    private val recentAudio = AudioCleanupAdapter(::openPreview, ::toggleSelection)
    private val olderAudio = AudioCleanupAdapter(::openPreview, ::toggleSelection)
    private val sectionAdapter by lazy {
        if (kind() == 2) ConcatAdapter(recentHeader, recentAudio, olderHeader, olderAudio)
        else ConcatAdapter(recentHeader, recentMedia, olderHeader, olderMedia)
    }

    private val access = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (hasAccess()) load() else showAccessRequired()
    }
    private val deleteApproval = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val attempted = pendingTargets
            load {
                val remaining = items.mapTo(hashSetOf()) { it.uri }
                val deleted = attempted.filter { it.uri !in remaining }
                selectedIds.removeAll(deleted.map { it.id }.toSet())
                render()
                showReport(attempted, deleted)
            }
        }
        pendingTargets = emptyList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentMediaListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.tvTitle.setText(titleRes())
        binding.actionContainer.isVisible = true
        binding.checkSelectAll.setOnClickListener { toggleSelectAll() }
        binding.btnPrimaryAction.setOnClickListener { confirmDelete() }
        val columns = if (kind() == 2) 1 else if (resources.configuration.screenWidthDp >= 430) 4 else 3
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), columns).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val firstHeader = if (recentHeader.visible) 1 else 0
                    val secondHeaderAt = firstHeader + recentVisibleCount
                    return if (position == 0 && recentHeader.visible ||
                        position == secondHeaderAt && olderHeader.visible) columns else 1
                }
            }
        }
        binding.recyclerView.adapter = sectionAdapter
        if (hasAccess()) load() else render()
    }

    override fun onResume() {
        super.onResume()
        if (hasAccess() && !loaded && !loading) load()
        else if (!hasAccess() && !asked) {
            asked = true
            access.launch(FeaturePermissionCoordinator.settingsIntent(requireContext(), FeatureAccess.ALL_FILES))
        }
    }

    override fun onDestroyView() {
        binding.recyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun hasAccess() = MediaPermissionHelper.hasPermissions(requireContext())
    private fun kind() = requireArguments().getInt(ARG_KIND)
    private fun titleRes() = when (kind()) {
        0 -> R.string.home_picture_cleanup
        1 -> R.string.home_video_cleanup
        else -> R.string.home_audio_cleanup
    }
    private fun collection() = when (kind()) {
        0 -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        1 -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    private fun load(afterLoad: (() -> Unit)? = null) {
        if (loading) return
        loading = true
        binding.emptyContainer.isVisible = false
        binding.recyclerView.isVisible = false
        binding.loadingView.root.isVisible = true
        binding.tvNote.isVisible = false
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val mediaCollection = collection()
                    val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED)
                    buildList {
                        context.contentResolver.query(mediaCollection, projection, null, null,
                            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")?.use { cursor ->
                            val id = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                            val name = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                            val size = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                            val date = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                            while (cursor.moveToNext()) {
                                val mediaId = cursor.getLong(id)
                                add(MediaItem(mediaId, ContentUris.withAppendedId(mediaCollection, mediaId),
                                    cursor.getString(name) ?: "", cursor.getLong(size), cursor.getLong(date) * 1000))
                            }
                        }
                    }
                }
            }
            loading = false
            loaded = result.isSuccess
            binding.loadingView.root.isVisible = false
            if (result.isSuccess) items = result.getOrThrow()
            render(result.exceptionOrNull()?.localizedMessage)
            if (result.isSuccess) afterLoad?.invoke()
        }
    }

    private fun render(error: String? = null) {
        val ui = _binding ?: return
        if (loading) {
            ui.recyclerView.isVisible = false
            ui.emptyContainer.isVisible = false
            ui.loadingView.root.isVisible = true
            ui.tvNote.isVisible = false
            return
        }
        ui.tvNote.isVisible = true
        selectedIds.retainAll(items.map { it.id }.toSet())
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val (recent, older) = items.partition { it.modifiedAt >= cutoff }
        recentVisibleCount = if (recentHeader.expanded) recent.size else 0
        val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
        fun display(item: MediaItem) =
            DisplayMediaItem(
                stableId = item.id,
                thumbnailUri = item.uri,
                title = item.name,
                metaPrimary = dateFormat.format(Date(item.modifiedAt)),
                metaSecondary = "",
                sizeText = Formatter.formatShortFileSize(requireContext(), item.size),
                isVideo = kind() == 1,
                isAudio = kind() == 2,
                isSelectable = true,
                isSelected = item.id in selectedIds,
            )
        recentHeader.title = getString(R.string.media_recent_group)
        olderHeader.title = getString(R.string.media_older_group)
        recentHeader.visible = recent.isNotEmpty()
        olderHeader.visible = older.isNotEmpty()
        recentHeader.summary = getString(R.string.media_group_summary, recent.size,
            Formatter.formatShortFileSize(requireContext(), recent.sumOf { it.size }))
        olderHeader.summary = getString(R.string.media_group_summary, older.size,
            Formatter.formatShortFileSize(requireContext(), older.sumOf { it.size }))
        recentHeader.selectLabel = getString(if (recent.isNotEmpty() && recent.all { it.id in selectedIds })
            R.string.clear_selected_action else R.string.select_all_action)
        olderHeader.selectLabel = getString(if (older.isNotEmpty() && older.all { it.id in selectedIds })
            R.string.clear_selected_action else R.string.select_all_action)
        recentHeader.refresh()
        olderHeader.refresh()
        if (kind() == 2) {
            recentAudio.submitList(if (recentHeader.expanded) recent.map(::display) else emptyList())
            olderAudio.submitList(if (olderHeader.expanded) older.map(::display) else emptyList())
        } else {
            recentMedia.submitList(if (recentHeader.expanded) recent.map(::display) else emptyList())
            olderMedia.submitList(if (olderHeader.expanded) older.map(::display) else emptyList())
        }
        ui.recyclerView.isVisible = items.isNotEmpty()
        ui.emptyContainer.isVisible = items.isEmpty()
        ui.tvEmptyTitle.setText(if (error == null) R.string.media_empty_title else R.string.media_error_title)
        ui.tvEmptyBody.text = error ?: getString(R.string.media_empty_body)
        ui.tvNote.text = getString(R.string.media_count_summary, items.size,
            Formatter.formatShortFileSize(requireContext(), items.sumOf { it.size }), selectedIds.size)
        ui.checkSelectAll.isChecked = items.isNotEmpty() && selectedIds.size == items.size
        ui.btnPrimaryAction.text = getString(R.string.similar_delete_count_action, selectedIds.size)
        ui.btnPrimaryAction.isEnabled = selectedIds.isNotEmpty()
    }

    private fun recentItems(): List<MediaItem> {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        return items.filter { it.modifiedAt >= cutoff }
    }

    private fun toggleSection(recent: Boolean) {
        val header = if (recent) recentHeader else olderHeader
        header.expanded = !header.expanded
        render()
    }

    private fun selectSection(recent: Boolean) {
        val recentItems = recentItems()
        val group = if (recent) recentItems else items.filter { it.modifiedAt < System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 }
        if (group.all { it.id in selectedIds }) selectedIds.removeAll(group.map { it.id }.toSet())
        else selectedIds.addAll(group.map { it.id })
        render()
    }

    private fun openPreview(item: DisplayMediaItem) {
        when (kind()) {
            0 -> MediaActions.openImagePreview(this, item.thumbnailUri, item.title)
            1 -> MediaActions.openVideoPreview(this, item.thumbnailUri, item.title)
            else -> runCatching {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(item.thumbnailUri, "audio/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            }.onFailure { Toast.makeText(requireContext(), R.string.audio_player_unavailable, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun toggleSelection(item: DisplayMediaItem) {
        if (!selectedIds.add(item.stableId)) selectedIds.remove(item.stableId)
        renderSelectionOnly()
    }

    private fun toggleSelectAll() {
        if (selectedIds.size == items.size && items.isNotEmpty()) selectedIds.clear()
        else {
            selectedIds.clear()
            selectedIds.addAll(items.map { it.id })
        }
        renderSelectionOnly()
    }

    private fun renderSelectionOnly() {
        fun update(adapter: MediaCleanupAdapter) {
            adapter.submitList(adapter.currentList.map { item ->
                val selected = item.stableId in selectedIds
                if (item.isSelected == selected) item else item.copy(isSelected = selected)
            })
        }
        fun updateAudio(adapter: AudioCleanupAdapter) {
            adapter.submitList(adapter.currentList.map { item ->
                val selected = item.stableId in selectedIds
                if (item.isSelected == selected) item else item.copy(isSelected = selected)
            })
        }
        if (kind() == 2) {
            updateAudio(recentAudio)
            updateAudio(olderAudio)
        } else {
            update(recentMedia)
            update(olderMedia)
        }
        val recent = recentItems()
        val older = items.size - recent.size
        recentHeader.selectLabel = getString(if (recent.isNotEmpty() && recent.all { it.id in selectedIds })
            R.string.clear_selected_action else R.string.select_all_action)
        olderHeader.selectLabel = getString(if (older > 0 && items.asSequence().filterNot { it.modifiedAt >= System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 }.all { it.id in selectedIds })
            R.string.clear_selected_action else R.string.select_all_action)
        recentHeader.refresh()
        olderHeader.refresh()
        binding.tvNote.text = getString(R.string.media_count_summary, items.size,
            Formatter.formatShortFileSize(requireContext(), items.sumOf { it.size }), selectedIds.size)
        binding.checkSelectAll.isChecked = items.isNotEmpty() && selectedIds.size == items.size
        binding.btnPrimaryAction.text = getString(R.string.similar_delete_count_action, selectedIds.size)
        binding.btnPrimaryAction.isEnabled = selectedIds.isNotEmpty()
    }

    private fun confirmDelete() {
        val targets = items.filter { it.id in selectedIds }
        if (targets.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.media_delete_title)
            .setMessage(getString(R.string.media_delete_message, targets.size))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete_dialog_confirm) { _, _ -> performDelete(targets) }
            .show()
    }

    private fun performDelete(targets: List<MediaItem>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pendingTargets = targets
            runCatching {
                val sender = MediaActions.createDeleteIntentSender(requireContext(), targets.map { it.uri })
                    ?: return@runCatching
                deleteApproval.launch(IntentSenderRequest.Builder(sender).build())
            }.onFailure { binding.tvNote.setText(R.string.delete_failed_message) }
        } else {
            val context = requireContext().applicationContext
            viewLifecycleOwner.lifecycleScope.launch {
                val deleted = withContext(Dispatchers.IO) {
                    targets.filter { runCatching { context.contentResolver.delete(it.uri, null, null) > 0 }.getOrDefault(false) }
                }
                val deletedIds = deleted.mapTo(hashSetOf()) { it.id }
                items = items.filterNot { it.id in deletedIds }
                selectedIds.removeAll(deletedIds)
                render()
                showReport(targets, deleted)
            }
        }
    }

    private fun showAccessRequired() {
        val ui = _binding ?: return
        ui.recyclerView.isVisible = false
        ui.emptyContainer.isVisible = true
        ui.tvEmptyTitle.setText(R.string.media_access_title)
        ui.tvEmptyBody.setText(R.string.media_access_body)
    }

    private fun showReport(attempted: List<MediaItem>, deleted: List<MediaItem>) {
        (activity as? MainActivity)?.openDetail(CleanupReportFragment.newInstance(
            getString(titleRes()), CleanupResult(attempted.size, deleted.size, deleted.sumOf { it.size })
        ), "cleanup_report")
    }

    companion object {
        private const val ARG_KIND = "kind"
        fun picture() = CategoryMediaFragment().apply { arguments = Bundle().apply { putInt(ARG_KIND, 0) } }
        fun video() = CategoryMediaFragment().apply { arguments = Bundle().apply { putInt(ARG_KIND, 1) } }
        fun audio() = CategoryMediaFragment().apply { arguments = Bundle().apply { putInt(ARG_KIND, 2) } }
    }
}
