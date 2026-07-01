package com.example.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import com.example.data.local.PasswordHasher
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class UserSession(
    val userId: String,
    val name: String,
    val email: String,
    val isGuest: Boolean = false
)

// In-place await extension to avoid needing extra library dependencies
suspend fun <T> Task<T>.await(): T {
    if (isComplete) {
        val e = exception
        if (e != null) {
            throw e
        } else {
            return result
        }
    }
    return suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            val e = task.exception
            if (e != null) {
                cont.resumeWithException(e)
            } else {
                cont.resume(task.result)
            }
        }
    }
}

class AuthRepository(private val context: Context) {
    private var auth: FirebaseAuth? = null
    private var useDemoFallback = false
    
    private val _currentUserSession = MutableStateFlow<UserSession?>(null)
    val currentUserSession: StateFlow<UserSession?> = _currentUserSession

    init {
        initializeFirebase()
        checkCurrentUser()
    }

    private fun initializeFirebase() {
        try {
            var isPlaceholder = false
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyPlaceholderKey-1234567890")
                    .setApplicationId("1:123456789012:android:abcdef1234567890")
                    .setProjectId("finance-tracker-placeholder")
                    .build()
                FirebaseApp.initializeApp(context, options)
                isPlaceholder = true
            } else {
                val app = FirebaseApp.getInstance()
                if (app.options.apiKey == "AIzaSyPlaceholderKey-1234567890" || app.options.apiKey.isEmpty()) {
                    isPlaceholder = true
                }
            }
            auth = FirebaseAuth.getInstance()
            if (isPlaceholder) {
                Log.w("AuthRepository", "Using placeholder Firebase configuration. Enabling Demo Fallback.")
                useDemoFallback = true
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to initialize Firebase Auth, using Demo Fallback")
            useDemoFallback = true
        }
    }

    fun checkCurrentUser() {
        val fbUser = auth?.currentUser
        if (fbUser != null && !useDemoFallback) {
            _currentUserSession.value = UserSession(
                userId = fbUser.uid,
                name = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "User",
                email = fbUser.email ?: "",
                isGuest = false
            )
        } else {
            val prefs = com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
            val isGuest = prefs.getBoolean("is_guest", false)
            if (isGuest) {
                _currentUserSession.value = UserSession(
                    userId = "guest_user",
                    name = "Guest User",
                    email = "guest@example.com",
                    isGuest = true
                )
            } else {
                val demoUserEmail = prefs.getString("demo_user_email", null)
                val demoUserName = prefs.getString("demo_user_name", null)
                if (demoUserEmail != null && useDemoFallback) {
                    _currentUserSession.value = UserSession(
                        userId = "demo_user",
                        name = demoUserName ?: "Demo User",
                        email = demoUserEmail,
                        isGuest = false
                    )
                } else {
                    _currentUserSession.value = null
                }
            }
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): UserSession {
        if (useDemoFallback || auth == null) {
            // Simulated local Firebase fallback
            val prefs = com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
            val salt = PasswordHasher.generateSalt()
            val passwordHash = PasswordHasher.hashPassword(password, salt)
            
            prefs.edit()
                .putString("demo_user_email", email)
                .putString("demo_user_name", name)
                .putString("demo_user_salt", salt)
                .putString("demo_user_password_hash", passwordHash)
                .putBoolean("is_guest", false)
                .apply()
            
            val session = UserSession(
                userId = "demo_user",
                name = name,
                email = email,
                isGuest = false
            )
            _currentUserSession.value = session
            return session
        }

        try {
            val result = auth!!.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Registration returned an empty user profile")
            
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdates).await()

            val session = UserSession(
                userId = user.uid,
                name = name,
                email = email,
                isGuest = false
            )
            _currentUserSession.value = session
            return session
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign up failed")
            throw e
        }
    }

    suspend fun signInWithEmail(email: String, password: String): UserSession {
        if (useDemoFallback || auth == null) {
            // Simulated local Firebase fallback
            val prefs = com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
            val savedEmail = prefs.getString("demo_user_email", "demo@example.com")
            val savedSalt = prefs.getString("demo_user_salt", null)
            val savedHash = prefs.getString("demo_user_password_hash", null)
            val savedName = prefs.getString("demo_user_name", "Alex Mitchell")
            
            val isPasswordValid = if (savedSalt != null && savedHash != null) {
                PasswordHasher.verifyPassword(password, savedSalt, savedHash)
            } else {
                false
            }
            
            if (email == savedEmail && isPasswordValid) {
                prefs.edit()
                    .putString("demo_user_email", email)
                    .putBoolean("is_guest", false)
                    .apply()
                val session = UserSession(
                    userId = "demo_user",
                    name = savedName ?: "Alex Mitchell",
                    email = email,
                    isGuest = false
                )
                _currentUserSession.value = session
                return session
            } else {
                throw Exception("Invalid email or password.")
            }
        }

        try {
            val result = auth!!.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Login returned an empty user profile")
            
            val session = UserSession(
                userId = user.uid,
                name = user.displayName ?: email.substringBefore("@"),
                email = email,
                isGuest = false
            )
            _currentUserSession.value = session
            return session
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign in failed")
            throw e
        }
    }

    suspend fun sendPasswordResetEmail(email: String) {
        if (useDemoFallback || auth == null) {
            // Simulated local password reset
            Log.d("AuthRepository", "Password reset link sent to $email")
            return
        }
        try {
            auth!!.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Password reset failed")
            throw e
        }
    }

    suspend fun signInWithBiometrics(): UserSession {
        val prefs = com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
        val savedEmail = prefs.getString("demo_user_email", "demo@example.com")
        val savedName = prefs.getString("demo_user_name", "Alex Mitchell")
        
        prefs.edit()
            .putBoolean("is_guest", false)
            .apply()
            
        val session = UserSession(
            userId = "demo_user",
            name = savedName ?: "Alex Mitchell",
            email = savedEmail ?: "demo@example.com",
            isGuest = false
        )
        _currentUserSession.value = session
        return session
    }

    fun loginAsGuest() {
        val session = UserSession(
            userId = "guest_user",
            name = "Guest User",
            email = "guest@example.com",
            isGuest = true
        )
        com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
            .edit()
            .putBoolean("is_guest", true)
            .apply()
        _currentUserSession.value = session
    }

    suspend fun updateProfileName(newName: String) {
        if (useDemoFallback || auth == null) {
            val prefs = com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
            prefs.edit().putString("demo_user_name", newName).apply()
            _currentUserSession.value = _currentUserSession.value?.copy(name = newName)
            return
        }
        try {
            val user = auth?.currentUser ?: throw Exception("No authenticated user found")
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()
            user.updateProfile(profileUpdates).await()
            _currentUserSession.value = _currentUserSession.value?.copy(name = newName)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update profile name")
            throw e
        }
    }

    suspend fun updateProfileEmail(newEmail: String) {
        if (useDemoFallback || auth == null) {
            val prefs = com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
            prefs.edit().putString("demo_user_email", newEmail).apply()
            _currentUserSession.value = _currentUserSession.value?.copy(email = newEmail)
            return
        }
        try {
            val user = auth?.currentUser ?: throw Exception("No authenticated user found")
            user.updateEmail(newEmail).await()
            _currentUserSession.value = _currentUserSession.value?.copy(email = newEmail)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update profile email")
            throw e
        }
    }

    fun logout() {
        auth?.signOut()
        com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
            .edit()
            .putBoolean("is_guest", false)
            .remove("demo_user_email")
            .apply()
        _currentUserSession.value = null
    }
}
