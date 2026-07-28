package com.example.cleansuperai.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.Formatter
import android.animation.ValueAnimator
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
import android.view.animation.OvershootInterpolator
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
import com.example.cleansuperai.ui.contacts.ContactsCleanupFragment
import com.example.cleansuperai.ui.similar.SimilarPhotosFragment
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private var permissionDenied = false
    private var heroAnimated = false
    private var lastScanStatus: HomeUiState.ScanStatus? = null

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
            R.drawable.bg_home_tool_icon_orange,
        )
        configureCard(
            binding.cardDuplicatePhotos,
            getString(R.string.feature_duplicate_photos),
            "0",
            getString(R.string.feature_smart_groups),
            R.drawable.ic_image,
            R.drawable.bg_home_tool_icon_blue,
        )
        configureCard(
            binding.cardDuplicateVideos,
            getString(R.string.feature_duplicate_videos),
            "0",
            getString(R.string.count_video_format, 0),
            R.drawable.ic_video_library,
            R.drawable.bg_home_tool_icon_green,
        )
        configureCard(
            binding.cardAllVideos,
            getString(R.string.feature_videos_title),
            "0",
            getString(R.string.em_dash),
            R.drawable.ic_video_library,
            R.drawable.bg_home_tool_icon_blue,
        )
        configureCard(
            binding.cardScreenshots,
            getString(R.string.feature_screenshots_short),
            "0",
            getString(R.string.em_dash),
            R.drawable.ic_screenshot,
            R.drawable.bg_home_tool_icon_teal,
        )
        configureCard(
            binding.cardSwipePhotos,
            getString(R.string.feature_contacts_title),
            null,
            getString(R.string.home_contacts_short_desc),
            R.drawable.ic_contacts,
            R.drawable.bg_home_tool_icon_pink,
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
        binding.cardSwipePhotos.root.setOnClickListener {
            openDetail(ContactsCleanupFragment(), "contacts_cleanup")
        }
    }

    private fun configureCard(
        card: ViewHomeFeatureCardBinding,
        title: String,
        value: String?,
        subtitle: String,
        iconRes: Int,
        iconBackgroundRes: Int,
    ) {
        card.tvFeatureTitle.text = title
        card.tvFeatureValue.isVisible = !value.isNullOrBlank()
        card.tvFeatureValue.text = value.orEmpty()
        card.tvFeatureCount.text = subtitle
        card.ivFeatureIcon.setImageResource(iconRes)
        card.iconContainer.setBackgroundResource(iconBackgroundRes)
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
        if (granted) {
            animateHeroEntry()
        }
    }

    private fun render(state: HomeUiState) {
        when (state.scanStatus) {
            HomeUiState.ScanStatus.IDLE,
            HomeUiState.ScanStatus.SCANNING,
            -> {
                binding.tvScanSummary.setText(R.string.scan_running_english)
                binding.tvReclaimValue.text = getString(R.string.em_dash)
                binding.progressScan.isVisible = false
                binding.progressScan.isIndeterminate = true
                binding.scanRing.setIndeterminate(true)
                binding.tvScanPercent.isVisible = false
            }

            HomeUiState.ScanStatus.DONE -> {
                binding.progressScan.isVisible = false
                binding.progressScan.isIndeterminate = false
                if (lastScanStatus != HomeUiState.ScanStatus.DONE) {
                    state.summary?.let(::animateSummary)
                } else {
                    state.summary?.let(::renderSummary)
                }
            }

            HomeUiState.ScanStatus.ERROR -> {
                binding.tvScanSummary.text =
                    state.errorMessage ?: getString(R.string.please_try_again)
                binding.tvReclaimValue.text = getString(R.string.em_dash)
                binding.progressScan.isVisible = false
                binding.progressScan.isIndeterminate = false
                binding.scanRing.setIndeterminate(false)
                binding.scanRing.setProgress(0f)
                binding.tvScanPercent.isVisible = false
            }
        }
        lastScanStatus = state.scanStatus
    }

    private fun renderSummary(summary: MediaScanSummary) {
        val reclaim = Formatter.formatFileSize(requireContext(), summary.estimatedReclaimBytes)
        binding.tvReclaimValue.text = getString(R.string.home_reclaim_compact_format, reclaim)
        binding.tvScanSummary.text =
            getString(R.string.home_storage_detail_format, reclaim, summary.mediaItemCount)
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

    private fun animateSummary(summary: MediaScanSummary) {
        renderSummary(summary)
        binding.scanRing.setIndeterminate(false)
        binding.scanRing.setProgress(0f)
        binding.tvScanPercent.isVisible = true
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900
            interpolator = OvershootInterpolator(0.8f)
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                binding.scanRing.setProgress(progress.coerceIn(0f, 1f))
                binding.tvScanPercent.text = "${(progress * 100).roundToInt()}%"
            }
            start()
        }
    }

    private fun animateHeroEntry() {
        if (heroAnimated || _binding == null || !binding.contentContainer.isVisible) return
        heroAnimated = true
        val views = listOf(binding.scanRing, binding.tvScanSummary, binding.tvReclaimValue, binding.btnQuickClean)
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 20f
            view.scaleX = if (view === binding.scanRing) 0.92f else 1f
            view.scaleY = if (view === binding.scanRing) 0.92f else 1f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay((index * 70).toLong())
                .setDuration(420)
                .setInterpolator(OvershootInterpolator(0.85f))
                .start()
        }
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
