package com.example.cleansuperai.ui.tools

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cleansuperai.R
import com.example.cleansuperai.databinding.FragmentCleanCalendarBinding
import com.example.cleansuperai.databinding.ItemCalendarEventBinding
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CalendarEventItem(
    val id: Long,
    val title: String,
    val startMs: Long,
    val calendarName: String,
)

class CleanCalendarFragment : Fragment() {
    private var _binding: FragmentCleanCalendarBinding? = null
    private val binding get() = _binding!!
    private val selectedIds = linkedSetOf<Long>()
    private val adapter = CalendarEventAdapter(
        isSelected = { selectedIds.contains(it.id) },
        onToggle = ::toggleSelection,
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.READ_CALENDAR] == true &&
            grants[Manifest.permission.WRITE_CALENDAR] == true
        if (granted) {
            loadEvents()
        } else {
            showPermissionUi()
            Toast.makeText(requireContext(), R.string.calendar_permission_denied, Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCleanCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.recyclerEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerEvents.adapter = adapter
        binding.btnGrant.setOnClickListener { requestCalendarPermission() }
        binding.btnDeleteSelected.setOnClickListener { confirmDelete() }
        if (hasCalendarPermission()) {
            loadEvents()
        } else {
            showPermissionUi()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun hasCalendarPermission(): Boolean {
        val read = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
        return read && write
    }

    private fun requestCalendarPermission() {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
        )
    }

    private fun showPermissionUi() {
        binding.btnGrant.isVisible = true
        binding.btnDeleteSelected.isVisible = false
        binding.recyclerEvents.isVisible = false
        binding.progressLoading.isVisible = false
        binding.tvSummary.setText(R.string.calendar_permission_hint)
    }

    private fun loadEvents() {
        binding.btnGrant.isVisible = false
        binding.btnDeleteSelected.isVisible = true
        binding.progressLoading.isVisible = true
        binding.recyclerEvents.isVisible = false
        val untitled = getString(R.string.calendar_untitled)
        viewLifecycleOwner.lifecycleScope.launch {
            val events = withContext(Dispatchers.IO) { queryPastEvents(untitled) }
            selectedIds.clear()
            binding.progressLoading.isVisible = false
            binding.recyclerEvents.isVisible = true
            adapter.submit(events)
            updateSummary(events.size)
        }
    }

    private fun queryPastEvents(untitledTitle: String): List<CalendarEventItem> {
        val resolver = requireContext().contentResolver
        val now = System.currentTimeMillis()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
        )
        val selection = "${CalendarContract.Events.DTEND} < ? OR " +
            "(${CalendarContract.Events.DTEND} IS NULL AND ${CalendarContract.Events.DTSTART} < ?)"
        val args = arrayOf(now.toString(), now.toString())
        val items = mutableListOf<CalendarEventItem>()
        resolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            args,
            "${CalendarContract.Events.DTSTART} DESC",
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val startIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val calIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                items += CalendarEventItem(
                    id = cursor.getLong(idIdx),
                    title = cursor.getString(titleIdx)?.ifBlank { null } ?: untitledTitle,
                    startMs = cursor.getLong(startIdx),
                    calendarName = cursor.getString(calIdx).orEmpty(),
                )
                if (items.size >= 200) break
            }
        }
        return items
    }

    private fun toggleSelection(item: CalendarEventItem) {
        if (!selectedIds.add(item.id)) {
            selectedIds.remove(item.id)
        }
        adapter.notifyDataSetChanged()
        binding.btnDeleteSelected.isEnabled = selectedIds.isNotEmpty()
        binding.tvSummary.text = getString(
            R.string.clean_calendar_summary_format,
            adapter.itemCount,
            selectedIds.size,
        )
    }

    private fun updateSummary(total: Int) {
        binding.btnDeleteSelected.isEnabled = selectedIds.isNotEmpty()
        binding.tvSummary.text = if (total == 0) {
            getString(R.string.clean_calendar_empty)
        } else {
            getString(R.string.clean_calendar_summary_format, total, selectedIds.size)
        }
    }

    private fun confirmDelete() {
        if (selectedIds.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.clean_calendar_delete)
            .setMessage(getString(R.string.clean_calendar_delete_confirm, selectedIds.size))
            .setNegativeButton(R.string.delete_dialog_cancel, null)
            .setPositiveButton(R.string.delete_dialog_confirm) { _, _ -> deleteSelected() }
            .show()
    }

    private fun deleteSelected() {
        val ids = selectedIds.toList()
        viewLifecycleOwner.lifecycleScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                var count = 0
                ids.forEach { id ->
                    val rows = requireContext().contentResolver.delete(
                        CalendarContract.Events.CONTENT_URI,
                        "${CalendarContract.Events._ID}=?",
                        arrayOf(id.toString()),
                    )
                    if (rows > 0) count++
                }
                count
            }
            Toast.makeText(
                requireContext(),
                getString(R.string.clean_calendar_delete_done, deleted),
                Toast.LENGTH_SHORT,
            ).show()
            loadEvents()
        }
    }
}

private class CalendarEventAdapter(
    private val isSelected: (CalendarEventItem) -> Boolean,
    private val onToggle: (CalendarEventItem) -> Unit,
) : RecyclerView.Adapter<CalendarEventAdapter.Holder>() {
    private val items = mutableListOf<CalendarEventItem>()

    fun submit(events: List<CalendarEventItem>) {
        items.clear()
        items.addAll(events)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemCalendarEventBinding.inflate(
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
        private val binding: ItemCalendarEventBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CalendarEventItem) {
            binding.tvTitle.text = item.title
            val date = DateFormat.getMediumDateFormat(binding.root.context).format(Date(item.startMs))
            val time = DateFormat.getTimeFormat(binding.root.context).format(Date(item.startMs))
            binding.tvMeta.text = listOfNotNull(
                "$date · $time",
                item.calendarName.takeIf { it.isNotBlank() },
            ).joinToString("\n")
            binding.checkSelected.isChecked = isSelected(item)
            binding.root.setOnClickListener { onToggle(item) }
        }
    }
}
