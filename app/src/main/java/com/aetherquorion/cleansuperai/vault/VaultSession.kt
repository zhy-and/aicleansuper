package com.aetherquorion.cleansuperai.vault

object VaultSession {
    @Volatile
    private var unlocked: Boolean = false

    fun isUnlocked(): Boolean = unlocked

    fun unlock() {
        unlocked = true
    }

    fun lock() {
        unlocked = false
    }
}
