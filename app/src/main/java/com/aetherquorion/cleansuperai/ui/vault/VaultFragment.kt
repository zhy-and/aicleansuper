package com.aetherquorion.cleansuperai.ui.vault

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import android.widget.MediaController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.ui.clean.detail.DisplayMediaItem
import com.aetherquorion.cleansuperai.ui.clean.detail.MediaCleanupAdapter
import com.aetherquorion.cleansuperai.ui.common.ParityUi
import com.aetherquorion.cleansuperai.ui.common.TopFilterUi
import com.aetherquorion.cleansuperai.vault.VaultAuth
import com.aetherquorion.cleansuperai.vault.VaultFileRepository
import com.aetherquorion.cleansuperai.vault.VaultMediaItem
import com.aetherquorion.cleansuperai.vault.VaultSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VaultFragment : Fragment() {
    private var items = emptyList<VaultMediaItem>()
    private val selected = mutableSetOf<String>()
    private lateinit var status: TextView
    private lateinit var selectAll: TextView
    private lateinit var list: RecyclerView
    private lateinit var emptyView: TextView
    private val mediaAdapter = MediaCleanupAdapter(::previewMedia, ::selectMedia)
    private lateinit var importButton: Button
    private lateinit var restoreButton: Button
    private lateinit var deleteButton: Button
    private lateinit var galleryRoot: View
    private lateinit var pinScreen: VaultPinScreen
    private var firstPin: String? = null
    private var settingPin = false
    private val picker = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(100)) { picked ->
        val uris = picked.distinct()
        if (uris.isEmpty()) return@registerForActivityResult
        viewLifecycleOwner.lifecycleScope.launch {
            status.text = "Importing…"
            val results = withContext(Dispatchers.IO) { uris.map { VaultFileRepository.importUri(requireContext(), it) } }
            val imported = results.count { it.imported }
            val kept = results.count { it.imported && !it.originalRemoved }
            render()
            status.text = "Imported $imported files" + if (kept > 0) "; $kept originals remain on device" else ""
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(16))
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.bg_primary))
        }
        root.addView(TopFilterUi.toolbar(ctx, "Private vault") { VaultSession.lock(); parentFragmentManager.popBackStack() })
        val summary = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(10) }
        }
        status = ParityUi.status(ctx).apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        summary.addView(status)
        selectAll = TopFilterUi.label(ctx, "Select all").apply {
            setPadding(dp(12), 0, dp(12), 0)
            background = TopFilterUi.surface(ctx)
            visibility = View.GONE
            setOnClickListener {
                if (selected.size == items.size) selected.clear() else selected.addAll(items.map { it.id })
                render()
            }
        }
        summary.addView(selectAll, LinearLayout.LayoutParams(-2, dp(36)))
        root.addView(summary)
        importButton = ParityUi.button(ctx, ParityUi.ButtonKind.PRIMARY).apply { text = "Import photos or videos"; setOnClickListener { openPicker() } }
        list = RecyclerView(ctx).apply {
            layoutManager = GridLayoutManager(ctx, if (resources.configuration.screenWidthDp >= 430) 4 else 3)
            adapter = mediaAdapter
            clipToPadding = false
            setPadding(0, dp(10), 0, dp(10))
        }
        emptyView = TextView(ctx).apply {
            setText(R.string.vault_empty_gallery)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
            textSize = 14f
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        root.addView(FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            addView(list, FrameLayout.LayoutParams(-1, -1))
            addView(emptyView, FrameLayout.LayoutParams(-1, -1))
        })
        restoreButton = ParityUi.button(ctx).apply { text = "Restore"; setOnClickListener { restore() } }
        deleteButton = ParityUi.button(ctx, ParityUi.ButtonKind.DANGER).apply { text = "Delete"; setOnClickListener { delete() } }
        val actions = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(-1, dp(64)).apply { bottomMargin = dp(12) }
        }
        actions.addView(importButton, LinearLayout.LayoutParams(-1, dp(52)))
        actions.addView(restoreButton, LinearLayout.LayoutParams(0, dp(52), 1f).apply { rightMargin = dp(8) })
        actions.addView(deleteButton, LinearLayout.LayoutParams(0, dp(52), 1f))
        root.addView(actions)
        importButton.isEnabled = false
        restoreButton.isEnabled = false
        deleteButton.isEnabled = false
        galleryRoot = root
        pinScreen = VaultPinScreen(ctx) { pin -> onPinEntered(pin) }
        return FrameLayout(ctx).apply {
            addView(root, FrameLayout.LayoutParams(-1, -1))
            addView(pinScreen, FrameLayout.LayoutParams(-1, -1))
        }
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        if (VaultSession.isUnlocked()) {
            pinScreen.visibility = View.GONE
            render()
        }
        else if (VaultAuth.hasPin()) askPin(false)
        else askPin(true)
    }

    override fun onDestroyView() {
        list.adapter = null
        VaultSession.lock()
        super.onDestroyView()
    }

    private fun askPin(setup: Boolean) {
        settingPin = setup
        firstPin = null
        galleryRoot.visibility = View.GONE
        pinScreen.visibility = View.VISIBLE
        pinScreen.showStep(if (setup) "Set vault PIN" else "Enter PIN",
            "Uninstalling the app will delete vault files.")
        pinScreen.setBackClick { parentFragmentManager.popBackStack() }
        pinScreen.setBiometricClick(if (!setup && canUseBiometrics()) ({ offerBiometric() }) else null)
    }

    private fun canUseBiometrics() = BiometricManager.from(requireContext()).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    private fun onPinEntered(pin: String) {
        if (settingPin) {
            val first = firstPin
            if (first == null) {
                firstPin = pin
                pinScreen.showStep("Confirm PIN", "Uninstalling the app will delete vault files.")
            } else if (first == pin && VaultAuth.setPin(pin)) {
                unlockGallery()
            } else {
                firstPin = null
                pinScreen.showStep("Set vault PIN", "Uninstalling the app will delete vault files.", "PINs did not match")
            }
        } else if (VaultAuth.verifyPin(pin)) {
            unlockGallery()
        } else {
            pinScreen.showStep("Enter PIN", "Uninstalling the app will delete vault files.", "Incorrect PIN")
        }
    }

    private fun unlockGallery() {
        VaultSession.unlock()
        pinScreen.visibility = View.GONE
        galleryRoot.visibility = View.VISIBLE
        render()
    }

    private fun offerBiometric() {
        if (BiometricManager.from(requireContext()).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            != BiometricManager.BIOMETRIC_SUCCESS) return
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(requireContext()),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlockGallery()
                }
            })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock private vault")
            .setNegativeButtonText("Use PIN")
            .build())
    }

    private fun render() {
        if (!VaultSession.isUnlocked()) return
        importButton.isEnabled = true
        items = VaultFileRepository.listExisting(requireContext())
        status.text = "${items.size} items · ${Formatter.formatShortFileSize(requireContext(), items.sumOf { it.sizeBytes })}"
        emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        list.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        selected.retainAll(items.map { it.id }.toSet())
        selectAll.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        selectAll.text = if (selected.size == items.size) "Deselect all" else "Select all"
        mediaAdapter.submitList(items.map { item ->
            DisplayMediaItem(
                stableId = stableIdFor(item),
                thumbnailUri = Uri.fromFile(item.file(requireContext().filesDir)),
                title = item.displayName,
                metaPrimary = "",
                metaSecondary = "",
                sizeText = Formatter.formatShortFileSize(requireContext(), item.sizeBytes),
                isVideo = item.isVideo,
                isPrivate = true,
                isSelectable = true,
                isSelected = item.id in selected,
            )
        })
        restoreButton.isEnabled = selected.isNotEmpty()
        deleteButton.isEnabled = selected.isNotEmpty()
        importButton.visibility = if (selected.isEmpty()) View.VISIBLE else View.GONE
        restoreButton.visibility = if (selected.isEmpty()) View.GONE else View.VISIBLE
        deleteButton.visibility = if (selected.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun previewMedia(media: DisplayMediaItem) {
        items.firstOrNull { stableIdFor(it) == media.stableId }?.let(::preview)
    }

    private fun selectMedia(media: DisplayMediaItem) {
        val item = items.firstOrNull { stableIdFor(it) == media.stableId } ?: return
        if (!selected.add(item.id)) selected.remove(item.id)
        render()
    }

    private fun stableIdFor(item: VaultMediaItem): Long =
        item.id.take(15).toLongOrNull(16) ?: item.id.hashCode().toLong()

    private fun openPicker() {
        if (!VaultSession.isUnlocked()) return
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }

    private fun selectedItems() = items.filter { it.id in selected }

    private fun restore() {
        if (!VaultSession.isUnlocked()) return
        val targets = selectedItems()
        AlertDialog.Builder(requireContext()).setMessage("Restore ${targets.size} files to Pictures/PurGuClean?")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Restore") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val count = withContext(Dispatchers.IO) { VaultFileRepository.restoreItems(requireContext(), targets) }
                    selected.clear(); render(); status.text = "Restored $count of ${targets.size} files"
                }
            }.show()
    }

    private fun delete() {
        if (!VaultSession.isUnlocked()) return
        val targets = selectedItems()
        AlertDialog.Builder(requireContext()).setMessage("Permanently delete ${targets.size} private files?")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) { VaultFileRepository.deleteItems(requireContext(), targets) }
                    selected.clear(); render()
                }
            }.show()
    }

    private fun preview(item: VaultMediaItem) {
        if (!VaultSession.isUnlocked()) return
        val file = item.file(requireContext().filesDir)
        if (item.isVideo) {
            val video = VideoView(requireContext()).apply {
                setVideoURI(android.net.Uri.fromFile(file))
                setMediaController(MediaController(requireContext()))
                layoutParams = LinearLayout.LayoutParams(-1, dp(320))
            }
            AlertDialog.Builder(requireContext()).setTitle(item.displayName)
                .setView(video).setPositiveButton(android.R.string.ok, null).show()
            video.start()
        } else {
            AlertDialog.Builder(requireContext()).setTitle(item.displayName)
                .setView(ImageView(requireContext()).apply {
                    setImageURI(android.net.Uri.fromFile(file))
                    adjustViewBounds = true
                }).setPositiveButton(android.R.string.ok, null).show()
        }
    }

    private fun message(text: String) = Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
