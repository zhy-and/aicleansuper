package com.example.cleansuperai.ui.clean.detail

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cleansuperai.R
import com.example.cleansuperai.data.model.LargeVideoItem
import com.example.cleansuperai.databinding.FragmentMediaListBinding
import com.example.cleansuperai.ui.common.MediaActions
import kotlinx.coroutines.launch
import java.util.Date

class LargeVideoListFragment : Fragment() {
    private var _binding: FragmentMediaListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LargeVideoListViewModel by viewModels()
    private val adapter = MediaCleanupAdapter(
        onPreviewClick = ::openPreview,
        onSelectionClick = ::toggleSelection,
    )
    private val selectedIds = linkedSetOf<Long>()
    private var currentFilteredItems: List<LargeVideoItem> = emptyList()
    private var minSizeBytes: Long = THRESHOLD_100_MB

    private val deleteRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val count = selectedIds.size
                selectedIds.clear()
                viewModel.load(force = true)
                showToast(getString(R.string.delete_items_complete_format, count))
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMediaListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.tvTitle.setText(R.string.large_video_detail_title)
        binding.tvSubtitle.setText(R.string.large_video_detail_subtitle)
        binding.actionContainer.isVisible = true
        binding.btnSecondaryAction.setText(R.string.select_all_action)
        binding.btnPrimaryAction.setText(R.string.delete_selected_action)
        binding.btnSecondaryAction.setOnClickListener { toggleSelectAll() }
        binding.btnPrimaryAction.setOnClickListener { confirmDeleteSelected() }
        binding.tvNote.setOnClickListener { cycleFilter() }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        collectUiState()
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: LargeVideoListUiState) {
        currentFilteredItems = state.items.filter { it.sizeBytes >= minSizeBytes }
        selectedIds.retainAll(currentFilteredItems.map { it.id }.toSet())
        val items = currentFilteredItems.map { item ->
            DisplayMediaItem(
                stableId = item.id,
                thumbnailUri = item.contentUri,
                title = item.displayName,
                metaPrimary = getString(
                    R.string.media_item_duration_format,
                    formatDuration(item.durationMs),
                ),
                metaSecondary = getString(
                    R.string.media_item_date_format,
                    DateFormat.format("yyyy-MM-dd HH:mm", Date(item.dateModifiedMs)).toString(),
                ),
                sizeText = Formatter.formatFileSize(requireContext(), item.sizeBytes),
                isVideo = true,
                isSelectable = true,
                isSelected = selectedIds.contains(item.id),
            )
        }
        adapter.submitList(items)

        binding.recyclerView.isVisible = items.isNotEmpty()
        binding.emptyContainer.isVisible = !state.isLoading && items.isEmpty()
        binding.tvEmptyTitle.setText(R.string.empty_large_video_title)
        binding.tvEmptyBody.text = state.errorMessage ?: getString(R.string.empty_large_video_body)
        renderSelectionState(items.size)
    }

    private fun cycleFilter() {
        minSizeBytes = if (minSizeBytes == THRESHOLD_100_MB) {
            THRESHOLD_500_MB
        } else {
            THRESHOLD_100_MB
        }
        render(viewModel.uiState.value)
    }

    private fun openPreview(item: DisplayMediaItem) {
        MediaActions.openVideoExternally(requireContext(), item.thumbnailUri)
    }

    private fun toggleSelection(item: DisplayMediaItem) {
        if (selectedIds.contains(item.stableId)) {
            selectedIds.remove(item.stableId)
        } else {
            selectedIds.add(item.stableId)
        }
        render(viewModel.uiState.value)
    }

    private fun toggleSelectAll() {
        if (selectedIds.size == currentFilteredItems.size && currentFilteredItems.isNotEmpty()) {
            selectedIds.clear()
        } else {
            selectedIds.clear()
            selectedIds.addAll(currentFilteredItems.map { it.id })
        }
        render(viewModel.uiState.value)
    }

    private fun renderSelectionState(totalItems: Int) {
        val filterLabel = if (minSizeBytes == THRESHOLD_500_MB) {
            getString(R.string.filter_500mb)
        } else {
            getString(R.string.filter_100mb)
        }
        binding.tvNote.text = getString(
            R.string.video_selection_filter_format,
            selectedIds.size,
            filterLabel,
        )
        binding.btnSecondaryAction.setText(
            if (selectedIds.size == totalItems && totalItems > 0) {
                R.string.clear_selected_action
            } else {
                R.string.select_all_action
            },
        )
        binding.btnPrimaryAction.isEnabled = selectedIds.isNotEmpty()
    }

    private fun confirmDeleteSelected() {
        if (selectedIds.isEmpty()) return

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_video_dialog_title)
            .setMessage(getString(R.string.delete_video_dialog_message_format, selectedIds.size))
            .setNegativeButton(R.string.delete_dialog_cancel, null)
            .setPositiveButton(R.string.delete_dialog_confirm) { _, _ ->
                deleteSelectedItems()
            }
            .show()
    }

    private fun deleteSelectedItems() {
        val uris = currentFilteredItems
            .filter { selectedIds.contains(it.id) }
            .map { it.contentUri }

        if (uris.isEmpty()) return

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intentSender = MediaActions.createDeleteIntentSender(requireContext(), uris)
                    ?: return@runCatching
                deleteRequestLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            } else {
                MediaActions.deleteLegacy(requireContext(), uris)
                val count = selectedIds.size
                selectedIds.clear()
                viewModel.load(force = true)
                showToast(getString(R.string.delete_items_complete_format, count))
            }
        }.onFailure {
            showToast(getString(R.string.delete_failed_message))
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val THRESHOLD_100_MB = 100L * 1024L * 1024L
        private const val THRESHOLD_500_MB = 500L * 1024L * 1024L
    }
}
