package com.aetherquorion.cleansuperai.ui.tools

import android.content.Intent
import android.app.AlertDialog
import android.app.usage.UsageStatsManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.Formatter
import android.view.LayoutInflater
import android.widget.PopupMenu
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.databinding.FragmentAppManagerBinding
import com.aetherquorion.cleansuperai.databinding.ItemAppManagerBinding
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val label: String,
    val packageName: String,
    val icon: Drawable?,
    val sizeBytes: Long,
    val installedAt: Long = 0L,
    val lastUsedAt: Long = 0L,
)

class AppManagerFragment : Fragment() {
    private var _binding: FragmentAppManagerBinding? = null
    private val binding get() = _binding!!
    private val adapter = AppManagerAdapter(
        onInfo = { openAppSettings(it.packageName) },
        onSelectionChanged = { updateUninstallButton() },
    )
    private var apps = emptyList<InstalledAppItem>()
    private var sort = AppSort.SIZE
    private val uninstallQueue = ArrayDeque<String>()
    private val uninstallLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (uninstallQueue.isNotEmpty()) launchNextUninstall() else loadApps()
    }

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
        binding.btnUninstall.setOnClickListener { confirmUninstall() }
        updateUninstallButton()
        binding.tvSubtitle.setOnClickListener { anchor ->
            PopupMenu(requireContext(), anchor).apply {
                AppSort.entries.forEach { menu.add(it.ordinal, it.ordinal, it.ordinal, it.name.replace('_', ' ')) }
                setOnMenuItemClickListener { item ->
                    sort = AppSort.entries[item.itemId]
                    adapter.submit(sortApps(apps, sort))
                    true
                }
                show()
            }
        }
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
            this@AppManagerFragment.apps = apps
            adapter.submit(sortApps(apps, sort))
            binding.tvSummary.text = getString(R.string.app_manager_summary_format, apps.size)
        }
    }

    private fun queryUserApps(): List<InstalledAppItem> {
        val pm = requireContext().packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val now = System.currentTimeMillis()
        val usage = runCatching {
            (requireContext().getSystemService(android.content.Context.USAGE_STATS_SERVICE) as? UsageStatsManager)
                ?.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, now - 365L * 24 * 60 * 60 * 1000, now)
                ?.groupBy { it.packageName }
                ?.mapValues { (_, entries) -> entries.maxOfOrNull { it.lastTimeUsed } ?: 0L }
                .orEmpty()
        }.getOrDefault(emptyMap())
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
                val installedAt = runCatching { pm.getPackageInfo(packageName, 0).firstInstallTime }.getOrDefault(0L)
                InstalledAppItem(label, packageName, icon, sizeBytes, installedAt, usage[packageName] ?: 0L)
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

    private fun updateUninstallButton() {
        if (_binding != null) {
            binding.btnUninstall.isEnabled = adapter.selectedPackages().isNotEmpty()
            binding.btnUninstall.text = "Uninstall selected (${adapter.selectedPackages().size})"
        }
    }

    private fun confirmUninstall() {
        val packages = adapter.selectedPackages()
        if (packages.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setMessage("Uninstall ${packages.size} selected apps? Android will ask you to confirm each app.")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Continue") { _, _ ->
                uninstallQueue.clear()
                uninstallQueue.addAll(packages)
                launchNextUninstall()
            }.show()
    }

    private fun launchNextUninstall() {
        val packageName = uninstallQueue.removeFirstOrNull()
        if (packageName == null) {
            loadApps()
            return
        }
        uninstallLauncher.launch(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")))
    }
}

private class AppManagerAdapter(
    private val onInfo: (InstalledAppItem) -> Unit,
    private val onSelectionChanged: () -> Unit,
) : RecyclerView.Adapter<AppManagerAdapter.Holder>() {
    private val items = mutableListOf<InstalledAppItem>()
    private val selected = mutableSetOf<String>()

    fun selectedPackages(): List<String> = selected.toList()

    fun submit(apps: List<InstalledAppItem>) {
        items.clear()
        items.addAll(apps)
        selected.retainAll(apps.map { it.packageName }.toSet())
        notifyDataSetChanged()
        onSelectionChanged()
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
            binding.checkSelected.setOnCheckedChangeListener(null)
            binding.checkSelected.isChecked = item.packageName in selected
            binding.checkSelected.setOnCheckedChangeListener { _, checked ->
                if (checked) selected += item.packageName else selected -= item.packageName
                onSelectionChanged()
            }
            binding.root.setOnClickListener { binding.checkSelected.isChecked = !binding.checkSelected.isChecked }
            binding.root.setOnLongClickListener { onInfo(item); true }
        }
    }
}
