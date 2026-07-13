package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val session: UserSession) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val repository: AuthRepository,
    val notificationManager: com.example.data.repository.NotificationManager = com.example.data.repository.NoOpNotificationManager
) : ViewModel() {
    
    val currentUserSession: StateFlow<UserSession?> = repository.currentUserSession
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotBlank() && "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex().matches(trimmed)
    }

    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun signIn(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }
        if (!isValidEmail(trimmedEmail)) {
            _authState.value = AuthState.Error("Invalid email format")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val session = repository.signInWithEmail(trimmedEmail, password)
                _authState.value = AuthState.Success(session)
                notificationManager.postInApp("Welcome back, ${session.name}!")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapAuthError(e))
            }
        }
    }

    fun signUp(email: String, password: String, name: String) {
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()
        if (trimmedEmail.isBlank() || password.isBlank() || trimmedName.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }
        if (!isValidEmail(trimmedEmail)) {
            _authState.value = AuthState.Error("Invalid email format")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val session = repository.signUpWithEmail(trimmedEmail, password, trimmedName)
                _authState.value = AuthState.Success(session)
                notificationManager.postInApp("Account created successfully. Welcome, ${session.name}!")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapAuthError(e))
            }
        }
    }

    fun sendPasswordReset(email: String, onSuccess: () -> Unit) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            _authState.value = AuthState.Error("Please enter your email address")
            return
        }
        if (!isValidEmail(trimmedEmail)) {
            _authState.value = AuthState.Error("Invalid email format")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                repository.sendPasswordResetEmail(trimmedEmail)
                _authState.value = AuthState.Idle
                notificationManager.postInApp("Password reset link sent to $trimmedEmail.")
                onSuccess()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapAuthError(e))
            }
        }
    }

    fun loginAsGuest() {
        viewModelScope.launch {
            try {
                repository.loginAsGuest()
                notificationManager.postInApp("Logged in as Guest. Local features active.", com.example.data.model.NotificationType.INFO)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Secure storage is unavailable.")
            }
        }
    }

    fun signInWithBiometrics() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val session = repository.signInWithBiometrics()
                _authState.value = AuthState.Success(session)
                notificationManager.postInApp("Welcome back, ${session.name}!")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Biometric login failed.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                repository.logout()
                _authState.value = AuthState.Idle
                notificationManager.postInApp("Logged out successfully.", com.example.data.model.NotificationType.INFO)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to log out safely.")
            }
        }
    }

    fun updateProfile(name: String, email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                if (trimmedName.isBlank()) {
                    throw Exception("Name cannot be blank")
                }
                repository.updateProfileName(trimmedName)
                
                val currentSession = currentUserSession.value
                if (currentSession != null && !currentSession.isGuest && trimmedEmail.isNotBlank() && trimmedEmail != currentSession.email) {
                    if (!isValidEmail(trimmedEmail)) {
                        throw Exception("Invalid email format")
                    }
                    repository.updateProfileEmail(trimmedEmail)
                }
                
                val finalSession = repository.currentUserSession.value
                if (finalSession != null) {
                    _authState.value = AuthState.Success(finalSession)
                } else {
                    _authState.value = AuthState.Idle
                }
                notificationManager.postInApp("Profile updated successfully.")
                onSuccess()
            } catch (e: Exception) {
                val mapped = mapAuthError(e)
                _authState.value = AuthState.Error(mapped)
                onError(mapped)
            }
        }
    }
    
    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }

    fun continueWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val session = repository.signInWithGoogleCredential(idToken)
                _authState.value = AuthState.Success(session)
                notificationManager.postInApp("Welcome back, ${session.name}!")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapAuthError(e))
            }
        }
    }

    private fun mapAuthError(e: Throwable): String {
        val message = e.message ?: ""
        return when {
            message.contains("already in use", ignoreCase = true) || message.contains("ALREADY_EXISTS", ignoreCase = true) ->
                "This email address is already registered. Please sign in instead."
            message.contains("no user record", ignoreCase = true) || message.contains("USER_NOT_FOUND", ignoreCase = true) ->
                "No account found with this email. Please sign up first."
            message.contains("password is invalid", ignoreCase = true) || message.contains("WRONG_PASSWORD", ignoreCase = true) ->
                "Incorrect password. Please check your credentials or reset your password."
            message.contains("badly formatted", ignoreCase = true) || message.contains("INVALID_EMAIL", ignoreCase = true) ->
                "Invalid email format. Please check the spelling."
            message.contains("network error", ignoreCase = true) || message.contains("network", ignoreCase = true) ->
                "Network connection failure. Please check your internet connection and try again."
            else -> e.localizedMessage ?: "Authentication failed. Please verify your details or connection."
        }
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository,
    private val notificationManager: com.example.data.repository.NotificationManager? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val nm = notificationManager ?: com.example.data.repository.NotificationManagerImpl(repository.getContext())
            return AuthViewModel(repository, nm) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
