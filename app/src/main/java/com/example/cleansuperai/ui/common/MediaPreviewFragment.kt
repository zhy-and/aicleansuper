package com.example.cleansuperai.ui.common

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import coil.load
import com.example.cleansuperai.R
import com.example.cleansuperai.databinding.FragmentMediaPreviewBinding

class MediaPreviewFragment : Fragment() {
    private var _binding: FragmentMediaPreviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMediaPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uri = BundleCompat.getParcelable(requireArguments(), ARG_URI, Uri::class.java)
        val title = requireArguments().getString(ARG_TITLE).orEmpty()
        binding.tvTitle.text = title.ifBlank { getString(R.string.media_preview_title) }
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        if (uri != null) {
            binding.imagePreview.load(uri) {
                crossfade(true)
                placeholder(R.drawable.ic_media_placeholder)
                error(R.drawable.ic_media_placeholder)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_URI = "arg_uri"
        private const val ARG_TITLE = "arg_title"

        fun newInstance(uri: Uri, title: String): MediaPreviewFragment {
            return MediaPreviewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_URI, uri)
                    putString(ARG_TITLE, title)
                }
            }
        }
    }
}
