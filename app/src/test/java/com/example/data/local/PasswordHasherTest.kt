package com.example.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
}
