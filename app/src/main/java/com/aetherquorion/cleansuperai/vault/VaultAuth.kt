package com.aetherquorion.cleansuperai.vault

import android.util.Base64
import com.tencent.mmkv.MMKV
import java.security.MessageDigest
import java.security.SecureRandom

object VaultAuth {
    private const val MMKV_ID = "private_vault_auth"
    private const val KEY_SALT = "pin_salt"
    private const val KEY_HASH = "pin_hash"
    private const val PIN_LENGTH = 6

    private fun kv(): MMKV = MMKV.mmkvWithID(MMKV_ID)

    fun hasPin(): Boolean {
        return try {
            val hash = kv().decodeString(KEY_HASH, null)
            val salt = kv().decodeString(KEY_SALT, null)
            !hash.isNullOrBlank() && !salt.isNullOrBlank()
        } catch (_: Throwable) {
            false
        }
    }

    fun isValidPinFormat(pin: String): Boolean {
        return pin.length == PIN_LENGTH && pin.all { it.isDigit() }
    }

    fun setPin(pin: String): Boolean {
        if (!isValidPinFormat(pin)) return false
        return try {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val hash = hashPin(pin, salt)
            kv().encode(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            kv().encode(KEY_HASH, hash)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun verifyPin(pin: String): Boolean {
        if (!isValidPinFormat(pin)) return false
        return try {
            val saltB64 = kv().decodeString(KEY_SALT, null) ?: return false
            val expected = kv().decodeString(KEY_HASH, null) ?: return false
            val salt = Base64.decode(saltB64, Base64.NO_WRAP)
            hashPin(pin, salt) == expected
        } catch (_: Throwable) {
            false
        }
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        var result = digest.digest()
        repeat(999) {
            digest.reset()
            result = digest.digest(result)
        }
        return Base64.encodeToString(result, Base64.NO_WRAP)
    }
}
