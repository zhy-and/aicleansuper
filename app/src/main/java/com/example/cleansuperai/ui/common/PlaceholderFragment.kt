package com.example.cleansuperai.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.example.cleansuperai.databinding.FragmentPlaceholderBinding

class PlaceholderFragment : Fragment() {
    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        binding.tvPlaceholderTitle.setText(args.getInt(ARG_TITLE))
        binding.tvPlaceholderSubtitle.setText(args.getInt(ARG_SUBTITLE))
        binding.tvPlaceholderBody.setText(args.getInt(ARG_BODY))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_SUBTITLE = "arg_subtitle"
        private const val ARG_BODY = "arg_body"

        fun newInstance(
            @StringRes titleRes: Int,
            @StringRes subtitleRes: Int,
            @StringRes bodyRes: Int,
        ): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TITLE, titleRes)
                    putInt(ARG_SUBTITLE, subtitleRes)
                    putInt(ARG_BODY, bodyRes)
                }
            }
        }
    }
}
