package com.example.cleansuperai.ui.compress

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
import com.example.cleansuperai.MainActivity
import com.example.cleansuperai.R
import com.example.cleansuperai.databinding.FragmentCompressBinding
import com.example.cleansuperai.ui.cleaner.CompressionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class CompressFragment : Fragment() {
    private var _binding: FragmentCompressBinding? = null
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
        binding.imagePreview.setImageURI(uri)
        binding.tvCompressionResult.text =
            getString(
                R.string.original_size_format,
                Formatter.formatFileSize(requireContext(), originalBytes),
            )
        binding.btnCompress.isEnabled = true
    }

    private fun compressSelected() {
        val uri = selectedUri ?: return
        binding.btnCompress.isEnabled = false
        binding.btnCompress.setText(R.string.compressing)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val bitmap = decodeScaledBitmap(uri)
                        ?: error("Unsupported image")
                    val output = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, output)
                    if (!bitmap.isRecycled) bitmap.recycle()
                    val bytes = output.toByteArray()
                    saveToMediaStore(bytes)
                    CompressionResult(originalBytes, bytes.size.toLong())
                }
            }.onSuccess { result ->
                val compressed = Formatter.formatFileSize(requireContext(), result.compressedBytes)
                val saved = Formatter.formatFileSize(requireContext(), result.savedBytes)
                binding.tvCompressionResult.text =
                    getString(R.string.compressed_result_format, compressed, saved)
                Toast.makeText(requireContext(), R.string.compression_saved, Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(requireContext(), R.string.compression_failed, Toast.LENGTH_SHORT).show()
            }
            binding.btnCompress.isEnabled = true
            binding.btnCompress.setText(R.string.compress_and_save)
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
}
