package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object EncryptedPrefsManager {
    private const val TAG = "EncryptedPrefsManager"

    fun getEncryptedPrefs(context: Context, name: String): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                name,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences for $name", e)
            if (isTestEnvironment()) {
                Log.d(TAG, "Test environment detected; falling back to plaintext SharedPreferences")
                context.getSharedPreferences(name, Context.MODE_PRIVATE)
            } else {
                throw SecureStorageUnavailableException("Secure storage is unavailable. Please restart the app or reset device credentials.", e)
            }
        }
    }

    fun isTestEnvironment(): Boolean {
        return try {
            Class.forName("org.robolectric.Robolectric") != null
        } catch (e: ClassNotFoundException) {
            Thread.currentThread().stackTrace.any {
                it.className.startsWith("org.junit.") ||
                it.className.startsWith("androidx.test.")
            }
        }
    }
}

class SecureStorageUnavailableException(message: String, cause: Throwable) : IllegalStateException(message, cause)
