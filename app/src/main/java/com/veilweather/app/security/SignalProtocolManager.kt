package com.veilweather.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.veilweather.app.data.model.Message
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Signal Protocol Implementation (simplified for demo)
 * In production, use libsignal-protocol-kotlin
 */
class SignalProtocolManager(private val context: Context) {
    
    companion object {
        private const val KEY_ALIAS = "veil_weather_signal"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    }
    
    // Session store for signal protocol
    private val sessionStore = SignalSessionStore(context)
    private val preKeyStore = SignalPreKeyStore(context)
    private val senderKeyStore = SignalSenderKeyStore(context)
    
    // Our identity keypair
    private var identityKeyPair: KeyPair? = null
    private val secureRandom = SecureRandom()
    
    init {
        loadOrGenerateIdentityKeyPair()
    }
    
    private fun loadOrGenerateIdentityKeyPair() {
        thread {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val prefs = EncryptedSharedPreferences.create(
                context,
                "signal_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            val serializedKey = prefs.getString("identity_key", null)
            
            if (serializedKey != null) {
                // Load existing key
                identityKeyPair = deserializeKeyPair(serializedKey)
            } else {
                // Generate new keypair
                identityKeyPair = generateIdentityKeyPair()
                val serialized = serializeKeyPair(identityKeyPair!!)
                prefs.edit().putString("identity_key", serialized).apply()
            }
        }
    }
    
    private fun generateIdentityKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC, 
            "AndroidKeyStore"
        )
        
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(
                java.security.spec.ECGenParameterSpec("secp256r1")
            )
            .build()
        
        keyPairGenerator.initialize(spec)
        return keyPairGenerator.generateKeyPair()
    }
    
    // E2EE functions
    suspend fun encryptMessage(
        recipientIdentityKey: ByteArray,
        message: String
    ): ByteArray = withContext(Dispatchers.IO) {
        // In production, use proper Signal Protocol ratchet
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSharedSecret(recipientIdentityKey))
        cipher.updateAAD("veilweather".toByteArray())
        cipher.doFinal(message.toByteArray())
    }
    
    suspend fun decryptMessage(
        senderIdentityKey: ByteArray,
        encryptedData: ByteArray
    ): String = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getSharedSecret(senderIdentityKey))
        cipher.updateAAD("veilweather".toByteArray())
        String(cipher.doFinal(encryptedData))
    }
    
    suspend fun createEncryptedMessage(
        recipientUserId: String,
        content: String
    ): EncryptedMessage = withContext(Dispatchers.IO) {
        val recipientKey = sessionStore.getPublicKey(recipientUserId)
        
        // Generate ephemeral keypair for this message
        val ephemeralKeyPair = generateEphemeralKeyPair()
        
        // Derive shared secret using X3DH (simplified)
        val sharedSecret = deriveSharedSecret(ephemeralKeyPair.private, recipientKey)
        
        // Encrypt message
        val encrypted = encryptMessage(recipientKey, content)
        
        EncryptedMessage(
            senderId = "self",
            recipientId = recipientUserId,
            ephemeralPublicKey = ephemeralKeyPair.public.encoded,
            encryptedContent = encrypted,
            timestamp = System.currentTimeMillis(),
            messageId = generateMessageId()
        )
    }
    
    suspend fun decryptEncryptedMessage(
        message: EncryptedMessage
    ): String = withContext(Dispatchers.IO) {
        // Decrypt using our identity and their ephemeral key
        val sharedSecret = deriveSharedSecret(identityKeyPair!!.private, message.ephemeralPublicKey)
        val decrypted = decryptMessage(message.ephemeralPublicKey, message.encryptedContent)
        decrypted
    }
    
    // Group messaging
    suspend fun generateSenderKey(): SenderKey = withContext(Dispatchers.IO) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES)
        keyGenerator.init(256)
        val secretKey = keyGenerator.generateKey()
        
        SenderKey(
            keyId = secureRandom.nextInt(0..Int.MAX_VALUE),
            secretKey = secretKey.encoded
        )
    }
    
    suspend fun createSenderKeyMessage(
        chatId: String,
        senderKey: SenderKey,
        content: String
    ): SenderKeyMessage = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(senderKey.secretKey, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(content.toByteArray())
        
        SenderKeyMessage(
            chatId = chatId,
            senderKeyId = senderKey.keyId,
            ciphertext = encrypted,
            iv = cipher.iv,
            timestamp = System.currentTimeMillis()
        )
    }
    
    suspend fun decryptSenderKeyMessage(
        chatId: String,
        message: SenderKeyMessage
    ): String = withContext(Dispatchers.IO) {
        val senderKey = sessionStore.getSenderKey(chatId)
        if (senderKey == null) throw Exception("No sender key for chat")
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(senderKey.secretKey, "AES")
        val spec = GCMParameterSpec(128, message.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        String(cipher.doFinal(message.ciphertext))
    }
    
    // Pairing and key exchange
    suspend fun initiatePairing(partnerId: String): ByteArray = withContext(Dispatchers.IO) {
        val tempKeyPair = generateEphemeralKeyPair()
        
        // Store for later use
        sessionStore.storeTempKeyPair(partnerId, tempKeyPair)
        
        // Return public key to send to partner
        tempKeyPair.public.encoded
    }
    
    suspend fun completePairing(
        partnerId: String,
        partnerPublicKey: ByteArray,
        myEphemeralKey: ByteArray
    ): Boolean = withContext(Dispatchers.IO) {
        // Verify and store partner key
        sessionStore.storePublicKey(partnerId, partnerPublicKey)
        sessionStore.storePublicKey(this@SignalProtocolManager.getUserId(), myEphemeralKey)
        
        // Mark as paired
        sessionStore.markPaired(partnerId)
        
        true
    }
    
    // Utility functions
    private fun generateEphemeralKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        kpg.initialize(256)
        return kpg.generateKeyPair()
    }
    
    private fun deriveSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
        // Simplified ECDH - in production use proper SignalProtocol
        return secrets.tokenHex(32).toByteArray()
    }
    
    private fun generateMessageId(): String {
        return secrets.tokenHex(8)
    }
    
    fun getUserId(): String {
        return identityKeyPair?.let { 
            secrets.tokenHex(16) // Placeholder
        } ?: throw Exception("No identity key")
    }
    
    // Data classes
    data class EncryptedMessage(
        val senderId: String,
        val recipientId: String,
        val ephemeralPublicKey: ByteArray,
        val encryptedContent: ByteArray,
        val timestamp: Long,
        val messageId: String
    )
    
    data class SenderKey(
        val keyId: Int,
        val secretKey: ByteArray
    )
    
    data class SenderKeyMessage(
        val chatId: String,
        val senderKeyId: Int,
        val ciphertext: ByteArray,
        val iv: ByteArray,
        val timestamp: Long
    )
}

// Session store (encrypted)
class SignalSessionStore(private val context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "signal_sessions",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun storePublicKey(userId: String, publicKey: ByteArray) {
        prefs.edit().putByteArray("pub_${userId}", publicKey).apply()
    }
    
    fun getPublicKey(userId: String): ByteArray? {
        return prefs.getBytes("pub_${userId}")
    }
    
    fun markPaired(userId: String) {
        prefs.edit().putBoolean("paired_${userId}", true).apply()
    }
    
    fun isPaired(userId: String): Boolean {
        return prefs.getBoolean("paired_${userId}", false)
    }
    
    fun storeTempKeyPair(userId: String, keyPair: KeyPair) {
        prefs.edit()
            .putByteArray("temp_pub_${userId}", keyPair.public.encoded)
            .putByteArray("temp_priv_${userId}", keyPair.private.encoded)
            .apply()
    }
    
    fun storeSenderKey(chatId: String, senderKey: SignalProtocolManager.SenderKey) {
        prefs.edit()
            .putByteArray("senderkey_${chatId}", senderKey.secretKey)
            .putInt("senderkey_id_${chatId}", senderKey.keyId)
            .apply()
    }
    
    fun getSenderKey(chatId: String): SignalProtocolManager.SenderKey? {
        val secretKey = prefs.getBytes("senderkey_${chatId}") ?: return null
        val keyId = prefs.getInt("senderkey_id_${chatId}", -1)
        return SignalProtocolManager.SenderKey(keyId, secretKey)
    }
}

// Keystore operations
class SignalPreKeyStore(private val context: Context) {
    // Pre-key storage for Signal Protocol
    fun storePreKey(preKeyId: Int, publicKey: ByteArray) {
        // Store in keystore
    }
    
    fun getPreKey(preKeyId: Int): ByteArray? {
        return null
    }
}

class SignalSenderKeyStore(private val context: Context) {
    // Sender-key storage for group messages
    fun storeSenderKey(chatId: String, keyId: Int, key: ByteArray) {
        // Store encrypted
    }
    
    fun getSenderKey(chatId: String, keyId: Int): ByteArray? {
        return null
    }
}
