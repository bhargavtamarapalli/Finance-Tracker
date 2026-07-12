package com.example.ui.viewmodel

import com.example.data.repository.AuthRepository
import com.example.data.repository.UserSession
import androidx.lifecycle.ViewModel
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @MockK
    private lateinit var mockRepository: AuthRepository

    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val sessionFlow = MutableStateFlow<UserSession?>(null)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        val dummyContext = mockk<android.content.Context>(relaxed = true)
        val dummyNm = mockk<android.app.NotificationManager>(relaxed = true)
        every { dummyContext.getSystemService(android.content.Context.NOTIFICATION_SERVICE) } returns dummyNm
        every { mockRepository.getContext() } returns dummyContext

        every { mockRepository.currentUserSession } returns sessionFlow
        viewModel = AuthViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // --- signIn ---

    @Test
    fun testSignIn_emptyEmail_setsError() {
        viewModel.signIn("", "password")
        assertEquals(AuthState.Error("Please fill in all fields"), viewModel.authState.value)
    }

    @Test
    fun testSignIn_emptyPassword_setsError() {
        viewModel.signIn("user@example.com", "")
        assertEquals(AuthState.Error("Please fill in all fields"), viewModel.authState.value)
    }

    @Test
    fun testSignIn_invalidEmail_setsError() {
        viewModel.signIn("not-an-email", "password123")
        assertEquals(AuthState.Error("Invalid email format"), viewModel.authState.value)
    }

    @Test
    fun testSignIn_validCredentials_emitsSuccess() = runTest {
        val session = UserSession("uid-1", "Alice", "alice@example.com")
        coEvery { mockRepository.signInWithEmail("alice@example.com", "pass123") } returns session

        viewModel.signIn("alice@example.com", "pass123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthState.Success(session), viewModel.authState.value)
    }

    @Test
    fun testSignIn_failure_emitsError() = runTest {
        coEvery { mockRepository.signInWithEmail(any(), any()) } throws Exception("Invalid credentials")

        viewModel.signIn("alice@example.com", "wrongpass")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.authState.value is AuthState.Error)
        assertEquals("Invalid credentials", (viewModel.authState.value as AuthState.Error).message)
    }

    // --- signUp ---

    @Test
    fun testSignUp_emptyName_setsError() {
        viewModel.signUp("user@example.com", "pass123", "")
        assertEquals(AuthState.Error("Please fill in all fields"), viewModel.authState.value)
    }

    @Test
    fun testSignUp_shortPassword_setsError() {
        viewModel.signUp("user@example.com", "abc", "Alice")
        assertEquals(AuthState.Error("Password must be at least 6 characters"), viewModel.authState.value)
    }

    @Test
    fun testSignUp_invalidEmail_setsError() {
        viewModel.signUp("bad-email", "pass123", "Alice")
        assertEquals(AuthState.Error("Invalid email format"), viewModel.authState.value)
    }

    @Test
    fun testSignUp_validData_emitsSuccess() = runTest {
        val session = UserSession("uid-2", "Bob", "bob@example.com")
        coEvery { mockRepository.signUpWithEmail("bob@example.com", "pass1234", "Bob") } returns session

        viewModel.signUp("bob@example.com", "pass1234", "Bob")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthState.Success(session), viewModel.authState.value)
    }

    @Test
    fun testSignUp_failure_emitsError() = runTest {
        coEvery { mockRepository.signUpWithEmail(any(), any(), any()) } throws Exception("Email already in use")

        viewModel.signUp("bob@example.com", "pass1234", "Bob")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.authState.value is AuthState.Error)
        assertEquals("Email already in use", (viewModel.authState.value as AuthState.Error).message)
    }

    // --- sendPasswordReset ---

    @Test
    fun testSendPasswordReset_emptyEmail_setsError() {
        viewModel.sendPasswordReset("") {}
        assertEquals(AuthState.Error("Please enter your email address"), viewModel.authState.value)
    }

    @Test
    fun testSendPasswordReset_invalidEmail_setsError() {
        viewModel.sendPasswordReset("invalid-email") {}
        assertEquals(AuthState.Error("Invalid email format"), viewModel.authState.value)
    }

    @Test
    fun testSendPasswordReset_success_callsOnSuccessAndResetsState() = runTest {
        coEvery { mockRepository.sendPasswordResetEmail(any()) } just Runs
        var successCalled = false

        viewModel.sendPasswordReset("user@example.com") { successCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals(AuthState.Idle, viewModel.authState.value)
    }

    @Test
    fun testSendPasswordReset_failure_emitsError() = runTest {
        coEvery { mockRepository.sendPasswordResetEmail(any()) } throws Exception("No such user")

        viewModel.sendPasswordReset("user@example.com") {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.authState.value is AuthState.Error)
        assertEquals("No such user", (viewModel.authState.value as AuthState.Error).message)
    }

    // --- loginAsGuest ---

    @Test
    fun testLoginAsGuest_delegatesToRepository() {
        every { mockRepository.loginAsGuest() } just Runs
        viewModel.loginAsGuest()
        verify(exactly = 1) { mockRepository.loginAsGuest() }
    }

    // --- signInWithBiometrics ---

    @Test
    fun testSignInWithBiometrics_success_emitsSuccess() = runTest {
        val session = UserSession("bio-user", "Alice", "alice@example.com")
        coEvery { mockRepository.signInWithBiometrics() } returns session

        viewModel.signInWithBiometrics()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthState.Success(session), viewModel.authState.value)
    }

    @Test
    fun testSignInWithBiometrics_failure_emitsError() = runTest {
        coEvery { mockRepository.signInWithBiometrics() } throws Exception("Hardware not available")

        viewModel.signInWithBiometrics()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.authState.value is AuthState.Error)
        assertEquals("Hardware not available", (viewModel.authState.value as AuthState.Error).message)
    }

    // --- logout ---

    @Test
    fun testLogout_delegatesToRepositoryAndResetsStateToIdle() {
        every { mockRepository.logout() } just Runs
        viewModel.logout()
        verify(exactly = 1) { mockRepository.logout() }
        assertEquals(AuthState.Idle, viewModel.authState.value)
    }

    // --- setError / clearError ---

    @Test
    fun testSetError_updatesStateToError() {
        viewModel.setError("Something went wrong")
        assertEquals(AuthState.Error("Something went wrong"), viewModel.authState.value)
    }

    @Test
    fun testClearError_whenStateIsError_resetsToIdle() {
        viewModel.setError("Some error")
        viewModel.clearError()
        assertEquals(AuthState.Idle, viewModel.authState.value)
    }

    @Test
    fun testClearError_whenStateIsNotError_doesNothing() {
        viewModel.clearError()
        assertEquals(AuthState.Idle, viewModel.authState.value)
    }

    // --- updateProfile ---

    @Test
    fun testUpdateProfile_blankName_callsOnErrorAndSetsErrorState() = runTest {
        var errorMsg: String? = null
        var successCalled = false

        viewModel.updateProfile("", "new@example.com", { successCalled = true }, { errorMsg = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals("Name cannot be blank", errorMsg)
        assertEquals(AuthState.Error("Name cannot be blank"), viewModel.authState.value)
    }

    @Test
    fun testUpdateProfile_validName_noEmailChange_success() = runTest {
        var errorMsg: String? = null
        var successCalled = false

        val currentSession = UserSession("uid-1", "OldName", "alice@example.com", isGuest = false)
        sessionFlow.value = currentSession

        coEvery { mockRepository.updateProfileName("NewName") } just Runs
        coEvery { mockRepository.currentUserSession } returns sessionFlow

        viewModel.updateProfile("NewName", "alice@example.com", { successCalled = true }, { errorMsg = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertNull(errorMsg)
        coVerify(exactly = 1) { mockRepository.updateProfileName("NewName") }
        coVerify(exactly = 0) { mockRepository.updateProfileEmail(any()) }
        assertEquals(AuthState.Success(currentSession), viewModel.authState.value)
    }

    @Test
    fun testUpdateProfile_validName_guestUser_doesNotUpdateEmail() = runTest {
        var errorMsg: String? = null
        var successCalled = false

        val currentSession = UserSession("uid-1", "OldName", "alice@example.com", isGuest = true)
        sessionFlow.value = currentSession

        coEvery { mockRepository.updateProfileName("NewName") } just Runs
        coEvery { mockRepository.currentUserSession } returns sessionFlow

        viewModel.updateProfile("NewName", "newemail@example.com", { successCalled = true }, { errorMsg = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertNull(errorMsg)
        coVerify(exactly = 1) { mockRepository.updateProfileName("NewName") }
        coVerify(exactly = 0) { mockRepository.updateProfileEmail(any()) }
    }

    @Test
    fun testUpdateProfile_validName_nonGuestUser_invalidEmail_error() = runTest {
        var errorMsg: String? = null
        var successCalled = false

        val currentSession = UserSession("uid-1", "OldName", "alice@example.com", isGuest = false)
        sessionFlow.value = currentSession

        coEvery { mockRepository.updateProfileName("NewName") } just Runs

        viewModel.updateProfile("NewName", "invalid-email", { successCalled = true }, { errorMsg = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals("Invalid email format", errorMsg)
        assertEquals(AuthState.Error("Invalid email format"), viewModel.authState.value)
    }

    @Test
    fun testUpdateProfile_validName_nonGuestUser_newValidEmail_success() = runTest {
        var errorMsg: String? = null
        var successCalled = false

        val currentSession = UserSession("uid-1", "OldName", "alice@example.com", isGuest = false)
        val updatedSession = UserSession("uid-1", "NewName", "newemail@example.com", isGuest = false)
        sessionFlow.value = currentSession

        coEvery { mockRepository.updateProfileName("NewName") } just Runs
        coEvery { mockRepository.updateProfileEmail("newemail@example.com") } coAnswers {
            sessionFlow.value = updatedSession
            Runs
        }
        coEvery { mockRepository.currentUserSession } returns sessionFlow

        viewModel.updateProfile("NewName", "newemail@example.com", { successCalled = true }, { errorMsg = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertNull(errorMsg)
        coVerify(exactly = 1) { mockRepository.updateProfileName("NewName") }
        coVerify(exactly = 1) { mockRepository.updateProfileEmail("newemail@example.com") }
        assertEquals(AuthState.Success(updatedSession), viewModel.authState.value)
    }

    @Test
    fun testUpdateProfile_finalSessionNull_resetsToIdle() = runTest {
        var errorMsg: String? = null
        var successCalled = false

        val currentSession = UserSession("uid-1", "OldName", "alice@example.com", isGuest = false)
        sessionFlow.value = currentSession

        coEvery { mockRepository.updateProfileName("NewName") } answers {
            sessionFlow.value = null
        }
        coEvery { mockRepository.currentUserSession } returns sessionFlow

        viewModel.updateProfile("NewName", "alice@example.com", {
            successCalled = true
        }, { errorMsg = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertNull(errorMsg)
        assertEquals(AuthState.Idle, viewModel.authState.value)
    }

    @Test
    fun testUpdateProfile_repositoryThrows_error() = runTest {
        var errorMsg: String? = null
        var successCalled = false

        coEvery { mockRepository.updateProfileName(any()) } throws Exception("Database timeout")

        viewModel.updateProfile("NewName", "alice@example.com", { successCalled = true }, { errorMsg = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals("Database timeout", errorMsg)
        assertEquals(AuthState.Error("Database timeout"), viewModel.authState.value)
    }

    // --- AuthViewModelFactory ---

    @Test
    fun testAuthViewModelFactory_createsAuthViewModel() {
        val factory = AuthViewModelFactory(mockRepository)
        val vm = factory.create(AuthViewModel::class.java)
        assertNotNull(vm)
        assertTrue(vm is AuthViewModel)
    }

    @Test
    fun testAuthViewModelFactory_unknownClass_throwsIllegalArgumentException() {
        val factory = AuthViewModelFactory(mockRepository)
        class UnknownViewModel : ViewModel()
        try {
            factory.create(UnknownViewModel::class.java)
            fail("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Unknown ViewModel class", e.message)
        }
    }
}
