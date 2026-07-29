package com.aetherquorion.cleansuperai.ui.similar

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.data.model.SimilarMediaMode
import com.aetherquorion.cleansuperai.data.model.SimilarPhotoItem
import com.aetherquorion.cleansuperai.databinding.FragmentSimilarPhotosBinding
import com.aetherquorion.cleansuperai.ui.common.MediaActions
import kotlinx.coroutines.launch

class SimilarPhotosFragment : Fragment() {
    private var _binding: FragmentSimilarPhotosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SimilarPhotosViewModel by viewModels()
    private val selectedIds = linkedSetOf<Long>()
    private var initializedSelection = false
    private lateinit var adapter: SimilarGroupAdapter
    private val mode: SimilarMediaMode by lazy {
        val raw = arguments?.getString(ARG_MODE).orEmpty()
        runCatching { SimilarMediaMode.valueOf(raw) }.getOrDefault(SimilarMediaMode.SIMILAR_PHOTOS)
    }

    private val deleteRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val count = selectedIds.size
                selectedIds.clear()
                initializedSelection = false
                viewModel.load(mode, force = true)
                showToast(getString(R.string.delete_items_complete_format, count))
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSimilarPhotosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyModeCopy()
        adapter = SimilarGroupAdapter(
            selectedIds = selectedIds,
            onPreview = ::openPreview,
            onToggle = ::toggleSelection,
        )
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSelectRecommended.setOnClickListener { selectAllCandidates() }
        binding.btnDeleteSelected.setOnClickListener { confirmDeleteSelected() }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        collectUiState()
        viewModel.load(mode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyModeCopy() {
        when (mode) {
            SimilarMediaMode.SIMILAR_PHOTOS -> {
                binding.tvTitle.setText(R.string.similar_page_title)
                binding.tvEmptyTitle.setText(R.string.similar_group_empty_title)
            }

            SimilarMediaMode.DUPLICATE_PHOTOS -> {
                binding.tvTitle.setText(R.string.duplicate_photos_page_title)
                binding.tvEmptyTitle.setText(R.string.duplicate_photos_empty_title)
            }

            SimilarMediaMode.DUPLICATE_VIDEOS -> {
                binding.tvTitle.setText(R.string.duplicate_videos_page_title)
                binding.tvEmptyTitle.setText(R.string.duplicate_videos_empty_title)
            }
        }
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: SimilarPhotosUiState) {
        val validIds = state.groups
            .flatMap { group -> group.items.filterNot { it.id == group.recommendedKeepId } }
            .map { it.id }
            .toSet()
        selectedIds.retainAll(validIds)
        if (!initializedSelection && state.groups.isNotEmpty()) {
            selectedIds.clear()
            selectedIds.addAll(validIds)
            initializedSelection = true
        }

        adapter.submitList(state.groups.toList()) {
            adapter.notifyDataSetChanged()
        }
        binding.recyclerView.isVisible = state.groups.isNotEmpty()
        binding.emptyContainer.isVisible = !state.isLoading && state.groups.isEmpty()
        binding.actionContainer.isVisible = state.groups.isNotEmpty()
        binding.tvEmptyBody.text = state.errorMessage ?: emptyBodyForMode()
        binding.btnDeleteSelected.isEnabled = selectedIds.isNotEmpty()
        binding.tvNote.text = getString(R.string.selected_count_format, selectedIds.size)

        val reclaimBytes = state.groups.sumOf { it.estimatedReclaimBytes }
        val reclaimText = Formatter.formatFileSize(requireContext(), reclaimBytes)
        binding.tvSummaryTitle.text = reclaimText
        binding.tvSummarySubtitle.text = getString(
            summaryFormatForMode(),
            state.groups.size,
            state.groups.sumOf { it.items.size },
        )
    }

    private fun emptyBodyForMode(): String = getString(
        when (mode) {
            SimilarMediaMode.SIMILAR_PHOTOS -> R.string.similar_group_empty_body
            SimilarMediaMode.DUPLICATE_PHOTOS -> R.string.duplicate_photos_empty_body
            SimilarMediaMode.DUPLICATE_VIDEOS -> R.string.duplicate_videos_empty_body
        },
    )

    private fun summaryFormatForMode(): Int = when (mode) {
        SimilarMediaMode.SIMILAR_PHOTOS -> R.string.similar_group_summary_format
        SimilarMediaMode.DUPLICATE_PHOTOS -> R.string.duplicate_photos_summary_format
        SimilarMediaMode.DUPLICATE_VIDEOS -> R.string.duplicate_videos_summary_format
    }

    private fun openPreview(item: SimilarPhotoItem) {
        if (mode == SimilarMediaMode.DUPLICATE_VIDEOS || item.durationMs > 0L) {
            MediaActions.openVideoExternally(requireContext(), item.contentUri)
        } else {
            MediaActions.openImagePreview(this, item.contentUri, item.displayName)
        }
    }

    private fun toggleSelection(item: SimilarPhotoItem) {
        if (selectedIds.contains(item.id)) {
            selectedIds.remove(item.id)
        } else {
            selectedIds.add(item.id)
        }
        render(viewModel.uiState.value)
    }

    private fun selectAllCandidates() {
        val candidates = viewModel.uiState.value.groups
            .flatMap { group -> group.items.filterNot { it.id == group.recommendedKeepId } }
            .map { it.id }
        if (selectedIds.size == candidates.size && candidates.isNotEmpty()) {
            selectedIds.clear()
        } else {
            selectedIds.clear()
            selectedIds.addAll(candidates)
        }
        render(viewModel.uiState.value)
    }

    private fun confirmDeleteSelected() {
        if (selectedIds.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_similar_dialog_title)
            .setMessage(getString(R.string.delete_similar_dialog_message_format, selectedIds.size))
            .setNegativeButton(R.string.delete_dialog_cancel, null)
            .setPositiveButton(R.string.delete_dialog_confirm) { _, _ ->
                deleteSelectedItems()
            }
            .show()
    }

    private fun deleteSelectedItems() {
        val uris = viewModel.uiState.value.groups
            .flatMap { it.items }
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
                initializedSelection = false
                viewModel.load(mode, force = true)
                showToast(getString(R.string.delete_items_complete_format, count))
            }
        }.onFailure {
            showToast(getString(R.string.delete_failed_message))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val ARG_MODE = "similar_media_mode"

        fun newInstance(mode: SimilarMediaMode): SimilarPhotosFragment {
            return SimilarPhotosFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, mode.name)
                }
            }
        }
    }
}
