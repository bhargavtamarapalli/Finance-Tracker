package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.JsonDataManager
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
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
    val isGuest: Boolean = false,
    val role: String = if (isGuest) {
        "GUEST"
    } else if (com.example.BuildConfig.DEBUG && email.startsWith("admin", ignoreCase = true)) {
        "ADMIN"
    } else {
        "USER"
    }
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

class AuthRepository(
    private val context: Context,
    private val database: com.example.data.local.AppDatabase? = null,
    injectedAuthPrefs: android.content.SharedPreferences? = null,
    private val forceDemoFallback: Boolean? = null
) {
    private var auth: FirebaseAuth? = null
    private var useDemoFallback = false

    /**
     * Lazily-resolved SharedPreferences for auth session state.
     * In production: backed by [com.example.data.local.EncryptedPrefsManager] (Keystore).
     * In tests: injected as [injectedAuthPrefs] to avoid requiring the Keystore.
     */
    private val authPrefs: android.content.SharedPreferences by lazy {
        injectedAuthPrefs
            ?: com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
    }

    
    private val _currentUserSession = MutableStateFlow<UserSession?>(null)
    val currentUserSession: StateFlow<UserSession?> = _currentUserSession

    init {
        initializeFirebase()
        checkCurrentUser()
    }

    fun getContext(): Context = context

    private fun initializeFirebase() {
        if (forceDemoFallback == true) {
            auth = null
            useDemoFallback = true
            Log.d("AuthRepository", "Forced local demo fallback")
            return
        }
        try {
            val app = FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()
            val isPlaceholder = app.options.projectId?.contains("placeholder", ignoreCase = true) == true ||
                app.options.apiKey.isNullOrBlank()
            if (isPlaceholder) throw IllegalStateException("Firebase configuration is missing or placeholder")
            auth = FirebaseAuth.getInstance(app)
        } catch (e: Exception) {
            auth = null
            useDemoFallback = forceDemoFallback ?: BuildConfig.DEBUG
            Log.e(
                "AuthRepository",
                if (useDemoFallback) "Firebase unavailable; using debug-only demo authentication" else "Firebase unavailable; authentication is disabled",
                e
            )
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
            val prefs = authPrefs
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
                val isSessionActive = prefs.getBoolean("demo_session_active", false)
                if (demoUserEmail != null && isSessionActive && useDemoFallback) {
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

    /**
     * Persists the user's identity into EncryptedSharedPreferences so that biometric
     * sign-in can restore the correct session on the next launch.
     *
     * @param userId The Firebase UID (or "demo_user" in offline mode)
     * @param email  The user's email address
     * @param name   The user's display name
     */
    private fun saveBiometricSession(userId: String, email: String, name: String) {
        authPrefs
            .edit()
            .putString("biometric_user_id", userId)
            .putString("biometric_user_email", email)
            .putString("biometric_user_name", name)
            .apply()
    }

    private suspend fun clearLocalData() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            database?.clearAllTables()
            JsonDataManager(context).clearLocalFiles()
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): UserSession {
        if (useDemoFallback || auth == null) {
            // Simulated local Firebase fallback
            val prefs = authPrefs
            val salt = PasswordHasher.generateSalt()
            val passwordHash = PasswordHasher.hashPassword(password, salt)
            
            prefs.edit()
                .putString("demo_user_email", email)
                .putString("demo_user_name", name)
                .putString("demo_user_salt", salt)
                .putString("demo_user_password_hash", passwordHash)
                .putBoolean("is_guest", false)
                .putBoolean("demo_session_active", true)
                .apply()
            
            val session = UserSession(
                userId = "demo_user",
                name = name,
                email = email,
                isGuest = false
            )
            clearLocalData()
            _currentUserSession.value = session
            // Phase 3: persist for biometric sign-in
            saveBiometricSession(session.userId, session.email, session.name)
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
            clearLocalData()
            _currentUserSession.value = session
            // Phase 3: persist for biometric sign-in
            saveBiometricSession(session.userId, session.email, session.name)
            return session
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign up failed")
            throw e
        }
    }

    suspend fun signInWithEmail(email: String, password: String): UserSession {
        if (useDemoFallback || auth == null) {
            // Simulated local Firebase fallback
            val prefs = authPrefs
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
                    .putBoolean("demo_session_active", true)
                    .apply()
                val session = UserSession(
                    userId = "demo_user",
                    name = savedName ?: "Alex Mitchell",
                    email = email,
                    isGuest = false
                )
                clearLocalData()
                _currentUserSession.value = session
                // Phase 3: persist for biometric sign-in
                saveBiometricSession(session.userId, session.email, session.name)
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
            clearLocalData()
            _currentUserSession.value = session
            // Phase 3: persist for biometric sign-in
            saveBiometricSession(session.userId, session.email, session.name)
            return session
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign in failed")
            throw e
        }
    }

    suspend fun sendPasswordResetEmail(email: String) {
        if (useDemoFallback || auth == null) {
            val maskedEmail = if (email.contains("@")) {
                val parts = email.split("@")
                val name = parts[0]
                val domain = parts[1]
                val maskedName = if (name.length > 2) name.take(2) + "***" else "***"
                "$maskedName@$domain"
            } else {
                "***"
            }
            Log.d("AuthRepository", "Password reset link sent to $maskedEmail")
            return
        }
        try {
            auth!!.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Password reset failed")
            throw e
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String): UserSession {
        if (useDemoFallback || auth == null) {
            // Simulated local Google auth fallback (offline/test mode only)
            // NOTE: In online mode the idToken comes from Google Credential Manager and
            // carries the real account's display name and email inside the JWT payload.
            // We decode it here so the fallback session reflects the actual selected account.
            val (email, name) = decodeGoogleIdTokenClaims(idToken)
            val prefs = authPrefs
            prefs.edit()
                .putString("demo_user_email", email)
                .putString("demo_user_name", name)
                .putBoolean("is_guest", false)
                .putBoolean("demo_session_active", true)
                .apply()
            val session = UserSession(
                userId = "demo_user",
                name = name,
                email = email,
                isGuest = false
            )
            clearLocalData()
            _currentUserSession.value = session
            // Phase 3: persist for biometric sign-in
            saveBiometricSession(session.userId, session.email, session.name)
            return session
        }

        try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = auth!!.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Google login returned an empty user profile")
            val session = UserSession(
                userId = user.uid,
                name = user.displayName ?: user.email?.substringBefore("@") ?: "Google User",
                email = user.email ?: "",
                isGuest = false
            )
            clearLocalData()
            _currentUserSession.value = session
            // Phase 3: persist for biometric sign-in
            saveBiometricSession(session.userId, session.email, session.name)
            return session
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google sign in failed")
            throw e
        }
    }

    /**
     * Decodes the email and name claims from a Google ID Token JWT without verifying
     * the signature (signature verification is performed server-side by Firebase Auth).
     * Used only in offline/demo fallback mode.
     *
     * @param idToken A compact JWT string from Google Credential Manager.
     * @return A [Pair] of (email, displayName) extracted from the token payload,
     *         or safe defaults if decoding fails.
     */
    private fun decodeGoogleIdTokenClaims(idToken: String): Pair<String, String> {
        return try {
            val parts = idToken.split(".")
            if (parts.size < 2) return Pair("user@gmail.com", "Google User")
            val paddedPayload = parts[1].let {
                val mod = it.length % 4
                if (mod == 0) it else it + "=".repeat(4 - mod)
            }
            val decoded = String(
                android.util.Base64.decode(paddedPayload, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP),
                Charsets.UTF_8
            )
            val email = Regex("\"email\":\"([^\"]+)\"").find(decoded)?.groupValues?.get(1) ?: "user@gmail.com"
            val name = Regex("\"name\":\"([^\"]+)\"").find(decoded)?.groupValues?.get(1)
                ?: email.substringBefore("@")
            Pair(email, name)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to decode Google id_token claims", e)
            throw IllegalArgumentException("Could not decode the Google ID token payload. The token may be malformed.", e)
        }
    }

    /**
     * Restores the user session after a successful biometric authentication.
     *
     * Phase 3 fix: instead of always returning a hardcoded `"demo_user"` session,
     * this method reads the identity that was persisted during the last successful
     * login (email, Google, or sign-up) and reconstructs the correct [UserSession].
     *
     * If a real Firebase session is still active for the stored UID it is reused;
     * otherwise the locally-cached credentials are used (offline mode).
     */
    suspend fun signInWithBiometrics(): UserSession {
        val prefs = authPrefs

        // Read the identity saved during the last successful login (Phase 3)
        val savedUserId = prefs.getString("biometric_user_id", null)
        val savedEmail  = prefs.getString("biometric_user_email", null)
        val savedName   = prefs.getString("biometric_user_name", null)

        if (savedUserId == null || savedEmail == null) {
            throw Exception("No saved account found. Please log in with your password first to enable biometric login.")
        }

        // If Firebase is active and the persisted UID matches the current Firebase user,
        // return a session based on the live Firebase profile.
        val firebaseUser = auth?.currentUser
        if (!useDemoFallback && firebaseUser != null && firebaseUser.uid == savedUserId) {
            val session = UserSession(
                userId = firebaseUser.uid,
                name = firebaseUser.displayName ?: savedName ?: savedEmail.substringBefore("@"),
                email = firebaseUser.email ?: savedEmail,
                isGuest = false
            )
            _currentUserSession.value = session
            return session
        }

        // Offline / demo fallback — use the locally persisted credentials
        prefs.edit()
            .putBoolean("is_guest", false)
            .putBoolean("demo_session_active", true)
            .apply()

        val session = UserSession(
            userId = savedUserId,
            name = savedName ?: savedEmail.substringBefore("@"),
            email = savedEmail,
            isGuest = false
        )
        _currentUserSession.value = session
        return session
    }

    suspend fun loginAsGuest() {
        val session = UserSession(
            userId = "guest_user",
            name = "Guest User",
            email = "guest@example.com",
            isGuest = true
        )
        clearLocalData()
        authPrefs
            .edit()
            .putBoolean("is_guest", true)
            .apply()
        _currentUserSession.value = session
    }

    suspend fun updateProfileName(newName: String) {
        if (useDemoFallback || auth == null) {
            val prefs = authPrefs
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
            val prefs = authPrefs
            prefs.edit().putString("demo_user_email", newEmail).apply()
            _currentUserSession.value = _currentUserSession.value?.copy(email = newEmail)
            return
        }
        try {
            val user = auth?.currentUser ?: throw Exception("No authenticated user found")
            user.verifyBeforeUpdateEmail(newEmail).await()
            // The local session is updated to reflect the change request; Firebase requires link verification
            _currentUserSession.value = _currentUserSession.value?.copy(email = newEmail)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update profile email")
            throw e
        }
    }

    /**
     * Signs the current user out and **clears all locally-cached financial data**
     * from the Room database.
     *
     * Phase 2 fix: without this wipe, the next user who logs in on the same device
     * would immediately see the previous user's transactions and categories.
     *
     * The biometric_* keys are intentionally kept so that biometric login can still
     * prompt the correct account name on the lock screen.  They are overwritten the
     * moment a different account logs in successfully.
     */
    suspend fun logout() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                auth?.signOut()
            } catch (e: Exception) {
                Log.e("AuthRepository", "Firebase sign-out failed; local session will still be cleared", e)
            }

            // Phase 2: wipe all local financial data so it cannot leak to the next user
            try {
                database?.clearAllTables()
                JsonDataManager(context).clearLocalFiles()
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to clear local database on logout", e)
            }

            authPrefs
                .edit()
                .putBoolean("is_guest", false)
                .putBoolean("demo_session_active", false)
                .remove("biometric_user_id")
                .remove("biometric_user_email")
                .remove("biometric_user_name")
                .apply()
            _currentUserSession.value = null
        }
    }
}
