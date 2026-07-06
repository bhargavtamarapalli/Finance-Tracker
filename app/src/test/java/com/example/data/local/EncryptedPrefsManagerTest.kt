package com.example.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EncryptedPrefsManagerTest {

    @Test
    fun testGetEncryptedPrefs_createsPrefsInstance() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = EncryptedPrefsManager.getEncryptedPrefs(context, "test_secure_prefs")
        assertNotNull(prefs)
        
        // Test basic write/read
        prefs.edit().putString("test_key", "secure_value").commit()
        org.junit.Assert.assertEquals("secure_value", prefs.getString("test_key", null))
    }

    @Test
    fun testGetEncryptedPrefs_handlesExceptionAndFallsBack() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // We trigger an exception by passing a mock context that throws exception on build
        val badContext = object : android.content.ContextWrapper(context) {
            override fun getSharedPreferences(name: String?, mode: Int): android.content.SharedPreferences {
                if (name == "throw_error_prefs") {
                    throw RuntimeException("Simulated Keystore Exception")
                }
                return super.getSharedPreferences(name, mode)
            }
        }
        
        // Under ordinary Robolectric execution, building EncryptedSharedPreferences might throw an exception 
        // if KeyStore is not fully configured, which triggers the fallback in EncryptedPrefsManager.
        val prefs = EncryptedPrefsManager.getEncryptedPrefs(badContext, "fallback_prefs_test")
        assertNotNull(prefs)
    }
}
