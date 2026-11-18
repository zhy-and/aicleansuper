package com.aetherquorion.cleansuperai.ui.tools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.aetherquorion.cleansuperai.MainActivity
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.databinding.FragmentToolsBinding
import com.aetherquorion.cleansuperai.databinding.ViewToolItemBinding
import com.aetherquorion.cleansuperai.databinding.ViewToolTileBinding
import com.aetherquorion.cleansuperai.ui.clean.CleanCenterFragment
import com.aetherquorion.cleansuperai.ui.vault.VaultFragment
import com.aetherquorion.cleansuperai.widget.WidgetPinHelper

class ToolsFragment : Fragment() {
    private var _binding: FragmentToolsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnPremium.isVisible = false
        binding.btnSettings.setOnClickListener { (activity as? MainActivity)?.openProfile() }
        binding.cardWidgetHero.setOnClickListener {
            WidgetPinHelper.requestPin(requireContext(), "tools_hero")
        }

        configure(
            binding.toolFileManager,
            R.string.tool_file_manager,
            R.string.tool_file_manager_desc,
            R.drawable.ic_storage_permission_file,
            R.drawable.bg_home_feature_icon_green,
        )
        configure(
            binding.toolMediaCompressor,
            R.string.tool_media_compressor,
            R.string.tool_media_compressor_desc,
            R.drawable.ic_nav_compress,
            R.drawable.bg_home_feature_icon_blue,
        )
        configure(
            binding.toolAppManager,
            R.string.tool_app_manager,
            R.string.tool_app_manager_desc,
            R.drawable.ic_tool_apps_grid,
            R.drawable.bg_home_feature_icon_orange,
        )
        configure(
            binding.toolCalendar,
            R.string.tool_clean_calendar,
            R.string.tool_clean_calendar_desc,
            R.drawable.ic_calendar,
            R.drawable.bg_home_feature_icon_teal,
        )
        configure(
            binding.toolEnhancer,
            R.string.tool_ai_enhancer,
            R.string.tool_ai_enhancer_desc,
            R.drawable.ic_enhance,
            R.drawable.bg_home_feature_icon_pink,
        )
        configure(
            binding.toolSpeedTest,
            R.string.tool_speed_test,
            R.string.tool_speed_test_desc,
            R.drawable.ic_speed,
            R.drawable.bg_home_feature_icon_blue,
        )
        configure(binding.toolPrivateVault, R.string.tool_private_vault, R.string.tool_private_vault_desc,
            R.drawable.ic_lock, R.drawable.bg_home_feature_icon_pink)

        binding.toolFileManager.root.setOnClickListener {
            (activity as? MainActivity)?.openDetail(CleanCenterFragment(), "clean_center")
        }
        binding.toolMediaCompressor.root.setOnClickListener {
            (activity as? MainActivity)?.selectDestination(R.id.menu_compress)
        }
        binding.toolAppManager.root.setOnClickListener {
            (activity as? MainActivity)?.openDetail(AppManagerFragment(), "app_manager")
        }
        binding.toolCalendar.root.setOnClickListener {
            (activity as? MainActivity)?.openDetail(CleanCalendarFragment(), "clean_calendar")
        }
        binding.toolEnhancer.root.setOnClickListener {
            (activity as? MainActivity)?.openDetail(ImageEnhancerFragment(), "image_enhancer")
        }
        binding.toolSpeedTest.root.setOnClickListener {
            (activity as? MainActivity)?.openDetail(SpeedTestFragment(), "speed_test")
        }
        binding.toolPrivateVault.root.setOnClickListener {
            (activity as? MainActivity)?.openDetail(VaultFragment(), "vault")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun configure(
        item: ViewToolItemBinding,
        titleRes: Int,
        subtitleRes: Int,
        iconRes: Int,
        iconBackgroundRes: Int,
    ) {
        item.tvToolTitle.setText(titleRes)
        item.tvToolSubtitle.isVisible = true
        item.tvToolSubtitle.setText(subtitleRes)
        item.tvToolBadge.isVisible = false
        item.ivToolIcon.setImageResource(iconRes)
        item.iconContainer.setBackgroundResource(iconBackgroundRes)
    }

    private fun configure(
        item: ViewToolTileBinding,
        titleRes: Int,
        subtitleRes: Int,
        iconRes: Int,
        iconBackgroundRes: Int,
    ) {
        item.tvToolTitle.setText(titleRes)
        item.tvToolSubtitle.setText(subtitleRes)
        item.ivToolIcon.setImageResource(iconRes)
        item.iconContainer.setBackgroundResource(iconBackgroundRes)
    }
}
