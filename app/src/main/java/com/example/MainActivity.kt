package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.local.JsonDataManager
import com.example.data.repository.FinanceRepository
import com.example.ui.FinanceApp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.viewmodel.AppTheme
import com.example.ui.theme.FinanceTrackerTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.FinanceViewModelFactory

import com.example.data.repository.AuthRepository
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.AuthViewModelFactory

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val jsonDataManager = JsonDataManager(this)
        val repository = FinanceRepository(database.financeDao(), jsonDataManager)
        val viewModelFactory = FinanceViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[FinanceViewModel::class.java]

        val authRepository = AuthRepository(this)
        val authViewModelFactory = AuthViewModelFactory(authRepository)
        val authViewModel = ViewModelProvider(this, authViewModelFactory)[AuthViewModel::class.java]

        splashScreen.setKeepOnScreenCondition {
            viewModel.isLoading.value
        }

        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val isDark = when (appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            var showSplash by rememberSaveable { mutableStateOf(true) }

            FinanceTrackerTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showSplash) {
                        com.example.ui.screens.CustomSplashScreen(
                            onAnimationComplete = { showSplash = false }
                        )
                    } else {
                        FinanceApp(viewModel = viewModel, authViewModel = authViewModel)
                    }
                }
            }
        }
    }
}
