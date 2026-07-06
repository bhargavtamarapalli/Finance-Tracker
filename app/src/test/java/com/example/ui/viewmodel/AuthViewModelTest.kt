package com.example.ui.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.EncryptedPrefsManager
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var repository: AuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        val prefs = EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
        prefs.edit().clear().commit()
        
        repository = AuthRepository(context)
        viewModel = AuthViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSignIn_emptyFields_setsErrorState() {
        viewModel.signIn("", "password")
        assertTrue(viewModel.authState.value is AuthState.Error)
        assertEquals("Please fill in all fields", (viewModel.authState.value as AuthState.Error).message)
    }

    @Test
    fun testSignIn_invalidEmail_setsErrorState() {
        viewModel.signIn("invalid-email", "password")
        assertTrue(viewModel.authState.value is AuthState.Error)
        assertEquals("Invalid email format", (viewModel.authState.value as AuthState.Error).message)
    }

    @Test
    fun testSignUp_emptyFields_setsErrorState() {
        viewModel.signUp("email@example.com", "", "Name")
        assertTrue(viewModel.authState.value is AuthState.Error)
        assertEquals("Please fill in all fields", (viewModel.authState.value as AuthState.Error).message)
    }

    @Test
    fun testSignUp_passwordTooShort_setsErrorState() {
        viewModel.signUp("email@example.com", "12345", "Name")
        assertTrue(viewModel.authState.value is AuthState.Error)
        assertEquals("Password must be at least 6 characters", (viewModel.authState.value as AuthState.Error).message)
    }

    @Test
    fun testSignIn_success_setsSuccessStateWithUserRole() {
        // First register
        viewModel.signUp("user@example.com", "Password123", "User Name")
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(viewModel.authState.value is AuthState.Success)
        var session = (viewModel.authState.value as AuthState.Success).session
        assertEquals("USER", session.role)

        // Then login
        viewModel.signIn("user@example.com", "Password123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.authState.value is AuthState.Success)
        session = (viewModel.authState.value as AuthState.Success).session
        assertEquals("USER", session.role)
    }

    @Test
    fun testSignIn_admin_success_setsSuccessStateWithAdminRole() {
        viewModel.signUp("admin@example.com", "Password123", "Admin User")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.authState.value is AuthState.Success)
        val session = (viewModel.authState.value as AuthState.Success).session
        assertEquals("ADMIN", session.role)
    }

    @Test
    fun testLoginAsGuest_setsGuestSessionWithGuestRole() {
        viewModel.loginAsGuest()
        testDispatcher.scheduler.advanceUntilIdle()

        val session = viewModel.currentUserSession.value
        org.junit.Assert.assertNotNull(session)
        assertEquals("GUEST", session!!.role)
    }

    @Test
    fun testLogout_clearsSession() {
        viewModel.loginAsGuest()
        testDispatcher.scheduler.advanceUntilIdle()
        org.junit.Assert.assertNotNull(viewModel.currentUserSession.value)

        viewModel.logout()
        org.junit.Assert.assertNull(viewModel.currentUserSession.value)
        assertTrue(viewModel.authState.value is AuthState.Idle)
    }
}
