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
            Log.e(TAG, "Failed to create EncryptedSharedPreferences for $name, falling back to standard prefs", e)
            // Fallback gracefully to standard SharedPreferences to guarantee no crash if Keystore is corrupted
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
        }
    }
}
