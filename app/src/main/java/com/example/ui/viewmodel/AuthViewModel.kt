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

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    
    val currentUserSession: StateFlow<UserSession?> = repository.currentUserSession
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex().matches(email)
    }

    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }
        if (!isValidEmail(email)) {
            _authState.value = AuthState.Error("Invalid email format")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val session = repository.signInWithEmail(email, password)
                _authState.value = AuthState.Success(session)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign in failed. Check your connection or email/password.")
            }
        }
    }

    fun signUp(email: String, password: String, name: String) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }
        if (!isValidEmail(email)) {
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
                val session = repository.signUpWithEmail(email, password, name)
                _authState.value = AuthState.Success(session)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign up failed. Please try again.")
            }
        }
    }

    fun sendPasswordReset(email: String, onSuccess: () -> Unit) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Please enter your email address")
            return
        }
        if (!isValidEmail(email)) {
            _authState.value = AuthState.Error("Invalid email format")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                repository.sendPasswordResetEmail(email)
                _authState.value = AuthState.Idle
                onSuccess()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Reset email failed. Ensure the email is registered.")
            }
        }
    }

    fun loginAsGuest() {
        repository.loginAsGuest()
    }

    fun logout() {
        repository.logout()
        _authState.value = AuthState.Idle
    }
    
    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}

class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
