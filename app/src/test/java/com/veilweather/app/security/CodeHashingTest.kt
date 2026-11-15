package com.veilweather.app.security

import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class CodeHashingTest {

    @Test
    fun pbkdf2_produces_256_bit_key() {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec("8546".toCharArray(), ByteArray(32), 1000, 256)
        val key = factory.generateSecret(spec)
        assertTrue("expected 32-byte derived key", key.encoded.size == 32)
    }

    @Test
    fun pbkdf2_is_deterministic_for_same_salt() {
        val salt = ByteArray(32) { it.toByte() }
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val a = factory.generateSecret(PBEKeySpec("1111".toCharArray(), salt, 1000, 256)).encoded
        val b = factory.generateSecret(PBEKeySpec("1111".toCharArray(), salt, 1000, 256)).encoded
        assertTrue("same salt+code must produce same hash", a.contentEquals(b))
    }

    @Test
    fun pbkdf2_differs_for_different_codes() {
        val salt = ByteArray(32) { it.toByte() }
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val a = factory.generateSecret(PBEKeySpec("8546".toCharArray(), salt, 1000, 256)).encoded
        val b = factory.generateSecret(PBEKeySpec("1111".toCharArray(), salt, 1000, 256)).encoded
        assertTrue("different codes must produce different hashes", !a.contentEquals(b))
    }
}
