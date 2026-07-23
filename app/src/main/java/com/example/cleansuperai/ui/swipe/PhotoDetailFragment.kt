package com.example.cleansuperai.ui.swipe

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.example.cleansuperai.R
import com.example.cleansuperai.databinding.FragmentPhotoDetailBinding

class PhotoDetailFragment : Fragment() {
    private var _binding: FragmentPhotoDetailBinding? = null
    private val binding get() = _binding!!
    private var position = 0
    private lateinit var monthLabel: String
    private var photoUris: List<Uri> = emptyList()
    private var pendingDeleteUris: List<Uri> = emptyList()

    private val deleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                clearReviewBin()
                Toast.makeText(requireContext(), R.string.selected_photos_deleted, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        monthLabel = requireArguments().getString(ARG_MONTH).orEmpty()
        photoUris = PhotoSelectionStore.take()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPhotoDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnShare.setOnClickListener { shareCurrent() }
        binding.btnKeep.setOnClickListener { advance() }
        binding.btnMoveToBin.setOnClickListener {
            currentUri()?.let(::addToReviewBin)
            advance()
        }
        binding.btnClearPending.setOnClickListener {
            deletePendingPhotos()
        }
        render()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun currentUri(): Uri? = photoUris.getOrNull(position)

    private fun render() {
        if (photoUris.isEmpty()) {
            binding.tvTitle.text = monthLabel
            binding.imagePhoto.setImageDrawable(null)
            binding.btnKeep.isEnabled = false
            binding.btnMoveToBin.isEnabled = false
            return
        }
        binding.tvTitle.text = "$monthLabel · ${position + 1}/${photoUris.size}"
        binding.imagePhoto.setImageURI(currentUri())
        renderPending()
    }

    private fun advance() {
        if (photoUris.isEmpty()) return
        position = (position + 1).coerceAtMost(photoUris.lastIndex)
        render()
    }

    private fun shareCurrent() {
        val uri = currentUri() ?: return
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                getString(R.string.share_photo),
            ),
        )
    }

    private fun addToReviewBin(uri: Uri) {
        val updated = preferences().getStringSet(KEY_PENDING, emptySet()).orEmpty().toMutableSet()
        updated += uri.toString()
        preferences().edit { putStringSet(KEY_PENDING, updated) }
        Toast.makeText(requireContext(), R.string.added_to_review_bin, Toast.LENGTH_SHORT).show()
    }

    private fun deletePendingPhotos() {
        val uris = preferences().getStringSet(KEY_PENDING, emptySet())
            .orEmpty()
            .map(Uri::parse)
        if (uris.isEmpty()) return
        pendingDeleteUris = uris

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = MediaStore.createDeleteRequest(requireContext().contentResolver, uris)
            deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        } else {
            val deleted = uris.sumOf { uri ->
                runCatching { requireContext().contentResolver.delete(uri, null, null) }.getOrDefault(0)
            }
            if (deleted > 0) {
                clearReviewBin()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.photos_deleted_format, deleted),
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                Toast.makeText(requireContext(), R.string.delete_photos_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearReviewBin() {
        val deletedUriStrings = pendingDeleteUris.mapTo(mutableSetOf(), Uri::toString)
        preferences().edit { remove(KEY_PENDING) }
        photoUris = photoUris.filterNot { it.toString() in deletedUriStrings }
        pendingDeleteUris = emptyList()
        position = position.coerceAtMost(photoUris.lastIndex.coerceAtLeast(0))
        render()
    }

    private fun renderPending() {
        val count = preferences().getStringSet(KEY_PENDING, emptySet()).orEmpty().size
        binding.tvPending.text = getString(R.string.review_bin_pending_format, count)
    }

    private fun preferences() =
        requireContext().getSharedPreferences("photo_review_bin", 0)

    companion object {
        private const val ARG_MONTH = "month"
        private const val KEY_PENDING = "pending_uris"

        fun newInstance(month: MediaMonth) = PhotoDetailFragment().apply {
            PhotoSelectionStore.set(month.photos.map(MediaPhoto::uri))
            arguments = Bundle().apply {
                putString(ARG_MONTH, month.label)
            }
        }
    }
}

private object PhotoSelectionStore {
    private var selectedUris: List<Uri> = emptyList()

    fun set(uris: List<Uri>) {
        selectedUris = uris
    }

    fun take(): List<Uri> = selectedUris
}
