package com.aetherquorion.cleansuperai.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aetherquorion.cleansuperai.MainActivity
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.databinding.FragmentProfileBinding
import com.aetherquorion.cleansuperai.databinding.ViewToolItemBinding
import java.io.File
import java.util.Locale

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        configureRow(
            binding.rowLanguage,
            R.drawable.ic_flag,
            R.string.settings_language_title,
            R.string.settings_language_subtitle,
        ) {
            (activity as? MainActivity)?.openLanguageSettings()
        }

        configureRow(
            binding.rowAbout,
            R.drawable.ic_about,
            R.string.settings_about_title,
            R.string.settings_about_subtitle,
        ) { showAboutDialog() }

        configureRow(
            binding.rowPrivacyPolicy,
            R.drawable.ic_shield,
            R.string.settings_privacy_policy_title,
            R.string.settings_privacy_policy_subtitle,
        ) { showPrivacyPolicy() }

        configureRow(
            binding.rowClearCache,
            R.drawable.ic_delete,
            R.string.settings_clear_cache_title,
            R.string.settings_clear_cache_subtitle,
        ) { confirmClearCache() }

        renderLanguageSummary()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            renderLanguageSummary()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun configureRow(
        row: ViewToolItemBinding,
        iconRes: Int,
        titleRes: Int,
        subtitleRes: Int,
        onClick: () -> Unit,
    ) {
        row.ivToolIcon.setImageResource(iconRes)
        row.tvToolTitle.setText(titleRes)
        row.tvToolSubtitle.setText(subtitleRes)
        row.root.setOnClickListener { onClick() }
    }

    private fun showAboutDialog() {
        val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        val versionName = packageInfo.versionName.orEmpty()
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_about_title)
            .setMessage(getString(R.string.settings_about_message_format, versionName, versionCode))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showPrivacyPolicy() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_privacy_policy_title)
            .setMessage(R.string.settings_privacy_policy_body)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun confirmClearCache() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_clear_cache_title)
            .setMessage(R.string.settings_clear_cache_confirm)
            .setNegativeButton(R.string.delete_dialog_cancel, null)
            .setPositiveButton(R.string.delete_dialog_confirm) { _, _ ->
                val freed = clearAppCache()
                val sizeText = formatBytes(freed)
                showToast(getString(R.string.settings_clear_cache_done_format, sizeText))
            }
            .show()
    }

    private fun clearAppCache(): Long {
        var freed = 0L
        freed += deleteRecursively(requireContext().cacheDir)
        requireContext().externalCacheDir?.let { freed += deleteRecursively(it) }
        return freed
    }

    private fun deleteRecursively(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        var freed = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                freed += deleteRecursively(child)
            }
        } else {
            freed += file.length()
        }
        file.delete()
        return freed
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.1f MB", mb)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun renderLanguageSummary() {
        binding.rowLanguage.tvToolSubtitle.text = AppLanguage.currentSelectionSummary(requireContext())
    }
}
