package com.example.admin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.admin.data.model.AdminUserRecord
import com.example.admin.ui.screens.AdminAnnouncementsTab
import com.example.admin.ui.screens.AdminDashboardTab
import com.example.admin.ui.screens.AdminSystemTab
import com.example.admin.ui.screens.AdminUsersTab
import com.example.admin.ui.screens.AdminUserProfileSheet
import com.example.admin.ui.viewmodel.AdminUiEvent
import com.example.admin.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

/**
 * Main administrative console entry point containing multi-tab configurations.
 * Coordinates statistics reporting, user lists, announcements, and system blueprint selections.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdminConsoleScreen(
    viewModel: FinanceViewModel,
    adminViewModel: AdminViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State Collection
    val systemStats by adminViewModel.systemStats.collectAsStateWithLifecycle()
    val filteredUsers by adminViewModel.filteredUsers.collectAsStateWithLifecycle()
    val searchQuery by adminViewModel.searchQuery.collectAsStateWithLifecycle()
    val filterStatus by adminViewModel.filterStatus.collectAsStateWithLifecycle()
    val filterPlan by adminViewModel.filterPlan.collectAsStateWithLifecycle()
    val isBusy by adminViewModel.isBusy.collectAsStateWithLifecycle()
    
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val activeAppMode by viewModel.appMode.collectAsStateWithLifecycle()

    var selectedUserForProfile by remember { mutableStateOf<AdminUserRecord?>(null) }
    var showProfileSheet by remember { mutableStateOf(false) }

    // Navigation and notification handler
    LaunchedEffect(Unit) {
        adminViewModel.adminEvent.collect { event ->
            when (event) {
                is AdminUiEvent.ShowSuccess -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is AdminUiEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is AdminUiEvent.NavigateBack -> {
                    onBackClick()
                }
            }
        }
    }

    // Keep dashboard statistics in sync with live announcements size
    LaunchedEffect(announcements.size) {
        adminViewModel.refreshStats(announcements.size)
    }

    // Tabs setup
    val tabs = listOf(
        Triple("Dashboard", Icons.Default.Dashboard, "tab_dashboard"),
        Triple("Users", Icons.Default.People, "tab_users"),
        Triple("Broadcasts", Icons.Default.Campaign, "tab_broadcasts"),
        Triple("System", Icons.Default.Settings, "tab_system")
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Admin Console",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.fillMaxWidth().testTag("admin_tab_row")
                ) {
                    tabs.forEachIndexed { index, (label, icon, tag) ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = { Icon(imageVector = icon, contentDescription = null) },
                            text = { Text(text = label) },
                            modifier = Modifier.testTag(tag)
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    when (page) {
                        0 -> AdminDashboardTab(
                            stats = systemStats,
                            announcements = announcements,
                            onExportReport = { adminViewModel.exportReport() },
                            onClearAllData = { adminViewModel.clearAllData() }
                        )
                        1 -> AdminUsersTab(
                            users = filteredUsers,
                            searchQuery = searchQuery,
                            filterStatus = filterStatus,
                            filterPlan = filterPlan,
                            onSearchChange = { adminViewModel.setSearchQuery(it) },
                            onStatusFilterChange = { adminViewModel.setFilterStatus(it) },
                            onPlanFilterChange = { adminViewModel.setFilterPlan(it) },
                            onSuspendToggle = { user ->
                                if (user.status == com.example.admin.data.model.AdminUserStatus.ACTIVE) {
                                    adminViewModel.suspendUser(user.uid)
                                } else {
                                    adminViewModel.reactivateUser(user.uid)
                                }
                            },
                            onResetPassword = { user -> adminViewModel.resetPassword(user.uid) },
                            onViewProfile = { user ->
                                selectedUserForProfile = user
                                showProfileSheet = true
                            },
                            onDeleteUser = { user -> adminViewModel.deleteUserRecord(user.uid) }
                        )
                        2 -> AdminAnnouncementsTab(
                            announcements = announcements,
                            onPublish = { title, content, cat ->
                                viewModel.publishAnnouncement(title, content, cat)
                            },
                            onDelete = { id -> viewModel.deleteAnnouncement(id) }
                        )
                        3 -> AdminSystemTab(
                            activeAppMode = activeAppMode,
                            onAppModeChange = { mode -> viewModel.setAppMode(mode) }
                        )
                    }
                }
            }

            // Global loading overlay for administrative modifications
            if (isBusy) {
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxSize().testTag("busy_overlay")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            // User Profile Modal Bottom Sheet
            if (showProfileSheet && selectedUserForProfile != null) {
                AdminUserProfileSheet(
                    user = selectedUserForProfile!!,
                    onSuspendToggle = {
                        val u = selectedUserForProfile!!
                        if (u.status == com.example.admin.data.model.AdminUserStatus.ACTIVE) {
                            adminViewModel.suspendUser(u.uid)
                        } else {
                            adminViewModel.reactivateUser(u.uid)
                        }
                        showProfileSheet = false
                    },
                    onResetPassword = {
                        adminViewModel.resetPassword(selectedUserForProfile!!.uid)
                        showProfileSheet = false
                    },
                    onDelete = {
                        adminViewModel.deleteUserRecord(selectedUserForProfile!!.uid)
                        showProfileSheet = false
                    },
                    onDismiss = {
                        showProfileSheet = false
                        selectedUserForProfile = null
                    }
                )
            }
        }
    }
}
