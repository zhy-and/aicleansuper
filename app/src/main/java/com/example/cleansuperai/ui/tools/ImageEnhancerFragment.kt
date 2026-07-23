package com.example.cleansuperai.ui.tools

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cleansuperai.R
import com.example.cleansuperai.databinding.FragmentImageEnhancerBinding
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageEnhancerFragment : Fragment() {
    private var _binding: FragmentImageEnhancerBinding? = null
    private val binding get() = _binding!!
    private var selectedUri: Uri? = null
    private var enhancedBitmap: Bitmap? = null

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::showSelection)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentImageEnhancerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnChoosePhoto.setOnClickListener { picker.launch("image/*") }
        binding.btnEnhance.setOnClickListener { enhanceSelected() }
        binding.btnSave.setOnClickListener { saveEnhanced() }
    }

    override fun onDestroyView() {
        enhancedBitmap?.takeIf { !it.isRecycled }?.recycle()
        enhancedBitmap = null
        super.onDestroyView()
        _binding = null
    }

    private fun showSelection(uri: Uri) {
        selectedUri = uri
        enhancedBitmap?.takeIf { !it.isRecycled }?.recycle()
        enhancedBitmap = null
        binding.imagePreview.setImageURI(uri)
        binding.tvStatus.setText(R.string.image_enhancer_ready)
        binding.btnEnhance.isEnabled = true
        binding.btnSave.isEnabled = false
    }

    private fun enhanceSelected() {
        val uri = selectedUri ?: return
        binding.btnEnhance.isEnabled = false
        binding.btnEnhance.text = getString(R.string.image_enhancer_working)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val source = decodeScaledBitmap(uri) ?: error("Unsupported image")
                    enhanceBitmap(source).also {
                        if (source !== it && !source.isRecycled) source.recycle()
                    }
                }
            }.onSuccess { bitmap ->
                enhancedBitmap?.takeIf { !it.isRecycled }?.recycle()
                enhancedBitmap = bitmap
                binding.imagePreview.setImageBitmap(bitmap)
                binding.tvStatus.setText(R.string.image_enhancer_done)
                binding.btnSave.isEnabled = true
            }.onFailure {
                Toast.makeText(requireContext(), R.string.image_enhancer_failed, Toast.LENGTH_SHORT)
                    .show()
            }
            binding.btnEnhance.isEnabled = selectedUri != null
            binding.btnEnhance.text = getString(R.string.image_enhancer_action)
        }
    }

    private fun saveEnhanced() {
        val bitmap = enhancedBitmap ?: return
        binding.btnSave.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val output = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
                    saveToMediaStore(output.toByteArray())
                }
            }.onSuccess {
                Toast.makeText(requireContext(), R.string.image_enhancer_saved, Toast.LENGTH_SHORT)
                    .show()
            }.onFailure {
                Toast.makeText(requireContext(), R.string.image_enhancer_failed, Toast.LENGTH_SHORT)
                    .show()
            }
            binding.btnSave.isEnabled = enhancedBitmap != null
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

    private fun enhanceBitmap(source: Bitmap): Bitmap {
        val mutable = if (source.config == Bitmap.Config.ARGB_8888 && source.isMutable) {
            source
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }
        val output = Bitmap.createBitmap(mutable.width, mutable.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val matrix = ColorMatrix().apply {
            setSaturation(1.2f)
            val contrast = 1.15f
            val translate = (-0.5f * contrast + 0.5f) * 255f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            postConcat(contrastMatrix)
            val brightness = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 12f,
                    0f, 1f, 0f, 0f, 12f,
                    0f, 0f, 1f, 0f, 12f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            postConcat(brightness)
        }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(mutable, 0f, 0f, paint)
        if (mutable !== source && !mutable.isRecycled) {
            mutable.recycle()
        }
        return sharpen(output)
    }

    private fun sharpen(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val outputPixels = pixels.copyOf()
        val kernel = intArrayOf(0, -1, 0, -1, 5, -1, 0, -1, 0)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0
                var g = 0
                var b = 0
                var k = 0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = pixels[(y + ky) * width + (x + kx)]
                        val weight = kernel[k++]
                        r += ((pixel shr 16) and 0xFF) * weight
                        g += ((pixel shr 8) and 0xFF) * weight
                        b += (pixel and 0xFF) * weight
                    }
                }
                val index = y * width + x
                val alpha = pixels[index] and -0x1000000
                outputPixels[index] = alpha or
                    (r.coerceIn(0, 255) shl 16) or
                    (g.coerceIn(0, 255) shl 8) or
                    b.coerceIn(0, 255)
            }
        }
        val sharpened = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        sharpened.setPixels(outputPixels, 0, width, 0, 0, width, height)
        if (!source.isRecycled) source.recycle()
        return sharpened
    }

    private fun saveToMediaStore(bytes: ByteArray) {
        val resolver = requireContext().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Enhanced_${System.currentTimeMillis()}.jpg")
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
