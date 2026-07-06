package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.EncryptedPrefsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthRepositoryTest {

    private lateinit var context: Context
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs before each test
        val prefs = EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
        prefs.edit().clear().commit()
        
        authRepository = AuthRepository(context)
    }

    @Test
    fun testInitialSession_isNull() = runBlocking {
        // Since we cleared auth_prefs, the initial session should be null
        authRepository.checkCurrentUser()
        val session = authRepository.currentUserSession.value
        assertNull(session)
    }

    @Test
    fun testLoginAsGuest_createsGuestSessionWithGuestRole() = runBlocking {
        authRepository.loginAsGuest()
        
        val session = authRepository.currentUserSession.first()
        assertNotNull(session)
        assertTrue(session!!.isGuest)
        assertEquals("guest_user", session.userId)
        assertEquals("GUEST", session.role)
    }

    @Test
    fun testSignUpAndLogin_normalUser_createsSessionWithUserRole() = runBlocking {
        val email = "user@example.com"
        val password = "Password123"
        val name = "Normal User"

        // Sign Up
        val registeredSession = authRepository.signUpWithEmail(email, password, name)
        ShadowLooper.idleMainLooper()
        assertNotNull(registeredSession)
        assertEquals(email, registeredSession.email)
        assertEquals(name, registeredSession.name)
        assertFalse(registeredSession.isGuest)
        assertEquals("USER", registeredSession.role)

        // Reset memory session and login again
        authRepository.logout()
        ShadowLooper.idleMainLooper()
        assertNull(authRepository.currentUserSession.value)

        val loggedInSession = authRepository.signInWithEmail(email, password)
        assertNotNull(loggedInSession)
        assertEquals("USER", loggedInSession.role)
    }

    @Test
    fun testSignUpAndLogin_adminUser_createsSessionWithAdminRole() = runBlocking {
        val email = "admin@example.com"
        val password = "AdminPassword123"
        val name = "Admin User"

        // Sign Up
        val registeredSession = authRepository.signUpWithEmail(email, password, name)
        ShadowLooper.idleMainLooper()
        assertNotNull(registeredSession)
        assertEquals("ADMIN", registeredSession.role)

        // Reset memory session and login again
        authRepository.logout()
        ShadowLooper.idleMainLooper()
        val loggedInSession = authRepository.signInWithEmail(email, password)
        assertNotNull(loggedInSession)
        assertEquals("ADMIN", loggedInSession.role)
    }

    @Test
    fun testSignInWithWrongPassword_throwsException() = runBlocking {
        val email = "user@example.com"
        val password = "CorrectPassword123"
        val name = "Normal User"

        authRepository.signUpWithEmail(email, password, name)
        ShadowLooper.idleMainLooper()
        
        var threwException = false
        try {
            authRepository.signInWithEmail(email, "WrongPassword123")
        } catch (e: Exception) {
            threwException = true
        }
        assertTrue(threwException)
    }
}
