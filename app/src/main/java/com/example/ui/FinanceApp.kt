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
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.AuthViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
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
                            .background(Color(0xFFEADDFF), CircleShape)
                            .border(2.dp, Color(0xFFD0BCFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = userSession?.name ?: "Guest User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                    Text(
                        text = if (userSession?.isGuest == true) "Guest Session" else (userSession?.email ?: "guest@example.com"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F)
                    )
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
                            selectedContainerColor = Color(0xFFE8DEF8),
                            selectedIconColor = Color(0xFF21005D),
                            selectedTextColor = Color(0xFF21005D),
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F)
                        )
                    )
                }
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
                        onMenuClick = { scope.launch { drawerState.open() } }
                    ) 
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
