package com.example.cleansuperai.ui.tools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.cleansuperai.MainActivity
import com.example.cleansuperai.R
import com.example.cleansuperai.databinding.FragmentToolsBinding
import com.example.cleansuperai.databinding.ViewToolItemBinding
import com.example.cleansuperai.ui.clean.CleanCenterFragment

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

        configure(binding.toolFileManager, R.string.tool_file_manager, R.drawable.ic_clean)
        configure(binding.toolMediaCompressor, R.string.tool_media_compressor, R.drawable.ic_nav_compress)
        configure(binding.toolAppManager, R.string.tool_app_manager, R.drawable.ic_apps)
        configure(binding.toolCalendar, R.string.tool_clean_calendar, R.drawable.ic_calendar)
        configure(binding.toolEnhancer, R.string.tool_ai_enhancer, R.drawable.ic_enhance)
        configure(binding.toolSpeedTest, R.string.tool_speed_test, R.drawable.ic_speed)

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun configure(
        item: ViewToolItemBinding,
        titleRes: Int,
        iconRes: Int,
    ) {
        item.tvToolTitle.setText(titleRes)
        item.tvToolSubtitle.isVisible = false
        item.ivToolIcon.setImageResource(iconRes)
    }
}
