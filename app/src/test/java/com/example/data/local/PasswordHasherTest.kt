package com.example.data.local

import io.mockk.*
import javax.crypto.SecretKeyFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun testGenerateSalt_isUniqueAndNotEmpty() {
        val salt1 = PasswordHasher.generateSalt()
        val salt2 = PasswordHasher.generateSalt()
        assertTrue(salt1.isNotEmpty())
        assertTrue(salt2.isNotEmpty())
        assertNotEquals(salt1, salt2)
    }

    @Test
    fun testHashAndPasswordVerification_success() {
        val password = "SecurePassword123"
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(password, salt)
        
        assertTrue(PasswordHasher.verifyPassword(password, salt, hash))
        assertNotEquals(password, hash)
    }

    @Test
    fun testVerifyPassword_failsOnWrongPassword() {
        val password = "SecurePassword123"
        val wrongPassword = "WrongPassword123"
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(password, salt)
        
        assertFalse(PasswordHasher.verifyPassword(wrongPassword, salt, hash))
    }

    @Test
    fun testHashPassword_isDeterministic() {
        val password = "MyTestPassword"
        val salt = PasswordHasher.generateSalt()
        val hash1 = PasswordHasher.hashPassword(password, salt)
        val hash2 = PasswordHasher.hashPassword(password, salt)
        
        assertTrue(hash1.isNotEmpty())
        assertTrue(hash2.isNotEmpty())
        assertTrue(hash1 == hash2)
    }

    @Test
    fun testEmptyPassword_worksCorrectly() {
        val password = ""
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(password, salt)
        assertTrue(PasswordHasher.verifyPassword(password, salt, hash))
    }

    @Test
    fun testVerifyPassword_failsOnWrongSalt() {
        val password = "SecurePassword123"
        val salt1 = PasswordHasher.generateSalt()
        val salt2 = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(password, salt1)
        assertFalse(PasswordHasher.verifyPassword(password, salt2, hash))
    }

    @Test
    fun testHashPassword_failsClosed_onException() {
        mockkStatic(SecretKeyFactory::class)
        every { SecretKeyFactory.getInstance(any()) } throws RuntimeException("Algorithm not found")

        try {
            val password = "SecurePassword123"
            val salt = PasswordHasher.generateSalt()
            PasswordHasher.hashPassword(password, salt)
            org.junit.Assert.fail("Expected hashPassword to throw an exception when PBKDF2 is unavailable")
        } catch (e: Exception) {
            org.junit.Assert.assertEquals("Algorithm not found", e.message)
        } finally {
            unmockkStatic(SecretKeyFactory::class)
        }
    }
}
