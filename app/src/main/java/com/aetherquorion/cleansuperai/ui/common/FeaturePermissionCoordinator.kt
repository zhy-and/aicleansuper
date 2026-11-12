package com.aetherquorion.cleansuperai.ui.common

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings

object FeaturePermissionCoordinator {
    fun hasAccess(context: Context, access: FeatureAccess): Boolean = when (access) {
        FeatureAccess.NONE -> true
        FeatureAccess.ALL_FILES -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            MediaPermissionHelper.hasPermissions(context)
        } else Environment.isExternalStorageManager()
        FeatureAccess.USAGE -> {
            val manager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            manager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        }
    }

    fun settingsIntent(context: Context, access: FeatureAccess): Intent = when (access) {
        FeatureAccess.ALL_FILES -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}"))
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"))
        }
        FeatureAccess.USAGE -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        FeatureAccess.NONE -> error("No settings screen is needed")
    }
}
