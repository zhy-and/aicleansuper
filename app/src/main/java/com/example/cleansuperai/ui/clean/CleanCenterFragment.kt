package com.example.cleansuperai.ui.clean

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cleansuperai.R
import com.example.cleansuperai.data.model.MediaScanSummary
import com.example.cleansuperai.databinding.FragmentCleanCenterBinding
import com.example.cleansuperai.ui.contacts.ContactsCleanupFragment
import com.example.cleansuperai.ui.clean.detail.LargeVideoListFragment
import com.example.cleansuperai.ui.clean.detail.ScreenshotListFragment
import com.example.cleansuperai.ui.common.MediaPermissionHelper
import com.example.cleansuperai.data.model.SimilarMediaMode
import com.example.cleansuperai.ui.similar.SimilarPhotosFragment
import kotlinx.coroutines.launch

class CleanCenterFragment : Fragment() {
    private var _binding: FragmentCleanCenterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CleanCenterViewModel by viewModels()
    private var pendingNavigation: PendingNavigation = PendingNavigation.NONE

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.values.all { it }
            viewModel.updatePermission(granted)
            if (granted) {
                when (pendingNavigation) {
                    PendingNavigation.SCREENSHOTS -> openScreenshots()
                    PendingNavigation.LARGE_VIDEOS -> openLargeVideos()
                    PendingNavigation.SIMILAR -> openSimilar()
                    PendingNavigation.NONE -> Unit
                }
            } else {
                showToast(getString(R.string.permission_denied_message))
            }
            pendingNavigation = PendingNavigation.NONE
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCleanCenterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClicks()
        collectUiState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.updatePermission(MediaPermissionHelper.hasPermissions(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupClicks() {
        binding.btnGrantOrRefresh.setOnClickListener {
            if (MediaPermissionHelper.hasPermissions(requireContext())) {
                viewModel.loadSummary()
            } else {
                permissionLauncher.launch(MediaPermissionHelper.requiredPermissions())
            }
        }

        binding.cardScreenshots.setOnClickListener {
            pendingNavigation = PendingNavigation.SCREENSHOTS
            openWithPermission()
        }

        binding.cardLargeVideos.setOnClickListener {
            pendingNavigation = PendingNavigation.LARGE_VIDEOS
            openWithPermission()
        }

        binding.cardSimilar.setOnClickListener {
            pendingNavigation = PendingNavigation.SIMILAR
            openWithPermission()
        }

        binding.cardContacts.setOnClickListener {
            parentFragmentManager.commit {
                replace(R.id.fragmentContainer, ContactsCleanupFragment())
                addToBackStack("contacts_cleanup")
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

    private fun render(state: CleanCenterUiState) {
        binding.progressBar.progress = if (state.isLoading) 45 else if (state.summary != null) 100 else 0

        if (!state.hasPermission) {
            binding.btnGrantOrRefresh.isEnabled = true
            binding.btnGrantOrRefresh.setText(R.string.permission_action_label)
            binding.tvReclaimSummary.setText(R.string.clean_permission_title)
            binding.tvLoadedHint.setText(R.string.clean_permission_body)
            binding.tvScreenshotCount.setText(R.string.count_placeholder)
            binding.tvLargeVideoCount.setText(R.string.count_placeholder)
            binding.tvSimilarCount.setText(R.string.phase_three_label)
            binding.tvContactsCount.setText(R.string.count_placeholder)
            binding.tvScreenshotSpace.setText(R.string.feature_screenshots_desc)
            binding.tvLargeVideoSpace.setText(R.string.feature_videos_desc)
            return
        }

        binding.btnGrantOrRefresh.setText(
            if (state.isLoading) R.string.scan_button_running else R.string.refresh_action_label,
        )
        binding.btnGrantOrRefresh.isEnabled = !state.isLoading

        if (state.isLoading) {
            binding.tvLoadedHint.setText(R.string.clean_loading_hint)
        } else if (state.summary != null) {
            renderSummary(state.summary)
        } else {
            binding.tvLoadedHint.text = state.errorMessage ?: getString(R.string.clean_empty_hint)
        }
    }

    private fun renderSummary(summary: MediaScanSummary) {
        val context = requireContext()
        val reclaimText = Formatter.formatFileSize(context, summary.estimatedReclaimBytes)
        val screenshotBytesText = Formatter.formatFileSize(context, summary.screenshotBytes)
        val largeVideoBytesText = Formatter.formatFileSize(context, summary.largeVideoBytes)

        binding.tvReclaimSummary.text = getString(R.string.clean_reclaim_summary_format, reclaimText)
        binding.tvLoadedHint.setText(R.string.clean_loaded_hint)
        binding.tvScreenshotCount.text = getString(R.string.count_image_format, summary.screenshotCount)
        binding.tvLargeVideoCount.text = getString(R.string.count_video_format, summary.largeVideoCount)
        binding.tvSimilarCount.text = getString(R.string.feature_similar_space_format, summary.similarGroupCount)
        binding.tvContactsCount.setText(R.string.feature_ready_label)
        binding.tvScreenshotSpace.text = getString(R.string.screenshot_space_format, screenshotBytesText)
        binding.tvLargeVideoSpace.text = getString(R.string.large_video_space_format, largeVideoBytesText)
    }

    private fun openWithPermission() {
        if (MediaPermissionHelper.hasPermissions(requireContext())) {
            when (pendingNavigation) {
                PendingNavigation.SCREENSHOTS -> openScreenshots()
                PendingNavigation.LARGE_VIDEOS -> openLargeVideos()
                PendingNavigation.SIMILAR -> openSimilar()
                PendingNavigation.NONE -> Unit
            }
            pendingNavigation = PendingNavigation.NONE
        } else {
            permissionLauncher.launch(MediaPermissionHelper.requiredPermissions())
        }
    }

    private fun openScreenshots() {
        parentFragmentManager.commit {
            replace(R.id.fragmentContainer, ScreenshotListFragment())
            addToBackStack("screenshot_list")
        }
    }

    private fun openLargeVideos() {
        parentFragmentManager.commit {
            replace(R.id.fragmentContainer, LargeVideoListFragment())
            addToBackStack("large_video_list")
        }
    }

    private fun openSimilar() {
        parentFragmentManager.commit {
            replace(
                R.id.fragmentContainer,
                SimilarPhotosFragment.newInstance(SimilarMediaMode.SIMILAR_PHOTOS),
            )
            addToBackStack("similar_photos")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private enum class PendingNavigation {
        NONE,
        SCREENSHOTS,
        LARGE_VIDEOS,
        SIMILAR,
    }
}
