package com.veilweather.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SignalProtocolManager(private val context: Context) {
    
    companion object {
        private const val KEY_ALIAS = "veil_weather_signal"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    }
    
    private val sessionStore = SignalSessionStore(context)
    private var identityKeyPair: KeyPair? = null
    private val secureRandom = SecureRandom()
    
    init {
        loadOrGenerateIdentityKeyPair()
    }
    
    private fun loadOrGenerateIdentityKeyPair() {
        thread {
            try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                
                val prefs = EncryptedSharedPreferences.create(
                    context, "signal_prefs", masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                
                val serializedKey = prefs.getString("identity_key", null)
                if (serializedKey != null) {
                    identityKeyPair = deserializeKeyPair(serializedKey)
                } else {
                    identityKeyPair = generateIdentityKeyPair()
                    prefs.edit().putString("identity_key", serializeKeyPair(identityKeyPair!!)).apply()
                }
            } catch (e: Exception) {
                identityKeyPair = generateIdentityKeyPair()
            }
        }
    }
    
    private fun generateIdentityKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(256)
        return kpg.generateKeyPair()
    }
    
    private fun serializeKeyPair(kp: KeyPair): String {
        return Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP) + "|" +
               Base64.encodeToString(kp.private.encoded, Base64.NO_WRAP)
    }
    
    private fun deserializeKeyPair(serialized: String): KeyPair {
        val parts = serialized.split("|")
        val publicSpec = java.security.spec.X509EncodedKeySpec(Base64.decode(parts[0], Base64.NO_WRAP))
        val privateSpec = java.security.spec.PKCS8EncodedKeySpec(Base64.decode(parts[1], Base64.NO_WRAP))
        val kf = java.security.KeyFactory.getInstance("EC")
        return KeyPair(kf.generatePublic(publicSpec), kf.generatePrivate(privateSpec))
    }
    
    suspend fun encryptMessage(recipientKey: ByteArray, message: String): ByteArray = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val keySpec = SecretKeySpec(recipientKey.copyOf(32), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        cipher.doFinal(message.toByteArray(Charsets.UTF_8))
    }
    
    suspend fun decryptMessage(senderKey: ByteArray, encryptedData: ByteArray): String = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val keySpec = SecretKeySpec(senderKey.copyOf(32), "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        String(cipher.doFinal(encryptedData), Charsets.UTF_8)
    }
    
    suspend fun createEncryptedMessage(recipientUserId: String, content: String): EncryptedMessage = withContext(Dispatchers.IO) {
        val ephemeralKey = ByteArray(32)
        secureRandom.nextBytes(ephemeralKey)
        val encrypted = encryptMessage(ephemeralKey, content)
        EncryptedMessage(
            senderId = getUserId(),
            recipientId = recipientUserId,
            ephemeralPublicKey = ephemeralKey,
            encryptedContent = encrypted,
            timestamp = System.currentTimeMillis(),
            messageId = generateMessageId()
        )
    }
    
    private fun generateMessageId(): String {
        val bytes = ByteArray(8)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    
    fun getUserId(): String {
        return identityKeyPair?.let {
            Base64.encodeToString(it.public.encoded.copyOf(16), Base64.NO_WRAP)
        } ?: "unknown"
    }
    
    data class EncryptedMessage(
        val senderId: String,
        val recipientId: String,
        val ephemeralPublicKey: ByteArray,
        val encryptedContent: ByteArray,
        val timestamp: Long,
        val messageId: String
    )
    
    data class SenderKey(val keyId: Int, val secretKey: ByteArray)
    
    data class SenderKeyMessage(
        val chatId: String,
        val senderKeyId: Int,
        val ciphertext: ByteArray,
        val iv: ByteArray,
        val timestamp: Long
    )
}

class SignalSessionStore(private val context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context, "signal_sessions", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun storePublicKey(userId: String, publicKey: ByteArray) {
        prefs.edit().putString("pub_$userId", Base64.encodeToString(publicKey, Base64.NO_WRAP)).apply()
    }
    
    fun getPublicKey(userId: String): ByteArray? {
        return prefs.getString("pub_$userId", null)?.let { Base64.decode(it, Base64.NO_WRAP) }
    }
    
    fun markPaired(userId: String) {
        prefs.edit().putBoolean("paired_$userId", true).apply()
    }
    
    fun storeTempKeyPair(userId: String, key: ByteArray) {
        prefs.edit().putString("temp_$userId", Base64.encodeToString(key, Base64.NO_WRAP)).apply()
    }
    
    fun storeSenderKey(chatId: String, senderKey: SignalProtocolManager.SenderKey) {
        prefs.edit()
            .putString("senderkey_$chatId", Base64.encodeToString(senderKey.secretKey, Base64.NO_WRAP))
            .putInt("senderkey_id_$chatId", senderKey.keyId)
            .apply()
    }
    
    fun getSenderKey(chatId: String): SignalProtocolManager.SenderKey? {
        val encoded = prefs.getString("senderkey_$chatId", null) ?: return null
        val keyId = prefs.getInt("senderkey_id_$chatId", -1)
        return SignalProtocolManager.SenderKey(keyId, Base64.decode(encoded, Base64.NO_WRAP))
    }
}

class SignalPreKeyStore(private val context: Context) {
    fun storePreKey(preKeyId: Int, publicKey: ByteArray) {}
    fun getPreKey(preKeyId: Int): ByteArray? = null
}

class SignalSenderKeyStore(private val context: Context) {
    fun storeSenderKey(chatId: String, keyId: Int, key: ByteArray) {}
    fun getSenderKey(chatId: String, keyId: Int): ByteArray? = null
}
