package com.example.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.admin.data.model.AdminUserRecord
import com.example.admin.data.model.AdminUserStatus
import com.example.ui.components.AdminPlanBadge
import com.example.ui.components.AdminStatusBadge
import com.example.ui.components.DangerConfirmDialog
import com.example.ui.theme.AppDimens
import com.example.ui.theme.AppShapes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bottom sheet displaying comprehensive credentials, diagnostics metadata,
 * and standard account modification links.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserProfileSheet(
    user: AdminUserRecord,
    onSuspendToggle: () -> Unit,
    onResetPassword: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSuspendConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Suspend check
    if (showSuspendConfirm) {
        val isSuspending = user.status == AdminUserStatus.ACTIVE
        DangerConfirmDialog(
            title = if (isSuspending) "Suspend ${user.displayName}?" else "Reactivate ${user.displayName}?",
            subtitle = if (isSuspending) {
                "${user.displayName} will lose all application privileges."
            } else {
                "${user.displayName} will recover full access privileges."
            },
            confirmLabel = if (isSuspending) "Confirm Suspend" else "Confirm Reactivate",
            onConfirm = {
                showSuspendConfirm = false
                onSuspendToggle()
            },
            onDismiss = { showSuspendConfirm = false }
        )
    }

    // Delete check
    if (showDeleteConfirm) {
        DangerConfirmDialog(
            title = "Delete ${user.displayName}?",
            subtitle = "This action will permanently delete all records related to this user profile. This cannot be undone.",
            guardText = user.displayName,
            confirmLabel = "Delete Permanently",
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("admin_user_profile_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.paddingLarge)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title Header
            Text(
                text = "Detailed Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Avatar Section
            val initials = user.displayName.split(" ")
                .mapNotNull { it.firstOrNull() }
                .joinToString("")
                .take(2)
                .uppercase()

            val avatarColors = listOf(Color(0xFF8B5CF6), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444))
            val colorIndex = Math.abs(user.displayName.hashCode()) % avatarColors.size
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(avatarColors[colorIndex]),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            // Name & Email Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    AdminStatusBadge(status = user.status)
                    AdminPlanBadge(plan = user.plan)
                }
            }

            // Metadata Detail Table
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = AppShapes.roundedCardMedium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(AppDimens.paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileMetaRow(label = "System ID", value = user.uid, isMono = true)
                    ProfileMetaRow(
                        label = "Member Since", 
                        value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(user.joinedAt))
                    )
                    ProfileMetaRow(
                        label = "Last Active", 
                        value = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(user.lastActiveAt))
                    )
                    ProfileMetaRow(label = "Total Sessions", value = user.sessionCount.toString())
                    ProfileMetaRow(label = "Device Info", value = user.deviceInfo)
                    ProfileMetaRow(label = "Region", value = user.region)
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showSuspendConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (user.status == AdminUserStatus.ACTIVE) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (user.status == AdminUserStatus.ACTIVE) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.weight(1f).testTag("btn_sheet_suspend"),
                        shape = AppShapes.roundedCardMedium
                    ) {
                        Icon(
                            imageVector = if (user.status == AdminUserStatus.ACTIVE) Icons.Default.Security else Icons.Default.SupervisorAccount,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (user.status == AdminUserStatus.ACTIVE) "Suspend" else "Reactivate")
                    }

                    Button(
                        onClick = onResetPassword,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f).testTag("btn_sheet_reset_password"),
                        shape = AppShapes.roundedCardMedium
                    ) {
                        Icon(imageVector = Icons.Default.LockReset, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Reset PW")
                    }
                }

                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("btn_sheet_delete"),
                    shape = AppShapes.roundedCardMedium
                ) {
                    Text(text = "Permanently Delete User")
                }
            }
        }
    }
}

@Composable
private fun ProfileMetaRow(
    label: String,
    value: String,
    isMono: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isMono) MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) else MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
