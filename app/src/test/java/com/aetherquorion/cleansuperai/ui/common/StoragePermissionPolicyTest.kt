package com.aetherquorion.cleansuperai.ui.common

import android.Manifest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class StoragePermissionPolicyTest {
    @Test fun modernAndroidUsesAllFilesSettingsInsteadOfMediaRuntimePermissions() {
        assertArrayEquals(emptyArray<String>(), MediaPermissionHelper.requiredPermissions(30))
        assertArrayEquals(emptyArray<String>(), MediaPermissionHelper.requiredPermissions(36))
    }

    @Test fun olderAndroidKeepsLegacyStoragePermission() {
        assertArrayEquals(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), MediaPermissionHelper.requiredPermissions(29))
        assertArrayEquals(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), MediaPermissionHelper.requiredPermissions(28))
    }
}
