package com.aetherquorion.cleansuperai.ui.common

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.aetherquorion.cleansuperai.R

object StoragePermissionDialog {
    fun show(context: Context, onContinue: () -> Unit): AlertDialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_storage_permission, null)
        val dialog = AlertDialog.Builder(context).setView(view).create()
        view.findViewById<View>(R.id.closeButton).setOnClickListener { dialog.dismiss() }
        view.findViewById<View>(R.id.continueButton).setOnClickListener {
            dialog.dismiss()
            onContinue()
        }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
}
