package com.aetherquorion.cleansuperai.notification

import android.app.NotificationManager
import com.tencent.mmkv.MMKV

/** A distinct system ID for each logical local push, including across process restarts. */
internal object LocalPushNotificationIds {
    const val FIRST_ID = 100_000
    private const val KEY_LAST_ID = "local_push_last_notification_id"

    @Synchronized
    fun allocate(manager: NotificationManager, reserved: Collection<Int>): Int {
        val store = MMKV.defaultMMKV()
        val occupied = manager.activeNotifications.mapTo(reserved.toMutableSet()) { it.id }
        val id = next(store.getInt(KEY_LAST_ID, 0), occupied)
        store.putInt(KEY_LAST_ID, id)
        return id
    }

    fun next(lastId: Int, occupied: Set<Int>): Int {
        var id = lastId.takeIf { it in FIRST_ID until Int.MAX_VALUE } ?: FIRST_ID - 1
        do {
            id = if (id == Int.MAX_VALUE - 1) FIRST_ID else id + 1
        } while (id in occupied)
        return id
    }
}
