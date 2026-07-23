package com.example.cleansuperai.data.local

import android.content.Context

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isProEnabled(): Boolean = prefs.getBoolean(KEY_PRO_ENABLED, false)

    fun setProEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRO_ENABLED, enabled).apply()
    }

    fun hasPrivacyPin(): Boolean = prefs.contains(KEY_PRIVACY_PIN_HASH)

    fun savePrivacyPin(pin: String) {
        prefs.edit().putInt(KEY_PRIVACY_PIN_HASH, pin.hashCode()).apply()
    }

    fun clearPrivacyPin() {
        prefs.edit().remove(KEY_PRIVACY_PIN_HASH).apply()
    }

    companion object {
        private const val PREFS_NAME = "clean_super_prefs"
        private const val KEY_PRO_ENABLED = "key_pro_enabled"
        private const val KEY_PRIVACY_PIN_HASH = "key_privacy_pin_hash"
    }
}
