package com.timeguard.tv.security

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinManager(context: Context) {
    private val prefs = context.getSharedPreferences("timeguard_security", Context.MODE_PRIVATE)

    init {
        if (!prefs.contains(KEY_PIN_HASH)) setPin(DEFAULT_PIN)
        if (!prefs.contains(KEY_RECOVERY_HASH)) regenerateRecoveryCode()
    }

    fun verifyPin(pin: String): Boolean = verify(pin, KEY_PIN_SALT, KEY_PIN_HASH)

    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin) || !isValidPin(newPin)) return false
        setPin(newPin)
        return true
    }

    fun resetPinWithRecovery(recoveryCode: String, newPin: String): Boolean {
        if (!verify(normalizeRecovery(recoveryCode), KEY_RECOVERY_SALT, KEY_RECOVERY_HASH) || !isValidPin(newPin)) return false
        setPin(newPin)
        regenerateRecoveryCode()
        return true
    }

    fun getRecoveryCode(): String = prefs.getString(KEY_RECOVERY_DISPLAY, "") ?: ""

    fun regenerateRecoveryCode(): String {
        val number = SecureRandom().nextInt(100_000_000)
        val raw = "%08d".format(number)
        val formatted = raw.chunked(4).joinToString("-")
        storeSecret(raw, KEY_RECOVERY_SALT, KEY_RECOVERY_HASH)
        prefs.edit().putString(KEY_RECOVERY_DISPLAY, formatted).apply()
        return formatted
    }

    fun isDefaultPin(): Boolean = verifyPin(DEFAULT_PIN)

    private fun setPin(pin: String) = storeSecret(pin, KEY_PIN_SALT, KEY_PIN_HASH)

    private fun storeSecret(secret: String, saltKey: String, hashKey: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(secret, salt)
        prefs.edit()
            .putString(saltKey, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(hashKey, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    private fun verify(secret: String, saltKey: String, hashKey: String): Boolean {
        val saltText = prefs.getString(saltKey, null) ?: return false
        val expectedText = prefs.getString(hashKey, null) ?: return false
        return try {
            val actual = hash(secret, Base64.decode(saltText, Base64.NO_WRAP))
            val expected = Base64.decode(expectedText, Base64.NO_WRAP)
            java.security.MessageDigest.isEqual(actual, expected)
        } catch (_: Exception) { false }
    }

    private fun hash(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, 60_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun normalizeRecovery(value: String) = value.replace("-", "").replace(" ", "")

    companion object {
        const val DEFAULT_PIN = "1234"
        fun isValidPin(pin: String) = pin.length in 4..8 && pin.all(Char::isDigit)
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_RECOVERY_SALT = "recovery_salt"
        private const val KEY_RECOVERY_HASH = "recovery_hash"
        private const val KEY_RECOVERY_DISPLAY = "recovery_display"
    }
}
