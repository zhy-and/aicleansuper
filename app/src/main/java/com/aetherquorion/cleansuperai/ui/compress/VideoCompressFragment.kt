package com.aetherquorion.cleansuperai.ui.compress

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.databinding.FragmentVideoCompressBinding
import com.aetherquorion.cleansuperai.ui.cleaner.CompressionResult
import com.aetherquorion.cleansuperai.ui.common.MediaActions
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class VideoCompressFragment : Fragment() {
    private var _binding: FragmentVideoCompressBinding? = null
    private val binding get() = _binding!!
    private var selectedUri: Uri? = null
    private var originalBytes: Long = 0
    private var compressedPreviewFile: File? = null

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::showSelection)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVideoCompressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnChooseVideo.setOnClickListener { picker.launch("video/*") }
        binding.emptyGuide.setOnClickListener { picker.launch("video/*") }
        binding.btnSaveCompressed.setOnClickListener { saveCompressedPreview() }
        binding.videoPreviewBefore.setOnClickListener { openOriginalVideoPreview() }
        binding.btnPlayBefore.setOnClickListener { openOriginalVideoPreview() }
        binding.videoPreviewAfter.setOnClickListener { openCompressedVideoPreview() }
        binding.btnPlayAfter.setOnClickListener { openCompressedVideoPreview() }
    }

    override fun onDestroyView() {
        clearCompressedPreview()
        super.onDestroyView()
        _binding = null
    }

    private fun showSelection(uri: Uri) {
        selectedUri = uri
        clearCompressedPreview()
        originalBytes = requireContext().contentResolver.openAssetFileDescriptor(uri, "r")
            ?.use { it.length.coerceAtLeast(0) } ?: 0
        binding.emptyGuide.isVisible = false
        binding.previewContainer.isVisible = true
        binding.tvBeforeLabel.isVisible = true
        binding.cardBeforePreview.isVisible = true
        binding.tvAfterLabel.isVisible = false
        binding.cardAfterPreview.isVisible = false
        bindVideoThumb(binding.imageVideoPreviewBefore, uri)
        binding.tvCompressionResult.isVisible = true
        binding.tvCompressionResult.text = getString(
            R.string.original_size_format,
            Formatter.formatFileSize(requireContext(), originalBytes),
        )
        binding.btnSaveCompressed.isEnabled = false
        binding.progressCompress.isVisible = false
        binding.progressCompress.progress = 0
        compressSelected()
    }

    private fun compressSelected() {
        val uri = selectedUri ?: return
        clearCompressedPreview()
        binding.btnChooseVideo.isEnabled = false
        binding.progressCompress.isVisible = true
        binding.progressCompress.isIndeterminate = true
        binding.progressCompress.progress = 0
        binding.tvCompressionResult.text =
            buildString {
                append(
                    getString(
                        R.string.original_size_format,
                        Formatter.formatFileSize(requireContext(), originalBytes),
                    ),
                )
                append('\n')
                append(getString(R.string.video_compression_preparing))
            }
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withTimeout(VIDEO_COMPRESSION_TIMEOUT_MS) {
                    val compressedFile = VideoCompressor.compress(
                        context = requireContext(),
                        inputUri = uri,
                        onProgress = { progress ->
                            val percent = (progress * 100).toInt().coerceIn(0, 100)
                            viewLifecycleOwner.lifecycleScope.launch {
                                _binding?.progressCompress?.isIndeterminate = false
                                _binding?.progressCompress?.progress = percent
                                if (percent in 1..98) {
                                    _binding?.tvCompressionResult?.text =
                                        buildString {
                                            append(
                                                getString(
                                                    R.string.original_size_format,
                                                    Formatter.formatFileSize(requireContext(), originalBytes),
                                                ),
                                            )
                                            append('\n')
                                            append(
                                                getString(
                                                    R.string.video_compression_progress_format,
                                                    percent,
                                                ),
                                            )
                                        }
                                } else if (percent >= 99) {
                                    _binding?.tvCompressionResult?.text =
                                        buildString {
                                            append(
                                                getString(
                                                    R.string.original_size_format,
                                                    Formatter.formatFileSize(requireContext(), originalBytes),
                                                ),
                                            )
                                            append('\n')
                                            append(getString(R.string.video_compression_finishing))
                                        }
                                }
                            }
                        }
                    )
                    withContext(Dispatchers.IO) {
                        compressedFile to CompressionResult(originalBytes, compressedFile.length())
                    }
                }
            }.onSuccess { (file, result) ->
                clearCompressedPreview()
                compressedPreviewFile = file
                binding.tvAfterLabel.isVisible = true
                binding.cardAfterPreview.isVisible = true
                bindVideoThumb(binding.imageVideoPreviewAfter, Uri.fromFile(file))
                val compressed = Formatter.formatFileSize(requireContext(), result.compressedBytes)
                val saved = Formatter.formatFileSize(requireContext(), result.savedBytes)
                binding.tvCompressionResult.isVisible = true
                binding.tvCompressionResult.text =
                    getString(R.string.compressed_result_format, compressed, saved)
                binding.btnSaveCompressed.isEnabled = true
            }.onFailure {
                clearCompressedPreview()
                binding.tvCompressionResult.text =
                    getString(
                        R.string.original_size_format,
                        Formatter.formatFileSize(requireContext(), originalBytes),
                    )
                val messageRes = if (it is kotlinx.coroutines.TimeoutCancellationException) {
                    R.string.video_compression_timeout
                } else {
                    R.string.video_compression_failed
                }
                Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
            }
            binding.btnChooseVideo.isEnabled = true
            binding.progressCompress.isVisible = false
            binding.progressCompress.isIndeterminate = false
        }
    }

    private fun saveCompressedPreview() {
        val file = compressedPreviewFile ?: return
        binding.btnSaveCompressed.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    saveToMediaStore(file)
                }
            }.onSuccess {
                Toast.makeText(requireContext(), R.string.video_compression_saved, Toast.LENGTH_SHORT)
                    .show()
            }.onFailure {
                binding.btnSaveCompressed.isEnabled = true
                Toast.makeText(requireContext(), R.string.video_compression_failed, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun saveToMediaStore(file: File) {
        val resolver = requireContext().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "CleanSuperAI_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/CleanSuperAI",
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val target = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create output")
        try {
            resolver.openOutputStream(target)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not write output")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(
                    target,
                    ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) },
                    null,
                    null,
                )
            }
        } catch (throwable: Throwable) {
            resolver.delete(target, null, null)
            throw throwable
        }
    }

    private fun openOriginalVideoPreview() {
        selectedUri?.let { uri ->
            MediaActions.openVideoPreview(this, uri, getString(R.string.preview_original_video_label))
        }
    }

    private fun openCompressedVideoPreview() {
        compressedPreviewFile?.let { file ->
            MediaActions.openVideoPreview(
                this,
                Uri.fromFile(file),
                getString(R.string.preview_compressed_video_label),
            )
        }
    }

    private fun bindVideoThumb(imageView: android.widget.ImageView, uri: Uri) {
        imageView.load(uri) {
            crossfade(true)
            placeholder(R.drawable.ic_media_placeholder)
            error(R.drawable.ic_media_placeholder)
        }
    }

    private fun clearCompressedPreview() {
        _binding?.imageVideoPreviewAfter?.setImageDrawable(null)
        compressedPreviewFile?.takeIf { it.exists() }?.delete()
        compressedPreviewFile = null
        _binding?.tvAfterLabel?.isVisible = false
        _binding?.cardAfterPreview?.isVisible = false
        _binding?.btnSaveCompressed?.isEnabled = false
    }

    companion object {
        private const val VIDEO_COMPRESSION_TIMEOUT_MS = 30 * 60 * 1000L
    }
}
