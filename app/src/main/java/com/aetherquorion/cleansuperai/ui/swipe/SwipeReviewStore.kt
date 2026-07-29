package com.aetherquorion.cleansuperai.ui.swipe

import android.content.Context
import android.net.Uri
import androidx.core.content.edit

internal class SwipeReviewStore(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun filterReviewable(uris: List<Uri>): List<Uri> {
        val hidden = hiddenUriStrings()
        return uris.filterNot { it.toString() in hidden }
    }

    fun markKept(uri: Uri) {
        val uriString = uri.toString()
        val kept = keptUriStrings().toMutableSet()
        val pending = pendingUriStrings().toMutableSet()
        kept += uriString
        pending -= uriString
        persist(KEY_KEPT, kept)
        persist(KEY_PENDING, pending)
    }

    fun moveToTrash(uri: Uri) {
        val uriString = uri.toString()
        val kept = keptUriStrings().toMutableSet()
        val pending = pendingUriStrings().toMutableSet()
        pending += uriString
        kept -= uriString
        persist(KEY_PENDING, pending)
        persist(KEY_KEPT, kept)
    }

    fun restoreFromTrash(uri: Uri) {
        val pending = pendingUriStrings().toMutableSet()
        pending -= uri.toString()
        persist(KEY_PENDING, pending)
    }

    fun loadTrashUris(): List<Uri> = pendingUriStrings().map(Uri::parse)

    fun clearDeleted(uris: Collection<Uri>) {
        if (uris.isEmpty()) return
        val deleted = uris.mapTo(mutableSetOf(), Uri::toString)
        val pending = pendingUriStrings().filterNot { it in deleted }.toSet()
        val kept = keptUriStrings().filterNot { it in deleted }.toSet()
        persist(KEY_PENDING, pending)
        persist(KEY_KEPT, kept)
    }

    fun pruneToExisting(existingUris: Collection<Uri>) {
        val existing = existingUris.mapTo(mutableSetOf(), Uri::toString)
        val pending = pendingUriStrings().filter { it in existing }.toSet()
        val kept = keptUriStrings().filter { it in existing }.toSet()
        persist(KEY_PENDING, pending)
        persist(KEY_KEPT, kept)
    }

    private fun hiddenUriStrings(): Set<String> = keptUriStrings() + pendingUriStrings()

    private fun keptUriStrings(): Set<String> =
        preferences.getStringSet(KEY_KEPT, emptySet()).orEmpty()

    private fun pendingUriStrings(): Set<String> =
        preferences.getStringSet(KEY_PENDING, emptySet()).orEmpty()

    private fun persist(key: String, values: Set<String>) {
        preferences.edit {
            if (values.isEmpty()) {
                remove(key)
            } else {
                putStringSet(key, values)
            }
        }
    }

    private companion object {
        const val PREFS_NAME = "photo_swipe_review"
        const val KEY_KEPT = "kept_uris"
        const val KEY_PENDING = "pending_uris"
    }
}
