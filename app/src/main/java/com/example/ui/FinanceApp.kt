package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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

    var isSplashAnimComplete by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(isSplashAnimComplete, isLoading) {
        if (isSplashAnimComplete && !isLoading) {
            showSplash = false
        }
    }

    LaunchedEffect(userSession) {
        val session = userSession
        if (session != null) {
            viewModel.updateSession(session.userId, session.isGuest)
        } else {
            viewModel.updateSession(null, false)
        }
    }

    if (showSplash) {
        SplashScreen(onAnimationComplete = { isSplashAnimComplete = true })
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

    // Trigger biometric unlock prompt on launch or theme change if enabled
    LaunchedEffect(biometricLockEnabled, userSession) {
        if (biometricLockEnabled && userSession != null && !isAppUnlocked) {
            if (activity != null) {
                com.example.ui.utils.BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    onSuccess = {
                        isAppUnlocked = true
                    },
                    onError = {
                        // User canceled or failed; they can retry manually using the unlock button
                    }
                )
            } else {
                isAppUnlocked = true
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
                .padding(24.dp),
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Finance App Locked",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
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
                            com.example.ui.utils.BiometricHelper.showBiometricPrompt(
                                activity = activity,
                                onSuccess = {
                                    isAppUnlocked = true
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 20.dp)
                ) {
                    val initials = userSession?.name
                        ?.split(" ")
                        ?.mapNotNull { it.firstOrNull() }
                        ?.take(2)
                        ?.joinToString("")
                        ?.uppercase() ?: "GU"

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
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
                        Spacer(modifier = Modifier.height(8.dp))
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
                Spacer(modifier = Modifier.height(16.dp))
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
                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = "Log Out", tint = MaterialTheme.colorScheme.error) },
                    label = { Text("Log Out", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        authViewModel.logout()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.error,
                        unselectedTextColor = MaterialTheme.colorScheme.error
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                if (currentRoute in listOf("dashboard", "transactions", "analytics", "settings")) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == "dashboard",
                            onClick = { navController.navigate("dashboard") { launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                            label = { Text("Home") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "transactions",
                            onClick = { navController.navigate("transactions") { launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History") },
                            label = { Text("History") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "analytics",
                            onClick = { navController.navigate("analytics") { launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.PieChart, contentDescription = "Analytics") },
                            label = { Text("Analytics") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "settings",
                            onClick = { navController.navigate("settings") { launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("dashboard") { 
                    DashboardScreen(viewModel, navController, userSession = userSession, onMenuClick = { scope.launch { drawerState.open() } }) 
                }
                composable("transactions") { 
                    TransactionHistoryScreen(viewModel, navController, onMenuClick = { scope.launch { drawerState.open() } }) 
                }
                composable("analytics") { 
                    AnalyticsScreen(viewModel, onMenuClick = { scope.launch { drawerState.open() } }) 
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
        }
    }
}
