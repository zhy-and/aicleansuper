package com.aetherquorion.cleansuperai.ui.swipe

import android.content.Context
import android.net.Uri
import androidx.core.content.edit

internal class SwipeReviewStore(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var cachedKept: MutableSet<String>? = null
    private var cachedPending: MutableSet<String>? = null

    fun filterReviewable(uris: List<Uri>): List<Uri> {
        val hidden = hiddenUriStrings()
        return uris.filterNot { it.toString() in hidden }
    }

    fun markKept(uri: Uri) {
        val uriString = uri.toString()
        val kept = keptUriStrings()
        val pending = pendingUriStrings()
        if (kept.add(uriString)) {
            persist(KEY_KEPT, kept)
        }
        if (pending.remove(uriString)) {
            persist(KEY_PENDING, pending)
        }
    }

    fun moveToTrash(uri: Uri) {
        val uriString = uri.toString()
        val kept = keptUriStrings()
        val pending = pendingUriStrings()
        if (pending.add(uriString)) {
            persist(KEY_PENDING, pending)
        }
        if (kept.remove(uriString)) {
            persist(KEY_KEPT, kept)
        }
    }

    fun restoreFromTrash(uri: Uri) {
        val pending = pendingUriStrings()
        if (pending.remove(uri.toString())) {
            persist(KEY_PENDING, pending)
        }
    }

    fun loadTrashUris(): List<Uri> = pendingUriStrings().map(Uri::parse)

    fun clearDeleted(uris: Collection<Uri>) {
        if (uris.isEmpty()) return
        val deleted = uris.mapTo(mutableSetOf(), Uri::toString)
        val pending = pendingUriStrings()
        val kept = keptUriStrings()
        if (pending.removeAll(deleted)) {
            persist(KEY_PENDING, pending)
        }
        if (kept.removeAll(deleted)) {
            persist(KEY_KEPT, kept)
        }
    }

    fun pruneToExisting(existingUris: Collection<Uri>) {
        val existing = existingUris.mapTo(mutableSetOf(), Uri::toString)
        val pending = pendingUriStrings()
        val kept = keptUriStrings()
        if (pending.retainAll(existing)) {
            persist(KEY_PENDING, pending)
        }
        if (kept.retainAll(existing)) {
            persist(KEY_KEPT, kept)
        }
    }

    private fun hiddenUriStrings(): Set<String> = keptUriStrings() + pendingUriStrings()

    private fun keptUriStrings(): MutableSet<String> =
        cachedKept ?: preferences.getStringSet(KEY_KEPT, emptySet())
            .orEmpty()
            .toMutableSet()
            .also { cachedKept = it }

    private fun pendingUriStrings(): MutableSet<String> =
        cachedPending ?: preferences.getStringSet(KEY_PENDING, emptySet())
            .orEmpty()
            .toMutableSet()
            .also { cachedPending = it }

    private fun persist(key: String, values: Set<String>) {
        val snapshot = values.toSet()
        preferences.edit {
            if (snapshot.isEmpty()) {
                remove(key)
            } else {
                putStringSet(key, snapshot)
            }
        }
    }

    private companion object {
        const val PREFS_NAME = "photo_swipe_review"
        const val KEY_KEPT = "kept_uris"
        const val KEY_PENDING = "pending_uris"
    }
}
