package com.aetherquorion.cleansuperai.ui.clean.detail

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
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
import androidx.recyclerview.widget.GridLayoutManager
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.data.model.ScreenshotItem
import com.aetherquorion.cleansuperai.databinding.FragmentMediaListBinding
import com.aetherquorion.cleansuperai.ui.common.MediaActions
import kotlinx.coroutines.launch

class ScreenshotListFragment : Fragment() {
    private var _binding: FragmentMediaListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScreenshotListViewModel by viewModels()
    private val selectedIds = linkedSetOf<Long>()
    private val adapter = MediaCleanupAdapter(
        onPreviewClick = ::openPreview,
        onSelectionClick = ::toggleSelection,
        selectedIds = selectedIds,
    )
    private var pendingCleanupResult: com.aetherquorion.cleansuperai.ui.flow.CleanupResult? = null
    private var currentItems: List<ScreenshotItem> = emptyList()

    private val deleteRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val count = selectedIds.size
                selectedIds.clear()
                viewModel.load(force = true)
                showToast(getString(R.string.delete_items_complete_format, count))
                pendingCleanupResult?.let(::showCleanupReport)
            }
            pendingCleanupResult = null
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
        binding.tvTitle.setText(R.string.screenshot_detail_title)
        binding.tvNote.text = getString(R.string.selected_count_format, 0)
        binding.actionContainer.isVisible = true
        binding.checkSelectAll.setOnClickListener { toggleSelectAll() }
        binding.btnPrimaryAction.setOnClickListener { confirmDeleteSelected() }
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), gridSpanCount())
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

    private fun render(state: ScreenshotListUiState) {
        currentItems = state.items
        selectedIds.retainAll(state.items.map { it.id }.toSet())
        val items = state.items.map { item ->
            DisplayMediaItem(
                stableId = item.id,
                thumbnailUri = item.contentUri,
                title = item.displayName,
                metaPrimary = "",
                metaSecondary = "",
                sizeText = Formatter.formatFileSize(requireContext(), item.sizeBytes),
                isSelectable = true,
                isSelected = selectedIds.contains(item.id),
            )
        }
        adapter.submitList(items)

        binding.recyclerView.isVisible = items.isNotEmpty()
        binding.loadingView.root.isVisible = state.isLoading
        binding.emptyContainer.isVisible = !state.isLoading && items.isEmpty()
        binding.tvEmptyTitle.setText(R.string.empty_screenshot_title)
        binding.tvEmptyBody.text = state.errorMessage ?: getString(R.string.empty_screenshot_body)
        renderSelectionState(items.size)
    }

    private fun openPreview(item: DisplayMediaItem) {
        MediaActions.openImagePreview(this, item.thumbnailUri, item.title)
    }

    private fun toggleSelection(item: DisplayMediaItem) {
        if (selectedIds.contains(item.stableId)) {
            selectedIds.remove(item.stableId)
        } else {
            selectedIds.add(item.stableId)
        }
        adapter.refreshVisibleSelection(binding.recyclerView)
        renderSelectionState(currentItems.size)
    }

    private fun toggleSelectAll() {
        if (selectedIds.size == currentItems.size && currentItems.isNotEmpty()) {
            selectedIds.clear()
        } else {
            selectedIds.clear()
            selectedIds.addAll(currentItems.map { it.id })
        }
        adapter.refreshVisibleSelection(binding.recyclerView)
        renderSelectionState(currentItems.size)
    }

    private fun renderSelectionState(totalItems: Int) {
        binding.tvNote.text = getString(R.string.selected_count_format, selectedIds.size)
        binding.checkSelectAll.isChecked = totalItems > 0 && selectedIds.size == totalItems
        binding.btnPrimaryAction.text = getString(R.string.similar_delete_count_action, selectedIds.size)
        binding.btnPrimaryAction.isEnabled = selectedIds.isNotEmpty()
    }

    private fun confirmDeleteSelected() {
        if (selectedIds.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteSelectedItems()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_dialog_title)
            .setMessage(getString(R.string.delete_dialog_message_format, selectedIds.size))
            .setNegativeButton(R.string.delete_dialog_cancel, null)
            .setPositiveButton(R.string.delete_dialog_confirm) { _, _ ->
                deleteSelectedItems()
            }
            .show()
    }

    private fun deleteSelectedItems() {
        val targets = currentItems
            .filter { selectedIds.contains(it.id) }
        val uris = targets.map { it.contentUri }

        if (uris.isEmpty()) return
        val cleanupResult = com.aetherquorion.cleansuperai.ui.flow.CleanupResult(
            targets.size, targets.size, targets.sumOf { it.sizeBytes })

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intentSender = MediaActions.createDeleteIntentSender(requireContext(), uris)
                    ?: return@runCatching
                pendingCleanupResult = cleanupResult
                deleteRequestLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            } else {
                val deleted = MediaActions.deleteLegacy(requireContext(), uris)
                val count = selectedIds.size
                selectedIds.clear()
                viewModel.load(force = true)
                showToast(getString(R.string.delete_items_complete_format, count))
                if (deleted > 0) showCleanupReport(cleanupResult.copy(
                    deleted = deleted, freedBytes = if (deleted == targets.size) cleanupResult.freedBytes else 0L))
            }
        }.onFailure {
            showToast(getString(R.string.delete_failed_message))
        }
    }

    private fun showCleanupReport(result: com.aetherquorion.cleansuperai.ui.flow.CleanupResult) {
        (activity as? com.aetherquorion.cleansuperai.MainActivity)?.openDetail(
            com.aetherquorion.cleansuperai.ui.flow.CleanupReportFragment.newInstance(
                getString(R.string.home_picture_cleanup), result), "cleanup_report")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun gridSpanCount(): Int = if (resources.configuration.screenWidthDp >= 430) 4 else 3
}
