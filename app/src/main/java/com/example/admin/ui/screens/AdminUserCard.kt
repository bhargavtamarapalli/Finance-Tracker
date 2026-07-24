package com.example.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.admin.data.model.AdminUserPlan
import com.example.admin.data.model.AdminUserRecord
import com.example.admin.data.model.AdminUserStatus
import com.example.ui.components.AdminPlanBadge
import com.example.ui.components.AdminStatusBadge
import com.example.ui.theme.AppDimens
import com.example.ui.theme.AppShapes

/**
 * List item card showing summary credentials of a user record
 * and an administrative actions menu button.
 */
@Composable
fun AdminUserCard(
    user: AdminUserRecord,
    onSuspendToggle: () -> Unit,
    onResetPassword: () -> Unit,
    onViewProfile: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Initials generation
    val initials = user.displayName.split(" ")
        .mapNotNull { it.firstOrNull() }
        .joinToString("")
        .take(2)
        .uppercase()

    // Deterministic avatar color based on name hash code
    val avatarColors = listOf(
        Color(0xFF8B5CF6), // Purple
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444)  // Red
    )
    val colorIndex = Math.abs(user.displayName.hashCode()) % avatarColors.size
    val avatarBg = avatarColors[colorIndex]

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("user_card_${user.uid}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        shape = AppShapes.roundedCardMedium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.paddingNormal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            // User Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminStatusBadge(status = user.status)
                    AdminPlanBadge(plan = user.plan)
                }
            }

            // Administrative Action Trigger
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("btn_actions_${user.uid}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Actions Menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.testTag("menu_actions_${user.uid}")
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (user.status == AdminUserStatus.ACTIVE) "Suspend User" else "Reactivate User",
                                color = if (user.status == AdminUserStatus.ACTIVE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onSuspendToggle()
                        },
                        modifier = Modifier.testTag("action_suspend_${user.uid}")
                    )
                    DropdownMenuItem(
                        text = { Text("Reset Password") },
                        onClick = {
                            menuExpanded = false
                            onResetPassword()
                        },
                        modifier = Modifier.testTag("action_reset_password_${user.uid}")
                    )
                    DropdownMenuItem(
                        text = { Text("View Profile") },
                        onClick = {
                            menuExpanded = false
                            onViewProfile()
                        },
                        modifier = Modifier.testTag("action_view_profile_${user.uid}")
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text("Delete User", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        modifier = Modifier.testTag("action_delete_${user.uid}")
                    )
                }
            }
        }
    }
}
