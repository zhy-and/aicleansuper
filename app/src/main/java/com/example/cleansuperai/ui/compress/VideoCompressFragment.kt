package com.example.cleansuperai.ui.compress

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
import com.example.cleansuperai.R
import com.example.cleansuperai.databinding.FragmentVideoCompressBinding
import com.example.cleansuperai.ui.cleaner.CompressionResult
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoCompressFragment : Fragment() {
    private var _binding: FragmentVideoCompressBinding? = null
    private val binding get() = _binding!!
    private var selectedUri: Uri? = null
    private var originalBytes: Long = 0

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
        binding.btnCompress.setOnClickListener { compressSelected() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showSelection(uri: Uri) {
        selectedUri = uri
        originalBytes = requireContext().contentResolver.openAssetFileDescriptor(uri, "r")
            ?.use { it.length.coerceAtLeast(0) } ?: 0
        binding.emptyGuide.isVisible = false
        binding.imagePreview.isVisible = true
        binding.imagePreview.load(uri)
        binding.tvCompressionResult.isVisible = true
        binding.tvCompressionResult.text = getString(
            R.string.original_size_format,
            Formatter.formatFileSize(requireContext(), originalBytes),
        )
        binding.btnCompress.isEnabled = true
        binding.progressCompress.isVisible = false
        binding.progressCompress.progress = 0
    }

    private fun compressSelected() {
        val uri = selectedUri ?: return
        binding.btnCompress.isEnabled = false
        binding.btnChooseVideo.isEnabled = false
        binding.btnCompress.text = getString(R.string.compressing)
        binding.progressCompress.isVisible = true
        binding.progressCompress.progress = 0
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val compressedFile = VideoCompressor.compress(
                    context = requireContext(),
                    inputUri = uri,
                    onProgress = { progress ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            _binding?.progressCompress?.progress = (progress * 100).toInt()
                        }
                    },
                )
                withContext(Dispatchers.IO) {
                    val compressedBytes = compressedFile.length()
                    saveToMediaStore(compressedFile)
                    compressedFile.delete()
                    CompressionResult(originalBytes, compressedBytes)
                }
            }.onSuccess { result ->
                val compressed = Formatter.formatFileSize(requireContext(), result.compressedBytes)
                val saved = Formatter.formatFileSize(requireContext(), result.savedBytes)
                binding.tvCompressionResult.isVisible = true
                binding.tvCompressionResult.text =
                    getString(R.string.compressed_result_format, compressed, saved)
                Toast.makeText(requireContext(), R.string.video_compression_saved, Toast.LENGTH_SHORT)
                    .show()
            }.onFailure {
                Toast.makeText(requireContext(), R.string.video_compression_failed, Toast.LENGTH_SHORT)
                    .show()
            }
            binding.btnCompress.isEnabled = selectedUri != null
            binding.btnChooseVideo.isEnabled = true
            binding.btnCompress.text = getString(R.string.compress_and_save)
            binding.progressCompress.isVisible = false
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
}
