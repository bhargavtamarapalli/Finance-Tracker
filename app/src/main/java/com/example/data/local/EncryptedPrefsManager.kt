package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages access to [EncryptedSharedPreferences] backed by an AES-256 master key
 * stored in the Android Keystore.
 *
 * This object is production-only. Test doubles must implement or mock
 * [android.content.SharedPreferences] directly; no test-environment detection is performed here.
 */
object EncryptedPrefsManager {
    private const val TAG = "EncryptedPrefsManager"

    /**
     * Returns an [EncryptedSharedPreferences] instance for the given [name].
     *
     * @param context Application or Activity context used to create the preferences store.
     * @param name    The preferences file name (must be unique per use-case).
     * @throws SecureStorageUnavailableException if the Keystore or encrypted prefs
     *         cannot be initialized (e.g., device credential reset, hardware failure).
     */
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
            Log.e(TAG, "Failed to create EncryptedSharedPreferences for $name")
            throw SecureStorageUnavailableException(
                "Secure storage is unavailable. Please restart the app or reset device credentials.",
                e
            )
        }
    }
}

class SecureStorageUnavailableException(message: String, cause: Throwable) : IllegalStateException(message, cause)
