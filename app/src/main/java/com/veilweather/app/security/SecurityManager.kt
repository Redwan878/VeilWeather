package com.veilweather.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

enum class UnlockState {
    LOCKED,
    UNLOCKED_REAL,
    UNLOCKED_DURESS
}

enum class CodeResult {
    VALID,
    DURESS,
    PANIC,
    INVALID
}

@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _unlockStateFlow = MutableStateFlow(UnlockState.LOCKED)
    val unlockStateFlow: StateFlow<UnlockState> = _unlockStateFlow

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "veil_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val PREF_SECRET_CODE_HASH = "secret_code_hash"
        private const val PREF_SECRET_CODE_SALT = "secret_code_salt"
        private const val PREF_DURESS_CODE_HASH = "duress_code_hash"
        private const val PREF_DURESS_CODE_SALT = "duress_code_salt"
        private const val PREF_PANIC_CODE_HASH = "panic_code_hash"
        private const val PREF_PANIC_CODE_SALT = "panic_code_salt"
        
        private const val DEFAULT_SECRET_CODE = "8546"
        private const val DEFAULT_DURESS_CODE = "1111"
        private const val DEFAULT_PANIC_CODE = "0000"
        
        private const val ITERATION_COUNT = 120000
        private const val KEY_LENGTH = 256

        @Volatile
        private var instance: SecurityManager? = null

        fun getInstance(): SecurityManager {
            return instance ?: throw IllegalStateException("SecurityManager not initialized")
        }

        internal fun setInstance(manager: SecurityManager) {
            instance = manager
        }
    }

    init {
        setInstance(this)
        initializeDefaultCodes()
    }

    private fun initializeDefaultCodes() {
        if (!encryptedPrefs.contains(PREF_SECRET_CODE_HASH)) {
            setSecretCode(DEFAULT_SECRET_CODE)
        }
        if (!encryptedPrefs.contains(PREF_DURESS_CODE_HASH)) {
            setDuressCode(DEFAULT_DURESS_CODE)
        }
        if (!encryptedPrefs.contains(PREF_PANIC_CODE_HASH)) {
            setPanicCode(DEFAULT_PANIC_CODE)
        }
    }

    /**
     * Hash a code using PBKDF2 with salt
     */
    private fun hashCode(code: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(code.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    /**
     * Generate random salt
     */
    private fun generateSalt(): ByteArray {
        val salt = ByteArray(32)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Verify code against stored hash
     */
    private fun verifyCode(code: String, hashKey: String, saltKey: String): Boolean {
        val storedHash = encryptedPrefs.getString(hashKey, null) ?: return false
        val storedSalt = encryptedPrefs.getString(saltKey, null) ?: return false
        
        val salt = android.util.Base64.decode(storedSalt, android.util.Base64.DEFAULT)
        val computedHash = hashCode(code, salt)
        val computedHashString = android.util.Base64.encodeToString(computedHash, android.util.Base64.DEFAULT)
        
        return MessageDigest.isEqual(storedHash.toByteArray(), computedHashString.toByteArray())
    }

    /**
     * Set secret code (for real messenger unlock)
     */
    fun setSecretCode(code: String) {
        val salt = generateSalt()
        val hash = hashCode(code, salt)
        encryptedPrefs.edit()
            .putString(PREF_SECRET_CODE_HASH, android.util.Base64.encodeToString(hash, android.util.Base64.DEFAULT))
            .putString(PREF_SECRET_CODE_SALT, android.util.Base64.encodeToString(salt, android.util.Base64.DEFAULT))
            .apply()
    }

    /**
     * Set duress code (for fake messenger)
     */
    fun setDuressCode(code: String) {
        val salt = generateSalt()
        val hash = hashCode(code, salt)
        encryptedPrefs.edit()
            .putString(PREF_DURESS_CODE_HASH, android.util.Base64.encodeToString(hash, android.util.Base64.DEFAULT))
            .putString(PREF_DURESS_CODE_SALT, android.util.Base64.encodeToString(salt, android.util.Base64.DEFAULT))
            .apply()
    }

    /**
     * Set panic code (for wipe)
     */
    fun setPanicCode(code: String) {
        val salt = generateSalt()
        val hash = hashCode(code, salt)
        encryptedPrefs.edit()
            .putString(PREF_PANIC_CODE_HASH, android.util.Base64.encodeToString(hash, android.util.Base64.DEFAULT))
            .putString(PREF_PANIC_CODE_SALT, android.util.Base64.encodeToString(salt, android.util.Base64.DEFAULT))
            .apply()
    }

    /**
     * Verify secret code
     */
    fun verifySecretCode(code: String): Boolean {
        return verifyCode(code, PREF_SECRET_CODE_HASH, PREF_SECRET_CODE_SALT)
    }

    /**
     * Verify duress code
     */
    fun verifyDuressCode(code: String): Boolean {
        return verifyCode(code, PREF_DURESS_CODE_HASH, PREF_DURESS_CODE_SALT)
    }

    /**
     * Verify panic code
     */
    fun verifyPanicCode(code: String): Boolean {
        return verifyCode(code, PREF_PANIC_CODE_HASH, PREF_PANIC_CODE_SALT)
    }

    /**
     * Unlock with real messenger access
     */
    fun unlock() {
        _unlockStateFlow.value = UnlockState.UNLOCKED_REAL
    }

    /**
     * Unlock with duress (fake messenger)
     */
    fun unlockWithDuress() {
        _unlockStateFlow.value = UnlockState.UNLOCKED_DURESS
    }

    /**
     * Lock messenger
     */
    fun lock() {
        _unlockStateFlow.value = UnlockState.LOCKED
    }

    /**
     * Check if unlocked
     */
    fun isUnlocked(): Boolean {
        return _unlockStateFlow.value != UnlockState.LOCKED
    }

    /**
     * Get current unlock state
     */
    fun getUnlockState(): UnlockState {
        return _unlockStateFlow.value
    }

    /**
     * Panic wipe - erase all keys, chats, media
     */
    fun panicWipe() {
        // Clear all encrypted preferences
        encryptedPrefs.edit().clear().apply()
        
        // Reinitialize default codes
        initializeDefaultCodes()
        
        // Lock
        lock()
        
        // TODO: Wipe all chat data, keys, media from database
    }

    /**
     * Remote wipe (triggered from messenger search bar)
     */
    fun remoteWipe() {
        panicWipe()
        // TODO: Send wipe command to paired device via WebSocket
    }
}