package com.aetherquorion.cleansuperai.ui.compress

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.aetherquorion.cleansuperai.MainActivity
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.databinding.FragmentCompressBinding
import com.aetherquorion.cleansuperai.ui.cleaner.CompressionResult
import com.aetherquorion.cleansuperai.ui.common.MediaActions
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class CompressFragment : Fragment() {
    private var _binding: FragmentCompressBinding? = null
    private val binding get() = _binding!!
    private var selectedUri: Uri? = null
    private var originalBytes: Long = 0
    private var compressedPreviewBytes: ByteArray? = null
    private var compressedPreviewBitmap: Bitmap? = null
    private var compressedPreviewFile: File? = null

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::showSelection)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCompressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnPremium.isVisible = false
        binding.btnSettings.setOnClickListener { (activity as? MainActivity)?.openProfile() }
        binding.cardVideo.setOnClickListener {
            (activity as? MainActivity)?.openDetail(VideoCompressFragment(), "video_compress")
        }
        binding.btnChoosePhoto.setOnClickListener { picker.launch("image/*") }
        binding.emptyGuide.setOnClickListener { picker.launch("image/*") }
        binding.btnSaveCompressed.setOnClickListener { saveCompressedPreview() }
        binding.cardBeforePreview.setOnClickListener {
            selectedUri?.let { uri ->
                MediaActions.openImagePreview(this, uri, getString(R.string.preview_original_photo_label))
            }
        }
        binding.cardAfterPreview.setOnClickListener {
            compressedPreviewFile?.let { file ->
                MediaActions.openImagePreview(
                    this,
                    Uri.fromFile(file),
                    getString(R.string.preview_compressed_photo_label),
                )
            }
        }
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
        binding.imagePreviewBefore.setImageURI(uri)
        binding.tvAfterLabel.isVisible = false
        binding.cardAfterPreview.isVisible = false
        binding.tvCompressionResult.isVisible = true
        binding.tvCompressionResult.text =
            getString(
                R.string.original_size_format,
                Formatter.formatFileSize(requireContext(), originalBytes),
            )
        binding.btnSaveCompressed.isEnabled = false
        compressSelected()
    }

    private fun compressSelected() {
        val uri = selectedUri ?: return
        clearCompressedPreview()
        binding.btnChoosePhoto.isEnabled = false
        binding.progressCompress.isVisible = true
        binding.tvCompressionResult.text =
            buildString {
                append(
                    getString(
                        R.string.original_size_format,
                        Formatter.formatFileSize(requireContext(), originalBytes),
                    ),
                )
                append('\n')
                append(getString(R.string.compressing))
            }
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val bitmap = decodeScaledBitmap(uri)
                        ?: error("Unsupported image")
                    val output = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, output)
                    if (!bitmap.isRecycled) bitmap.recycle()
                    val bytes = output.toByteArray()
                    val previewBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: error("Preview decode failed")
                    val previewFile = writeCompressedPreviewToCache(bytes)
                    CompressionPreview(bytes, previewBitmap, previewFile, CompressionResult(originalBytes, bytes.size.toLong()))
                }
            }.onSuccess { preview ->
                clearCompressedPreview()
                compressedPreviewBytes = preview.bytes
                compressedPreviewBitmap = preview.bitmap
                compressedPreviewFile = preview.file
                binding.tvAfterLabel.isVisible = true
                binding.cardAfterPreview.isVisible = true
                binding.imagePreviewAfter.setImageBitmap(preview.bitmap)
                val compressed = Formatter.formatFileSize(requireContext(), preview.result.compressedBytes)
                val saved = Formatter.formatFileSize(requireContext(), preview.result.savedBytes)
                binding.tvCompressionResult.isVisible = true
                binding.tvCompressionResult.text =
                    getString(R.string.compressed_result_format, compressed, saved)
                binding.btnSaveCompressed.isEnabled = true
            }.onFailure {
                binding.tvCompressionResult.text =
                    getString(
                        R.string.original_size_format,
                        Formatter.formatFileSize(requireContext(), originalBytes),
                    )
                Toast.makeText(requireContext(), R.string.compression_failed, Toast.LENGTH_SHORT).show()
            }
            binding.btnChoosePhoto.isEnabled = true
            binding.progressCompress.isVisible = false
        }
    }

    private fun saveCompressedPreview() {
        val bytes = compressedPreviewBytes ?: return
        binding.btnSaveCompressed.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    saveToMediaStore(bytes)
                }
            }.onSuccess {
                Toast.makeText(requireContext(), R.string.compression_saved, Toast.LENGTH_SHORT).show()
            }.onFailure {
                binding.btnSaveCompressed.isEnabled = true
                Toast.makeText(requireContext(), R.string.compression_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun decodeScaledBitmap(uri: Uri): Bitmap? {
        val resolver = requireContext().contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun saveToMediaStore(bytes: ByteArray) {
        val resolver = requireContext().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "CleanSuperAI_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/CleanSuperAI",
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val target = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create output")
        try {
            resolver.openOutputStream(target)?.use { it.write(bytes) }
                ?: error("Could not write output")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(
                    target,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null,
                )
            }
        } catch (throwable: Throwable) {
            resolver.delete(target, null, null)
            throw throwable
        }
    }

    private fun writeCompressedPreviewToCache(bytes: ByteArray): File {
        val file = File(requireContext().cacheDir, "compressed_photo_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { it.write(bytes) }
        return file
    }

    private fun clearCompressedPreview() {
        compressedPreviewBytes = null
        compressedPreviewBitmap?.takeIf { !it.isRecycled }?.recycle()
        compressedPreviewBitmap = null
        compressedPreviewFile?.takeIf { it.exists() }?.delete()
        compressedPreviewFile = null
        _binding?.tvAfterLabel?.isVisible = false
        _binding?.cardAfterPreview?.isVisible = false
        _binding?.imagePreviewAfter?.setImageDrawable(null)
        _binding?.btnSaveCompressed?.isEnabled = false
        _binding?.progressCompress?.isVisible = false
    }

    private data class CompressionPreview(
        val bytes: ByteArray,
        val bitmap: Bitmap,
        val file: File,
        val result: CompressionResult,
    )
}
