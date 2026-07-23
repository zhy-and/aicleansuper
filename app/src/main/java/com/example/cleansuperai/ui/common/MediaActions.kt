package com.example.cleansuperai.ui.common

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cleansuperai.R

object MediaActions {
    fun openImagePreview(fragment: Fragment, uri: Uri, title: String) {
        fragment.parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                MediaPreviewFragment.newInstance(uri, title),
            )
            .addToBackStack(null)
            .commit()
    }

    fun openVideoExternally(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, null))
        }.onFailure {
            Toast.makeText(context, R.string.video_player_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    fun createDeleteIntentSender(context: Context, uris: List<Uri>): IntentSender? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
        } else {
            null
        }
    }

    fun deleteLegacy(context: Context, uris: List<Uri>): Int {
        var deleted = 0
        uris.forEach { uri ->
            deleted += context.contentResolver.delete(uri, null, null)
        }
        return deleted
    }
}
