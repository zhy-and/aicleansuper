package com.aetherquorion.cleansuperai.ui.contacts

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.data.model.DuplicateContactGroup
import com.aetherquorion.cleansuperai.databinding.FragmentContactsCleanupBinding
import com.aetherquorion.cleansuperai.ui.common.ContactsPermissionHelper
import kotlinx.coroutines.launch

class ContactsCleanupFragment : Fragment() {
    private var _binding: FragmentContactsCleanupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactsCleanupViewModel by viewModels()
    private val adapter = DuplicateContactAdapter(
        onMergeClick = ::mergeGroup,
        onViewClick = ::openSystemContact,
    )

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.values.all { it }
            viewModel.updatePermission(granted)
            if (!granted) {
                showToast(getString(R.string.permission_denied_message))
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentContactsCleanupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.adapter = adapter
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnGrantOrRefresh.setOnClickListener {
            if (ContactsPermissionHelper.hasPermissions(requireContext())) {
                viewModel.loadSummary()
            } else {
                permissionLauncher.launch(ContactsPermissionHelper.requiredPermissions())
            }
        }
        binding.btnDeleteEmpty.setOnClickListener { confirmDeleteEmptyContacts() }
        collectUiState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.updatePermission(ContactsPermissionHelper.hasPermissions(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: ContactsUiState) {
        if (!state.hasPermission) {
            binding.tvPermissionTitle.setText(R.string.contacts_permission_title)
            binding.tvPermissionBody.setText(R.string.contacts_permission_body)
            binding.btnGrantOrRefresh.setText(R.string.contacts_load_action)
            binding.tvDuplicateCount.text = "—"
            binding.tvEmptyCount.text = "—"
            binding.btnDeleteEmpty.isEnabled = false
            adapter.submitList(emptyList())
            binding.emptyContainer.isVisible = false
            return
        }

        binding.btnGrantOrRefresh.setText(
            if (state.isLoading) R.string.scan_button_running else R.string.contacts_refresh_action,
        )
        binding.btnGrantOrRefresh.isEnabled = !state.isLoading

        val summary = state.summary
        if (summary == null) {
            binding.tvPermissionTitle.setText(R.string.contacts_scan_title)
            binding.tvPermissionBody.text = state.errorMessage ?: getString(R.string.contacts_scan_body)
            return
        }

        binding.tvPermissionTitle.setText(R.string.contacts_scan_title)
        binding.tvPermissionBody.setText(R.string.contacts_scan_body)
        binding.tvDuplicateCount.text = summary.duplicateGroups.size.toString()
        binding.tvEmptyCount.text = summary.emptyContacts.size.toString()
        binding.btnDeleteEmpty.isEnabled = summary.emptyContacts.isNotEmpty()
        adapter.submitList(summary.duplicateGroups)
        binding.emptyContainer.isVisible = summary.duplicateGroups.isEmpty()
        binding.tvEmptyTitle.setText(R.string.contacts_empty_group_title)
        binding.tvEmptyBody.setText(R.string.contacts_empty_group_body)
    }

    private fun confirmDeleteEmptyContacts() {
        val summary = viewModel.uiState.value.summary ?: return
        if (summary.emptyContacts.isEmpty()) return

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.contacts_delete_empty_confirm_title)
            .setMessage(
                getString(
                    R.string.contacts_delete_empty_confirm_message_format,
                    summary.emptyContacts.size,
                ),
            )
            .setNegativeButton(R.string.delete_dialog_cancel, null)
            .setPositiveButton(R.string.delete_dialog_confirm) { _, _ ->
                val ids = summary.emptyContacts.map { it.contactId }
                viewModel.deleteEmptyContacts(ids) { success ->
                    if (success) {
                        showToast(getString(R.string.contacts_cleanup_complete_message_format, ids.size))
                    } else {
                        showToast(getString(R.string.contacts_action_failed))
                    }
                }
            }
            .show()
    }

    private fun mergeGroup(group: DuplicateContactGroup) {
        viewModel.mergeGroup(group) { success ->
            if (success) {
                showToast(getString(R.string.contacts_merge_complete_message))
            } else {
                showToast(getString(R.string.contacts_action_failed))
            }
        }
    }

    private fun openSystemContact(group: DuplicateContactGroup) {
        val entry = group.entries.firstOrNull() ?: return
        val uri = ContactsContract.Contacts.getLookupUri(entry.contactId, entry.lookupKey)
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
