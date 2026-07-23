package com.example.cleansuperai.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cleansuperai.MainActivity
import com.example.cleansuperai.R
import com.example.cleansuperai.data.model.MediaScanSummary
import com.example.cleansuperai.data.model.SimilarMediaMode
import com.example.cleansuperai.databinding.FragmentHomeBinding
import com.example.cleansuperai.databinding.ViewHomeFeatureCardBinding
import com.example.cleansuperai.ui.clean.CleanCenterFragment
import com.example.cleansuperai.ui.clean.detail.LargeVideoListFragment
import com.example.cleansuperai.ui.clean.detail.ScreenshotListFragment
import com.example.cleansuperai.ui.common.MediaPermissionHelper
import com.example.cleansuperai.ui.similar.SimilarPhotosFragment
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private var permissionDenied = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (MediaPermissionHelper.hasPermissions(requireContext())) {
                permissionDenied = false
                savePermissionDenied(false)
                renderPermission(true)
                viewModel.scanMedia()
            } else {
                permissionDenied = true
                savePermissionDenied(true)
                renderPermission(false)
                Toast.makeText(requireContext(), R.string.permission_denied_message, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        permissionDenied = permissionPreferences().getBoolean(KEY_PERMISSION_DENIED, false)
        setupHeader()
        setupCards()
        binding.btnPermission.setOnClickListener {
            if (permissionDenied) openAppSettings() else requestPermission()
        }
        binding.btnQuickClean.setOnClickListener {
            (activity as? MainActivity)?.openDetail(CleanCenterFragment(), "clean_center")
        }
        collectUiState()
    }

    override fun onResume() {
        super.onResume()
        val granted = MediaPermissionHelper.hasPermissions(requireContext())
        if (granted) {
            permissionDenied = false
            savePermissionDenied(false)
        }
        renderPermission(granted)
        if (granted && viewModel.uiState.value.scanStatus == HomeUiState.ScanStatus.IDLE) {
            viewModel.scanMedia()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupHeader() {
        binding.btnPremium.isVisible = false
        binding.btnSettings.setOnClickListener { (activity as? MainActivity)?.openProfile() }
    }

    private fun setupCards() {
        configureCard(
            binding.cardSimilarPhotos,
            getString(R.string.feature_similar_title),
            "0",
            getString(R.string.feature_smart_groups),
            R.drawable.ic_copy,
        )
        configureCard(
            binding.cardDuplicatePhotos,
            getString(R.string.feature_duplicate_photos),
            "0",
            getString(R.string.feature_smart_groups),
            R.drawable.ic_image,
        )
        configureCard(
            binding.cardDuplicateVideos,
            getString(R.string.feature_duplicate_videos),
            "0",
            getString(R.string.count_video_format, 0),
            R.drawable.ic_video_library,
        )
        configureCard(
            binding.cardAllVideos,
            getString(R.string.feature_all_videos),
            "0",
            getString(R.string.em_dash),
            R.drawable.ic_nav_swipe,
        )
        configureCard(
            binding.cardScreenshots,
            getString(R.string.feature_screenshots_short),
            "0",
            getString(R.string.em_dash),
            R.drawable.ic_screenshot,
        )

        binding.cardSimilarPhotos.root.setOnClickListener {
            openDetail(SimilarPhotosFragment.newInstance(SimilarMediaMode.SIMILAR_PHOTOS), "similar")
        }
        binding.cardDuplicatePhotos.root.setOnClickListener {
            openDetail(SimilarPhotosFragment.newInstance(SimilarMediaMode.DUPLICATE_PHOTOS), "duplicates")
        }
        binding.cardDuplicateVideos.root.setOnClickListener {
            openDetail(SimilarPhotosFragment.newInstance(SimilarMediaMode.DUPLICATE_VIDEOS), "duplicate_videos")
        }
        binding.cardAllVideos.root.setOnClickListener { openDetail(LargeVideoListFragment(), "videos") }
        binding.cardScreenshots.root.setOnClickListener { openDetail(ScreenshotListFragment(), "screenshots") }
    }

    private fun configureCard(
        card: ViewHomeFeatureCardBinding,
        title: String,
        value: String,
        subtitle: String,
        iconRes: Int,
    ) {
        card.tvFeatureTitle.text = title
        card.tvFeatureValue.text = value
        card.tvFeatureCount.text = subtitle
        card.ivFeatureIcon.setImageResource(iconRes)
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun renderPermission(granted: Boolean) {
        if (_binding == null) return
        binding.permissionContainer.isVisible = !granted
        binding.contentContainer.isVisible = granted
        binding.btnPermission.setText(if (permissionDenied) R.string.go_to_settings else R.string.grant_access)
    }

    private fun render(state: HomeUiState) {
        when (state.scanStatus) {
            HomeUiState.ScanStatus.IDLE,
            HomeUiState.ScanStatus.SCANNING,
            -> {
                binding.tvScanStatus.setText(R.string.scan_running_english)
                binding.tvScanSummary.setText(R.string.home_secure_analysis)
                binding.tvReclaimValue.text = getString(R.string.em_dash)
                binding.progressScan.isIndeterminate = true
                binding.scanRing.setIndeterminate(true)
                binding.tvScanPercent.isVisible = false
            }

            HomeUiState.ScanStatus.DONE -> {
                binding.tvScanStatus.setText(R.string.scan_ready_english)
                binding.progressScan.isIndeterminate = false
                binding.progressScan.progress = 100
                binding.scanRing.setIndeterminate(false)
                binding.scanRing.setProgress(1f)
                binding.tvScanPercent.text = "100%"
                binding.tvScanPercent.isVisible = true
                state.summary?.let(::renderSummary)
            }

            HomeUiState.ScanStatus.ERROR -> {
                binding.tvScanStatus.setText(R.string.scan_unavailable)
                binding.tvScanSummary.text =
                    state.errorMessage ?: getString(R.string.please_try_again)
                binding.tvReclaimValue.text = getString(R.string.em_dash)
                binding.progressScan.isIndeterminate = false
                binding.scanRing.setIndeterminate(false)
                binding.scanRing.setProgress(0f)
                binding.tvScanPercent.isVisible = false
            }
        }
    }

    private fun renderSummary(summary: MediaScanSummary) {
        val reclaim = Formatter.formatFileSize(requireContext(), summary.estimatedReclaimBytes)
        binding.tvReclaimValue.text = getString(R.string.home_reclaim_ready_format, reclaim)
        binding.tvScanSummary.text =
            getString(R.string.scan_summary_format, summary.mediaItemCount, reclaim)
        binding.cardSimilarPhotos.tvFeatureValue.text = summary.similarGroupCount.toString()
        binding.cardSimilarPhotos.tvFeatureCount.text =
            getString(R.string.count_group_format, summary.similarGroupCount)
        binding.cardDuplicatePhotos.tvFeatureValue.text = summary.duplicatePhotoGroupCount.toString()
        binding.cardDuplicatePhotos.tvFeatureCount.text =
            getString(R.string.count_group_format, summary.duplicatePhotoGroupCount)
        binding.cardDuplicateVideos.tvFeatureValue.text = summary.duplicateVideoGroupCount.toString()
        binding.cardDuplicateVideos.tvFeatureCount.text =
            getString(R.string.count_group_format, summary.duplicateVideoGroupCount)
        binding.cardAllVideos.tvFeatureValue.text = summary.totalVideos.toString()
        binding.cardAllVideos.tvFeatureCount.text =
            getString(R.string.count_video_format, summary.totalVideos)
        binding.cardScreenshots.tvFeatureValue.text = summary.screenshotCount.toString()
        binding.cardScreenshots.tvFeatureCount.text =
            getString(R.string.count_image_format, summary.screenshotCount)
    }

    private fun requestPermission() {
        permissionLauncher.launch(MediaPermissionHelper.requiredPermissions())
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${requireContext().packageName}"),
            ),
        )
    }

    private fun savePermissionDenied(denied: Boolean) {
        permissionPreferences().edit().putBoolean(KEY_PERMISSION_DENIED, denied).apply()
    }

    private fun permissionPreferences() =
        requireContext().getSharedPreferences("media_permission", 0)

    private fun openDetail(fragment: Fragment, tag: String) {
        if (!MediaPermissionHelper.hasPermissions(requireContext())) {
            requestPermission()
            return
        }
        parentFragmentManager.commit {
            replace(R.id.fragmentContainer, fragment)
            addToBackStack(tag)
        }
    }

    companion object {
        private const val KEY_PERMISSION_DENIED = "permission_denied"
    }
}
