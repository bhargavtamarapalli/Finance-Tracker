package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Only enable FLAG_SECURE in release builds for security
        if (!com.example.BuildConfig.DEBUG) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        
        enableEdgeToEdge()

        var database: AppDatabase? = null
        var jsonDataManager: JsonDataManager? = null
        var repository: FinanceRepository? = null
        var viewModel: FinanceViewModel? = null
        var authRepository: AuthRepository? = null
        var authViewModel: AuthViewModel? = null

        lifecycleScope.launch {
            database = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@MainActivity)
            }
            jsonDataManager = JsonDataManager(this@MainActivity)
            repository = FinanceRepository(database!!.financeDao(), jsonDataManager!!)
            val viewModelFactory = FinanceViewModelFactory(repository!!)
            viewModel = ViewModelProvider(this@MainActivity, viewModelFactory)[FinanceViewModel::class.java]

            authRepository = AuthRepository(this@MainActivity, database)
            val authViewModelFactory = AuthViewModelFactory(authRepository!!)
            authViewModel = ViewModelProvider(this@MainActivity, authViewModelFactory)[AuthViewModel::class.java]
        }

        splashScreen.setKeepOnScreenCondition {
            viewModel?.isLoading?.value != false || viewModel == null || authViewModel == null
        }

        setContent {
            val appTheme by viewModel?.appTheme?.collectAsState() ?: rememberSaveable { mutableStateOf(AppTheme.SYSTEM) }
            val isDark = when (appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            var showSplash by rememberSaveable { mutableStateOf(true) }

            FinanceTrackerTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showSplash || viewModel == null || authViewModel == null) {
                        com.example.ui.screens.CustomSplashScreen(
                            onAnimationComplete = { 
                                if (viewModel != null && authViewModel != null) {
                                    showSplash = false
                                }
                            }
                        )
                    } else {
                        FinanceApp(viewModel = viewModel, authViewModel = authViewModel)
                    }
                }
            }
        }
    }
}
