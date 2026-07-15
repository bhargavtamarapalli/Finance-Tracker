package com.example.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.AuthViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.FinanceButton
import com.example.ui.components.FinanceTextButton
import com.example.ui.components.ProfileAvatar
import kotlinx.coroutines.launch

data class NavigationDrawerItemData(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun FinanceApp(
    viewModel: FinanceViewModel,
    authViewModel: AuthViewModel
) {
    val userSession by authViewModel.currentUserSession.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(userSession) {
        val session = userSession
        if (session != null) {
            viewModel.updateSession(session.userId, session.isGuest)
        } else {
            viewModel.updateSession(null, false)
        }
    }

    if (isLoading) {
        // Show empty background while initial DB loading completes
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    if (userSession == null) {
        AuthScreen(viewModel = authViewModel)
        return
    }

    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()
    var isAppUnlocked by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    // Lock the app when it goes to the background (ON_STOP)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                    isAppUnlocked = false
                }
            }
        )
    }

    // Trigger biometric unlock prompt on launch, resume, or theme change if enabled
    LaunchedEffect(biometricLockEnabled, userSession, isAppUnlocked) {
        if (biometricLockEnabled && userSession != null && !isAppUnlocked) {
            if (activity != null) {
                val prefs = com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(activity, "auth_prefs")
                val ivBase64 = prefs.getString("biometric_challenge_iv", null)
                val iv = if (ivBase64 != null) android.util.Base64.decode(ivBase64, android.util.Base64.NO_WRAP) else null
                val cipher = com.example.ui.utils.BiometricHelper.getInitializedCipher(javax.crypto.Cipher.DECRYPT_MODE, iv)
                val cryptoObject = if (cipher != null) androidx.biometric.BiometricPrompt.CryptoObject(cipher) else null

                com.example.ui.utils.BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    cryptoObject = cryptoObject,
                    onSuccess = { result ->
                        val unlockedCipher = result.cryptoObject?.cipher
                        if (unlockedCipher != null && com.example.ui.utils.BiometricHelper.decryptAndVerifyChallenge(activity, unlockedCipher)) {
                            isAppUnlocked = true
                        }
                    },
                    onError = {
                        // User canceled or failed; they can retry manually using the unlock button
                    }
                )
            } else {
                android.util.Log.w("FinanceApp", "FragmentActivity is null; biometric prompt cannot be shown. Lock screen remains active.")
            }
        }
    }

    // Reset unlock state when user logs out
    LaunchedEffect(userSession) {
        if (userSession == null) {
            isAppUnlocked = false
        }
    }

    // High security lock overlay blocking access to financial data
    if (biometricLockEnabled && !isAppUnlocked && userSession != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(AppDimens.paddingLarge),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "App Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
                
                Spacer(modifier = Modifier.height(AppDimens.paddingLarge))
                
                Text(
                    text = "Finance App Locked",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                
                Text(
                    text = "Confirm biometrics to unlock and view your secure session",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                FinanceButton(
                    text = "Unlock with Biometrics",
                    onClick = {
                        if (activity != null) {
                            val prefs = com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(activity, "auth_prefs")
                            val ivBase64 = prefs.getString("biometric_challenge_iv", null)
                            val iv = if (ivBase64 != null) android.util.Base64.decode(ivBase64, android.util.Base64.NO_WRAP) else null
                            val cipher = com.example.ui.utils.BiometricHelper.getInitializedCipher(javax.crypto.Cipher.DECRYPT_MODE, iv)
                            val cryptoObject = if (cipher != null) androidx.biometric.BiometricPrompt.CryptoObject(cipher) else null

                            com.example.ui.utils.BiometricHelper.showBiometricPrompt(
                                activity = activity,
                                cryptoObject = cryptoObject,
                                onSuccess = { result ->
                                    val unlockedCipher = result.cryptoObject?.cipher
                                    if (unlockedCipher != null && com.example.ui.utils.BiometricHelper.decryptAndVerifyChallenge(activity, unlockedCipher)) {
                                        isAppUnlocked = true
                                    }
                                },
                                onError = {
                                    // Handle retry
                                }
                            )
                        }
                    },
                    icon = Icons.Default.Fingerprint,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                
                Spacer(modifier = Modifier.height(AppDimens.paddingNormal))
                
                FinanceTextButton(
                    text = "Log Out",
                    onClick = {
                        authViewModel.logout()
                    },
                    contentColor = MaterialTheme.colorScheme.error
                )
            }
        }
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val drawerItems = listOf(
        NavigationDrawerItemData("dashboard", "Home", Icons.Default.Home),
        NavigationDrawerItemData("transactions", "History", Icons.AutoMirrored.Filled.List),
        NavigationDrawerItemData("analytics", "Analytics", Icons.Default.PieChart),
        NavigationDrawerItemData("settings", "Settings", Icons.Default.Settings)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute in listOf("dashboard", "transactions", "analytics", "settings"),
        drawerContent = {
ModalDrawerSheet(modifier = Modifier.testTag("drawer_sheet")) {
    Spacer(modifier = Modifier.height(AppDimens.paddingNormal))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.ic_app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(AppDimens.paddingMedium))
                        Text(
                            text = "Finance Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(AppDimens.paddingNormal))
                    ProfileAvatar(
                        name = userSession?.name,
                        isGuest = userSession?.isGuest == true,
                        size = AppDimens.sizeDrawerAvatar
                    )
                    Spacer(modifier = Modifier.height(AppDimens.paddingMedium))
                    Text(
                        text = userSession?.name ?: "Guest User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (userSession?.isGuest == true) "Guest Session" else (userSession?.email ?: "guest@example.com"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (userSession?.isGuest == true) {
                        Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                        SuggestionChip(
                            onClick = {
                                scope.launch { drawerState.close() }
                                authViewModel.logout()
                            },
                            label = { Text("Sign In / Register") },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Login,
                                    contentDescription = "Sign In icon",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp))
                Spacer(modifier = Modifier.height(AppDimens.paddingNormal))
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, fontWeight = FontWeight.Bold) },
                        selected = currentRoute == item.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp))
                Spacer(modifier = Modifier.height(AppDimens.paddingSmall))

                val isGuest = userSession?.isGuest == true
                NavigationDrawerItem(
                    icon = { 
                        Icon(
                            imageVector = if (isGuest) Icons.Default.Login else Icons.Default.Logout, 
                            contentDescription = if (isGuest) "Sign In" else "Log Out", 
                            tint = if (isGuest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        ) 
                    },
                    label = { 
                        Text(
                            text = if (isGuest) "Sign In / Register" else "Log Out", 
                            fontWeight = FontWeight.Bold, 
                            color = if (isGuest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        ) 
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        authViewModel.logout()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = if (isGuest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        unselectedTextColor = if (isGuest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                )
                Spacer(modifier = Modifier.height(AppDimens.paddingNormal))
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (currentRoute in listOf("dashboard", "transactions", "analytics", "settings")) {
NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 0.dp,
    modifier = Modifier
        .clip(RoundedCornerShape(topStart = AppDimens.paddingNormal, topEnd = AppDimens.paddingNormal))
        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(topStart = AppDimens.paddingNormal, topEnd = AppDimens.paddingNormal))
        .testTag("bottom_navigation_bar")
) {
    val items = listOf(
        Triple("dashboard", "Home", Icons.Default.Home),
        Triple("transactions", "History", Icons.AutoMirrored.Filled.List),
        Triple("analytics", "Analytics", Icons.Default.PieChart),
        Triple("settings", "Settings", Icons.Default.Settings)
    )
                        items.forEach { (route, label, icon) ->
                            val isSelected = currentRoute == route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { navController.navigate(route) { launchSingleTop = true; restoreState = true } },
                                icon = { 
                                    Icon(
                                        icon, 
                                        contentDescription = label
                                    ) 
                                },
                                label = { 
                                    Text(
                                        label, 
                                        style = MaterialTheme.typography.labelSmall
                                    ) 
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            val notifications by viewModel.notificationManager.activeInAppNotifications.collectAsState()
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    modifier = Modifier.padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding()
                    )
                ) {
                    composable("dashboard") { 
                        DashboardScreen(
                            viewModel = viewModel,
                            navController = navController,
                            userSession = userSession,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onSignOut = { authViewModel.logout() },
                            onSignIn = { authViewModel.logout() }
                        ) 
                    }
                    composable("transactions") { 
                        TransactionHistoryScreen(viewModel, navController, onMenuClick = { scope.launch { drawerState.open() } }) 
                    }
                    composable("analytics") { 
                        AnalyticsScreen(
                            viewModel = viewModel, 
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onChartClick = { chartType ->
                                navController.navigate("analytics_details?initialChartType=$chartType")
                            }
                        ) 
                    }
                    composable(
                        route = "analytics_details?initialChartType={initialChartType}",
                        arguments = listOf(
                            navArgument("initialChartType") {
                                type = NavType.StringType
                                defaultValue = "CATEGORY"
                            }
                        )
                    ) { backStackEntry ->
                        val initialChartType = backStackEntry.arguments?.getString("initialChartType") ?: "CATEGORY"
                        AnalyticsDetailScreen(
                            viewModel = viewModel,
                            initialChartType = initialChartType,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("settings") { 
                        SettingsScreen(
                            viewModel = viewModel,
                            authViewModel = authViewModel,
                            onManageCategoriesClick = { navController.navigate("categories_management") },
                            onAdminConsoleClick = { navController.navigate("admin_console") },
                            onMenuClick = { scope.launch { drawerState.open() } }
                        ) 
                    }
                    composable("admin_console") {
                        if (userSession?.role == "ADMIN") {
                            AdminConsoleScreen(
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                navController.navigate("dashboard") {
                                    popUpTo("admin_console") { inclusive = true }
                                }
                            }
                        }
                    }
                    composable("categories_management") {
                        CategoryManagementScreen(viewModel, navController)
                    }
                    composable(
                        route = "add_transaction/{type}?transactionId={transactionId}&duplicate={duplicate}",
                        arguments = listOf(
                            navArgument("transactionId") { 
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("duplicate") { 
                                type = NavType.BoolType
                                defaultValue = false
                            }
                        )
                    ) { backStackEntry -> 
                        val type = backStackEntry.arguments?.getString("type") ?: "EXPENSE"
                        val transactionIdString = backStackEntry.arguments?.getString("transactionId")
                        val transactionId = transactionIdString?.toIntOrNull()
                        val duplicate = backStackEntry.arguments?.getBoolean("duplicate") ?: false
                        AddTransactionScreen(viewModel, navController, type, transactionId, duplicate) 
                    }
                }
                
                com.example.ui.components.InAppNotificationHost(
                    notifications = notifications,
                    onDismiss = { id -> viewModel.notificationManager.dismissInApp(id) }
                )
            }
        }
    }
}
