package com.example.cleansuperai.ui.tools

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cleansuperai.R
import com.example.cleansuperai.databinding.FragmentAppManagerBinding
import com.example.cleansuperai.databinding.ItemAppManagerBinding
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val label: String,
    val packageName: String,
    val icon: Drawable?,
    val sizeBytes: Long,
)

class AppManagerFragment : Fragment() {
    private var _binding: FragmentAppManagerBinding? = null
    private val binding get() = _binding!!
    private val adapter = AppManagerAdapter { openAppSettings(it.packageName) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAppManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.recyclerApps.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerApps.adapter = adapter
        loadApps()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadApps() {
        binding.progressLoading.isVisible = true
        binding.recyclerApps.isVisible = false
        viewLifecycleOwner.lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) { queryUserApps() }
            binding.progressLoading.isVisible = false
            binding.recyclerApps.isVisible = true
            adapter.submit(apps)
            binding.tvSummary.text = getString(R.string.app_manager_summary_format, apps.size)
        }
    }

    private fun queryUserApps(): List<InstalledAppItem> {
        val pm = requireContext().packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolve ->
                val packageName = resolve.activityInfo.packageName
                val appInfo = runCatching {
                    pm.getApplicationInfo(packageName, 0)
                }.getOrNull() ?: return@mapNotNull null
                if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
                    appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0
                ) {
                    return@mapNotNull null
                }
                val label = resolve.loadLabel(pm)?.toString().orEmpty().ifBlank { packageName }
                val icon = runCatching { resolve.loadIcon(pm) }.getOrNull()
                val sizeBytes = runCatching {
                    File(appInfo.sourceDir).length()
                }.getOrDefault(0L)
                InstalledAppItem(label, packageName, icon, sizeBytes)
            }
            .distinctBy { it.packageName }
            .sortedByDescending { it.sizeBytes }
    }

    private fun openAppSettings(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}

private class AppManagerAdapter(
    private val onClick: (InstalledAppItem) -> Unit,
) : RecyclerView.Adapter<AppManagerAdapter.Holder>() {
    private val items = mutableListOf<InstalledAppItem>()

    fun submit(apps: List<InstalledAppItem>) {
        items.clear()
        items.addAll(apps)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemAppManagerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(
        private val binding: ItemAppManagerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InstalledAppItem) {
            binding.tvAppName.text = item.label
            binding.tvPackageName.text = item.packageName
            binding.imageIcon.setImageDrawable(item.icon)
            binding.tvAppSize.text = Formatter.formatFileSize(binding.root.context, item.sizeBytes)
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
