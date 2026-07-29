package com.aetherquorion.cleansuperai.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.aetherquorion.cleansuperai.databinding.FragmentLanguageSettingsBinding

class LanguageSettingsFragment : Fragment() {
    private var _binding: FragmentLanguageSettingsBinding? = null
    private val binding get() = _binding!!

    private val adapter = LanguageAdapter(::applyLanguage)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLanguageSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.recyclerLanguages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLanguages.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        adapter.submitList(AppLanguage.supported, AppLanguage.selectedTag())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerLanguages.adapter = null
        _binding = null
    }

    private fun applyLanguage(language: AppLanguage) {
        if (language.tag == AppLanguage.selectedTag()) return
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.tag.orEmpty()),
        )
    }
}
