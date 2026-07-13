package com.example.data.local

import java.security.SecureRandom
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 310_000
    private const val KEY_LENGTH = 256 // in bits
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    /**
     * Generates a random salt of 16 bytes.
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    /**
     * Hashes the password using PBKDF2 with HMAC-SHA256 and the provided salt.
     */
    fun hashPassword(password: String, salt: String): String {
        val saltBytes = Base64.getDecoder().decode(salt)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
        return try {
            val skf = SecretKeyFactory.getInstance(ALGORITHM)
            val hash = skf.generateSecret(spec).encoded
            Base64.getEncoder().encodeToString(hash)
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * Verifies if the candidate password matches the secured password hash.
     */
    fun verifyPassword(password: String, salt: String, securedHash: String): Boolean {
        return MessageDigest.isEqual(
            Base64.getDecoder().decode(hashPassword(password, salt)),
            Base64.getDecoder().decode(securedHash)
        )
    }
}
