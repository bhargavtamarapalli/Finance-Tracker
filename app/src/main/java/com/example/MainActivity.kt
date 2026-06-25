package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.example.ui.viewmodel.AppTheme
import com.example.ui.theme.FinanceTrackerTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.FinanceViewModelFactory

import com.example.data.repository.AuthRepository
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.AuthViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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

        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val isDark = when (appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            FinanceTrackerTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FinanceApp(viewModel = viewModel, authViewModel = authViewModel)
                }
            }
        }
    }
}
