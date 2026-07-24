package com.example.admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.admin.data.model.AdminUserPlan
import com.example.admin.data.model.AdminUserRecord
import com.example.admin.data.model.AdminUserStatus
import com.example.ui.components.AdminSectionHeader
import com.example.ui.components.DangerConfirmDialog
import com.example.ui.theme.AppDimens
import com.example.ui.theme.AppShapes

/**
 * Users tab offering full-featured search, access-tier filters,
 * and standard suspend/reset/delete workflows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersTab(
    users: List<AdminUserRecord>,
    searchQuery: String,
    filterStatus: AdminUserStatus?,
    filterPlan: AdminUserPlan?,
    onSearchChange: (String) -> Unit,
    onStatusFilterChange: (AdminUserStatus?) -> Unit,
    onPlanFilterChange: (AdminUserPlan?) -> Unit,
    onSuspendToggle: (AdminUserRecord) -> Unit,
    onResetPassword: (AdminUserRecord) -> Unit,
    onViewProfile: (AdminUserRecord) -> Unit,
    onDeleteUser: (AdminUserRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var userToSuspendToggle by remember { mutableStateOf<AdminUserRecord?>(null) }
    var userToDelete by remember { mutableStateOf<AdminUserRecord?>(null) }

    // Suspend action confirmation dialog
    if (userToSuspendToggle != null) {
        val u = userToSuspendToggle!!
        val isSuspending = u.status == AdminUserStatus.ACTIVE
        DangerConfirmDialog(
            title = if (isSuspending) "Suspend ${u.displayName}?" else "Reactivate ${u.displayName}?",
            subtitle = if (isSuspending) {
                "${u.displayName} will immediately lose all platform access. You can reactivate this user profile later."
            } else {
                "${u.displayName} will regain full access to their ledger history and features."
            },
            confirmLabel = if (isSuspending) "Confirm Suspend" else "Confirm Reactivate",
            onConfirm = {
                userToSuspendToggle = null
                onSuspendToggle(u)
            },
            onDismiss = { userToSuspendToggle = null }
        )
    }

    // Delete action confirmation dialog with typed-name guard
    if (userToDelete != null) {
        val u = userToDelete!!
        DangerConfirmDialog(
            title = "Delete User Profile?",
            subtitle = "This action permanently removes the user metadata for ${u.displayName}. This cannot be undone.",
            guardText = u.displayName,
            confirmLabel = "Delete Permanently",
            onConfirm = {
                userToDelete = null
                onDeleteUser(u)
            },
            onDismiss = { userToDelete = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_users_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search & Filters Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.paddingLarge)
                .padding(top = AppDimens.paddingLarge),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = { Text("Search users...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("user_search_field")
            )

            // Status Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.bindToDp()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { onStatusFilterChange(null) },
                    label = { Text("All Status") },
                    modifier = Modifier.testTag("chip_status_all")
                )
                FilterChip(
                    selected = filterStatus == AdminUserStatus.ACTIVE,
                    onClick = { onStatusFilterChange(AdminUserStatus.ACTIVE) },
                    label = { Text("Active") },
                    modifier = Modifier.testTag("chip_status_active")
                )
                FilterChip(
                    selected = filterStatus == AdminUserStatus.SUSPENDED,
                    onClick = { onStatusFilterChange(AdminUserStatus.SUSPENDED) },
                    label = { Text("Suspended") },
                    modifier = Modifier.testTag("chip_status_suspended")
                )
            }

            // Plan Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.bindToDp()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                FilterChip(
                    selected = filterPlan == null,
                    onClick = { onPlanFilterChange(null) },
                    label = { Text("All Plans") },
                    modifier = Modifier.testTag("chip_plan_all")
                )
                FilterChip(
                    selected = filterPlan == AdminUserPlan.BASIC,
                    onClick = { onPlanFilterChange(AdminUserPlan.BASIC) },
                    label = { Text("Basic") },
                    modifier = Modifier.testTag("chip_plan_basic")
                )
                FilterChip(
                    selected = filterPlan == AdminUserPlan.BUSINESS,
                    onClick = { onPlanFilterChange(AdminUserPlan.BUSINESS) },
                    label = { Text("Business") },
                    modifier = Modifier.testTag("chip_plan_business")
                )
            }
        }

        HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.outlineVariant)

        // Users LazyColumn
        if (users.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No user records match the active criteria.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("users_lazy_column"),
                contentPadding = PaddingValues(horizontal = AppDimens.paddingLarge, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(users, key = { it.uid }) { user ->
                    AdminUserCard(
                        user = user,
                        onSuspendToggle = { userToSuspendToggle = user },
                        onResetPassword = { onResetPassword(user) },
                        onViewProfile = { onViewProfile(user) },
                        onDelete = { userToDelete = user }
                    )
                }
            }
        }
    }
}

private fun Int.bindToDp(): androidx.compose.ui.unit.Dp = this.dp
