package com.example.cleansuperai.ui.privacy

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.cleansuperai.R
import com.example.cleansuperai.data.local.AppPreferences
import com.example.cleansuperai.databinding.FragmentPrivacyBinding

class PrivacyFragment : Fragment() {
    private var _binding: FragmentPrivacyBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPrivacyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = AppPreferences(requireContext())
        binding.btnSetPin.setOnClickListener { showSetPinDialog() }
        binding.btnResetPin.setOnClickListener { confirmResetPin() }
        renderState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderState() {
        val hasPin = preferences.hasPrivacyPin()
        binding.tvPinStatus.setText(
            if (hasPin) R.string.privacy_status_enabled else R.string.privacy_status_disabled,
        )
        binding.tvPinBody.setText(
            if (hasPin) R.string.privacy_status_body_enabled else R.string.privacy_status_body_disabled,
        )
        binding.btnResetPin.isEnabled = hasPin
    }

    private fun showSetPinDialog() {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(InputFilter.LengthFilter(4))
            hint = getString(R.string.privacy_pin_dialog_hint)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.privacy_pin_dialog_title)
            .setView(input)
            .setNegativeButton(R.string.delete_dialog_cancel, null)
            .setPositiveButton(R.string.delete_dialog_confirm) { _, _ ->
                val pin = input.text?.toString().orEmpty()
                if (pin.length == 4 && pin.all(Char::isDigit)) {
                    preferences.savePrivacyPin(pin)
                    renderState()
                    showToast(getString(R.string.privacy_pin_saved))
                } else {
                    showToast(getString(R.string.privacy_pin_invalid))
                }
            }
            .show()
    }

    private fun confirmResetPin() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.privacy_reset_pin_action)
            .setMessage(R.string.privacy_pin_reset_confirm)
            .setNegativeButton(R.string.delete_dialog_cancel, null)
            .setPositiveButton(R.string.delete_dialog_confirm) { _, _ ->
                preferences.clearPrivacyPin()
                renderState()
                showToast(getString(R.string.privacy_pin_reset_done))
            }
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
