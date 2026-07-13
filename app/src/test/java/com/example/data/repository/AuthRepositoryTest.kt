package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.fakes.FakeSharedPreferences
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
@Config(sdk = [33])
class AuthRepositoryTest {

    private lateinit var context: Context
    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Use FakeSharedPreferences — no Keystore / EncryptedSharedPreferences needed
        fakePrefs = FakeSharedPreferences()
        authRepository = AuthRepository(context, injectedAuthPrefs = fakePrefs, forceDemoFallback = true)
    }

    @Test
    fun testInitialSession_isNull() = runBlocking {
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

        val registeredSession = authRepository.signUpWithEmail(email, password, name)
        ShadowLooper.idleMainLooper()
        assertNotNull(registeredSession)
        assertEquals(email, registeredSession.email)
        assertEquals(name, registeredSession.name)
        assertFalse(registeredSession.isGuest)
        assertEquals("USER", registeredSession.role)

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

        val registeredSession = authRepository.signUpWithEmail(email, password, name)
        ShadowLooper.idleMainLooper()
        assertNotNull(registeredSession)
        assertEquals("ADMIN", registeredSession.role)

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

    @Test
    fun testSignInWithBiometrics_createsSession() = runBlocking {
        // Seed biometric identity into the shared fake prefs (simulates a prior login)
        fakePrefs.edit()
            .putString("biometric_user_id", "demo_user")
            .putString("biometric_user_email", "bio@example.com")
            .putString("biometric_user_name", "Bio User")
            .apply()

        val session = authRepository.signInWithBiometrics()
        assertNotNull(session)
        assertEquals("bio@example.com", session.email)
        assertEquals("Bio User", session.name)
        assertFalse(session.isGuest)
    }

    @Test
    fun testSendPasswordResetEmail_doesNotCrash() = runBlocking {
        authRepository.sendPasswordResetEmail("reset@example.com")
    }

    @Test
    fun testUpdateProfileName_updatesSessionAndPrefs() = runBlocking {
        authRepository.signUpWithEmail("user@example.com", "Pass123", "Old Name")
        authRepository.updateProfileName("New Name")

        val session = authRepository.currentUserSession.value
        assertNotNull(session)
        assertEquals("New Name", session!!.name)
        assertEquals("New Name", fakePrefs.getString("demo_user_name", null))
    }

    @Test
    fun testUpdateProfileEmail_updatesSessionAndPrefs() = runBlocking {
        authRepository.signUpWithEmail("user@example.com", "Pass123", "Old Name")
        authRepository.updateProfileEmail("new@example.com")

        val session = authRepository.currentUserSession.value
        assertNotNull(session)
        assertEquals("new@example.com", session!!.email)
        assertEquals("new@example.com", fakePrefs.getString("demo_user_email", null))
    }
}
