package com.example.ui.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

object BiometricHelper {

    private const val KEY_NAME = "biometric_encryption_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val CHALLENGE_STRING = "BIOMETRIC_CHALLENGE_VERIFY_123"

    /**
     * Checks if biometric hardware is present, enabled, and has enrolled credentials.
     */
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * Generates or retrieves the AES key in the Android Keystore that is bound to user authentication.
     */
    fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_NAME)) {
            return keyStore.getKey(KEY_NAME, null) as SecretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val builder = KeyGenParameterSpec.Builder(
            KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Initializes a cipher in the requested mode (ENCRYPT_MODE or DECRYPT_MODE).
     */
    fun getInitializedCipher(opMode: Int, iv: ByteArray? = null): Cipher? {
        return try {
            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}")
            if (opMode == Cipher.DECRYPT_MODE && iv != null) {
                cipher.init(opMode, key, IvParameterSpec(iv))
            } else {
                cipher.init(opMode, key)
            }
            cipher
        } catch (e: Exception) {
            Log.e("BiometricHelper", "Failed to initialize cipher for biometric prompt", e)
            null
        }
    }

    /**
     * Encrypts the verification challenge using the unlocked cipher and saves it to SharedPreferences.
     */
    fun encryptAndSaveChallenge(context: Context, cipher: Cipher): Boolean {
        return try {
            val encryptedBytes = cipher.doFinal(CHALLENGE_STRING.toByteArray(Charsets.UTF_8))
            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val ivBase64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)

            val prefs = com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
            prefs.edit()
                .putString("biometric_challenge_enc", encryptedBase64)
                .putString("biometric_challenge_iv", ivBase64)
                .commit()
            true
        } catch (e: Exception) {
            Log.e("BiometricHelper", "Failed to encrypt and save challenge", e)
            false
        }
    }

    /**
     * Decrypts the challenge from SharedPreferences using the unlocked cipher and verifies it.
     */
    fun decryptAndVerifyChallenge(context: Context, cipher: Cipher): Boolean {
        return try {
            val prefs = com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
            val encryptedBase64 = prefs.getString("biometric_challenge_enc", null) ?: return false
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            val decryptedString = String(decryptedBytes, Charsets.UTF_8)
            decryptedString == CHALLENGE_STRING
        } catch (e: Exception) {
            Log.e("BiometricHelper", "Failed to decrypt and verify challenge", e)
            false
        }
    }

    /**
     * Displays the standard AndroidX BiometricPrompt with optional CryptoObject.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Secure Lock",
        subtitle: String = "Verify identity to unlock your secure session",
        cryptoObject: BiometricPrompt.CryptoObject? = null,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onError("Cancelled")
                    } else {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess(result)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed. Please try again.")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        try {
            if (cryptoObject != null) {
                biometricPrompt.authenticate(promptInfo, cryptoObject)
            } else {
                biometricPrompt.authenticate(promptInfo)
            }
        } catch (e: Exception) {
            Log.e("BiometricHelper", "Failed to start biometric prompt", e)
            onError(e.message ?: "Failed to initiate biometric prompt")
        }
    }
}
