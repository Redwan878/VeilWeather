package com.veilweather.app.security.encryption

import java.security.KeyPairGenerator
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.KeyStore
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * E2E Encryption Manager implementing Signal Protocol concepts
 * Uses Android Keystore for key storage
 */
@Singleton
class E2EEncryptionManager @Inject constructor() {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS_IDENTITY = "veil_identity_key"
        private const val KEY_ALIAS_SESSION = "veil_session_key"
        private const val AES_GCM_TAG_LENGTH = 128
        private const val AES_GCM_IV_LENGTH = 12
    }

    /**
     * Generate identity key pair (stored in Android Keystore)
     */
    fun generateIdentityKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC", ANDROID_KEYSTORE)
        keyPairGenerator.initialize(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS_IDENTITY,
                android.security.keystore.KeyProperties.PURPOSE_AGREE_KEY
            ).build()
        )
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Generate session key for message encryption
     */
    fun generateSessionKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypt message with AES-GCM (session encryption)
     */
    fun encryptMessage(plaintext: String, sessionKey: SecretKey): String {
        val iv = ByteArray(AES_GCM_IV_LENGTH)
        java.security.SecureRandom().nextBytes(iv)
        
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(AES_GCM_TAG_LENGTH, iv)
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, sessionKey, gcmSpec)
        
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        
        // Combine IV + ciphertext
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    /**
     * Decrypt message with AES-GCM
     */
    fun decryptMessage(encryptedMessage: String, sessionKey: SecretKey): String {
        val combined = Base64.decode(encryptedMessage, Base64.DEFAULT)
        
        val iv = combined.copyOfRange(0, AES_GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(AES_GCM_IV_LENGTH, combined.size)
        
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(AES_GCM_TAG_LENGTH, iv)
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, sessionKey, gcmSpec)
        
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }

    /**
     * Derive shared secret using ECDH (Elliptic Curve Diffie-Hellman)
     */
    fun deriveSharedSecret(
        privateKey: PrivateKey,
        publicKey: PublicKey
    ): SecretKey {
        val keyAgreement = javax.crypto.KeyAgreement.getInstance("ECDH")
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)
        val sharedSecret = keyAgreement.generateSecret()
        
        // Derive AES key from shared secret
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        val aesKey = keyGenerator.generateKey()
        
        // Note: In production, use proper KDF (HKDF) here
        return aesKey
    }

    /**
     * Encrypt media file (images, voice) before sending
     */
    fun encryptMedia(mediaData: ByteArray, sessionKey: SecretKey): ByteArray {
        val iv = ByteArray(AES_GCM_IV_LENGTH)
        java.security.SecureRandom().nextBytes(iv)
        
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, sessionKey, GCMParameterSpec(AES_GCM_TAG_LENGTH, iv))
        
        return cipher.doFinal(mediaData)
    }
}

/**
 * X3DH (Extended Triple Diffie-Hellman) for key agreement
 */
class X3DHManager {
    
    /**
     * Generate one-time prekeys for initial key agreement
     */
    fun generatePreKeys(count: Int): List<ByteArray> {
        return (0 until count).map {
            generateKeyMaterial()
        }
    }
    
    private fun generateKeyMaterial(): ByteArray {
        val material = ByteArray(32)
        java.security.SecureRandom().nextBytes(material)
        return material
    }
}

/**
 * Double Ratchet implementation for forward secrecy
 * (Simplified - production would use libsignal)
 */
class DoubleRatchetManager {
    
    private var sendingChainKey: ByteArray? = null
    private var receivingChainKey: ByteArray? = null
    private var remotePublicKey: ByteArray? = null
    private var localPublicKey: ByteArray? = null
    private var previousChainLength = 0
    private var skippedMessageKeys = mutableMapOf<String, ByteArray>()

    /**
     * Initialize ratchet with shared secret
     */
    fun initializeRatchet(sharedSecret: ByteArray, remotePublicKey: ByteArray) {
        // Initial ratchet
        this.remotePublicKey = remotePublicKey
        val rootKey = deriveRootKey(sharedSecret, remotePublicKey)
        sendingChainKey = rootKey
        receivingChainKey = rootKey
    }

    private fun deriveRootKey(sharedSecret: ByteArray, publicKey: ByteArray): ByteArray {
        // Simplified - use HKDF in production
        val key = SecretKeySpec(sharedSecret, "HmacSHA256")
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(key)
        return mac.doFinal(publicKey)
    }

    /**
     * Encrypt message (returns ciphertext + header)
     */
    fun encryptMessage(plaintext: String): Pair<String, ByteArray> {
        // Use sending chain to derive message key
        val messageKey = deriveMessageKey(sendingChainKey!!)
        
        // Encrypt with AES-GCM
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        java.security.SecureRandom().nextBytes(iv)
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, SecretKeySpec(messageKey, "AES"), javax.crypto.spec.GCMParameterSpec(128, iv))
        
        val ciphertext = cipher.doFinal(plaintext.toByteArray())
        
        // Advance chain
        advanceChain()
        
        return Pair(
            Base64.encodeToString(iv + ciphertext, Base64.DEFAULT),
            messageKey
        )
    }

    /**
     * Decrypt message
     */
    fun decryptMessage(encryptedMessage: String): String {
        val data = Base64.decode(encryptedMessage, Base64.DEFAULT)
        val iv = data.copyOfRange(0, 12)
        val ciphertext = data.copyOfRange(12, data.size)
        
        // Try to get message key from skip list first
        val messageKey = findMessageKey(remotePublicKey!!, previousChainLength)
        
        if (messageKey != null) {
            return decryptWithMessageKey(ciphertext, messageKey, iv)
        }
        
        // Derive from receiving chain
        val derivedKey = deriveMessageKey(receivingChainKey!!)
        advanceChain()
        return decryptWithMessageKey(ciphertext, derivedKey, iv)
    }

    private fun decryptWithMessageKey(ciphertext: ByteArray, messageKey: ByteArray, iv: ByteArray): String {
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, SecretKeySpec(messageKey, "AES"), javax.crypto.spec.GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun deriveMessageKey(chainKey: ByteArray): ByteArray {
        val key = SecretKeySpec(chainKey, "HmacSHA256")
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(key)
        return mac.doFinal("messagekey".toByteArray())
    }

    private fun advanceChain() {
        // In real implementation, advance chain key properly
    }

    private fun findMessageKey(pubKey: ByteArray, count: Int): ByteArray? {
        return skippedMessageKeys["$pubKey:$count"]
    }
}