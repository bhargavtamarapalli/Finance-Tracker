package com.example.ui.utils

import android.content.Context
import androidx.biometric.BiometricManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BiometricHelperTest {

    private lateinit var context: Context
    private lateinit var biometricManager: BiometricManager

    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
        biometricManager = mockk<BiometricManager>(relaxed = true)
        mockkStatic(BiometricManager::class)
        every { BiometricManager.from(context) } returns biometricManager
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun isBiometricAvailable_returnsTrue_whenBiometricSuccess() {
        every { biometricManager.canAuthenticate(any<Int>()) } returns BiometricManager.BIOMETRIC_SUCCESS

        val isAvailable = BiometricHelper.isBiometricAvailable(context)

        assertTrue(isAvailable)
    }

    @Test
    fun isBiometricAvailable_returnsFalse_whenBiometricErrorNoneEnrolled() {
        every { biometricManager.canAuthenticate(any<Int>()) } returns BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED

        val isAvailable = BiometricHelper.isBiometricAvailable(context)

        assertFalse(isAvailable)
    }

    @Test
    fun isBiometricAvailable_returnsFalse_whenBiometricErrorNoHardware() {
        every { biometricManager.canAuthenticate(any<Int>()) } returns BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE

        val isAvailable = BiometricHelper.isBiometricAvailable(context)

        assertFalse(isAvailable)
    }

    @Test
    fun isBiometricAvailable_returnsFalse_whenBiometricErrorHwUnavailable() {
        every { biometricManager.canAuthenticate(any<Int>()) } returns BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE

        val isAvailable = BiometricHelper.isBiometricAvailable(context)

        assertFalse(isAvailable)
    }
}
