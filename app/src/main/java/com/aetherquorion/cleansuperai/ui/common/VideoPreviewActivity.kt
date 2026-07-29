package com.aetherquorion.cleansuperai.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.MediaController
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.databinding.FragmentVideoPreviewBinding
import java.io.File

class VideoPreviewActivity : AppCompatActivity() {
    private lateinit var binding: FragmentVideoPreviewBinding
    private var playbackPosition = 0
    private var resumePlayback = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.isNavigationBarContrastEnforced = false

        binding = FragmentVideoPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        playbackPosition = savedInstanceState?.getInt(STATE_PLAYBACK_POSITION) ?: 0

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        binding.tvTitle.text = title.ifBlank { getString(R.string.video_preview_title) }
        binding.btnBack.setOnClickListener { finish() }
        ViewCompat.setOnApplyWindowInsetsListener(binding.previewChrome) { chrome, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            chrome.updatePadding(top = systemBars.top)
            insets
        }

        val uri = resolveVideoUri()
        if (uri == null) {
            Toast.makeText(this, R.string.video_preview_failed, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val controller = MediaController(this).apply {
            setAnchorView(binding.videoPreview)
        }
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
            Toast.makeText(this, R.string.video_preview_failed, Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        if (resumePlayback && playbackPosition > 0) {
            runCatching {
                binding.videoPreview.seekTo(playbackPosition)
                binding.videoPreview.start()
            }
        }
    }

    override fun onPause() {
        resumePlayback = binding.videoPreview.isPlaying
        playbackPosition = binding.videoPreview.currentPosition.coerceAtLeast(0)
        runCatching { binding.videoPreview.pause() }
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_PLAYBACK_POSITION, playbackPosition)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        runCatching { binding.videoPreview.stopPlayback() }
        super.onDestroy()
    }

    private fun resolveVideoUri(): Uri? {
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        if (!filePath.isNullOrBlank()) {
            return Uri.fromFile(File(filePath))
        }
        return IntentCompat.getParcelableExtra(intent, EXTRA_URI, Uri::class.java)
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    companion object {
        private const val EXTRA_URI = "video_uri"
        private const val EXTRA_FILE_PATH = "video_file_path"
        private const val EXTRA_TITLE = "video_title"
        private const val STATE_PLAYBACK_POSITION = "state_playback_position"
        private const val CONTROLS_TIMEOUT_MS = 2_500

        fun createIntent(context: Context, uri: Uri, title: String): Intent {
            return Intent(context, VideoPreviewActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                if (uri.scheme == "file") {
                    putExtra(EXTRA_FILE_PATH, uri.path)
                } else {
                    putExtra(EXTRA_URI, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }
    }
}
