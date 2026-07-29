package com.aetherquorion.cleansuperai.ui.common

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.MediaController
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.databinding.FragmentVideoPreviewBinding

class VideoPreviewFragment : DialogFragment() {
    private var _binding: FragmentVideoPreviewBinding? = null
    private val binding get() = _binding!!
    private var playbackPosition = 0
    private var resumePlayback = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_Cleansuperai_VideoPreview)
        playbackPosition = savedInstanceState?.getInt(STATE_PLAYBACK_POSITION) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVideoPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uri = BundleCompat.getParcelable(requireArguments(), ARG_URI, Uri::class.java)
        val title = requireArguments().getString(ARG_TITLE).orEmpty()
        binding.tvTitle.text = title.ifBlank { getString(R.string.video_preview_title) }
        binding.btnBack.setOnClickListener { dismiss() }
        ViewCompat.setOnApplyWindowInsetsListener(binding.previewChrome) { chrome, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            chrome.updatePadding(top = systemBars.top)
            insets
        }
        if (uri != null) {
            val controller = MediaController(requireContext())
            controller.setAnchorView(binding.videoPreview)
            binding.videoPreview.setMediaController(controller)
            binding.videoPreview.setVideoURI(uri)
            binding.videoPreview.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = false
                binding.progressPreview.visibility = View.GONE
                if (playbackPosition > 0) {
                    binding.videoPreview.seekTo(playbackPosition)
                }
                binding.videoPreview.start()
                controller.show(CONTROLS_TIMEOUT_MS)
            }
            binding.videoPreview.setOnErrorListener { _, _, _ ->
                binding.progressPreview.visibility = View.GONE
                Toast.makeText(requireContext(), R.string.video_preview_failed, Toast.LENGTH_SHORT)
                    .show()
                true
            }
        } else {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
            window.setBackgroundDrawable(Color.BLACK.toDrawable())
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (resumePlayback && playbackPosition > 0) {
            runCatching {
                binding.videoPreview.seekTo(playbackPosition)
                binding.videoPreview.start()
            }
        }
    }

    override fun onPause() {
        _binding?.videoPreview?.let { videoView ->
            resumePlayback = videoView.isPlaying
            playbackPosition = videoView.currentPosition.coerceAtLeast(0)
            runCatching { videoView.pause() }
        }
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_PLAYBACK_POSITION, playbackPosition)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        runCatching { _binding?.videoPreview?.stopPlayback() }
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "video_preview"
        private const val ARG_URI = "arg_uri"
        private const val ARG_TITLE = "arg_title"
        private const val STATE_PLAYBACK_POSITION = "state_playback_position"
        private const val CONTROLS_TIMEOUT_MS = 2_500

        fun newInstance(uri: Uri, title: String): VideoPreviewFragment {
            return VideoPreviewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_URI, uri)
                    putString(ARG_TITLE, title)
                }
            }
        }
    }
}
