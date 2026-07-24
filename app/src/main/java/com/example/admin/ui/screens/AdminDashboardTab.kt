package com.example.admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.admin.data.model.AdminSystemStats
import com.example.data.model.Announcement
import com.example.ui.components.AdminSectionHeader
import com.example.ui.components.AdminStatCard
import com.example.ui.components.DangerConfirmDialog
import com.example.ui.theme.AppDimens
import com.example.ui.theme.AppShapes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dashboard tab showing system-level high-level metrics, news updates,
 * and global maintenance action cards.
 */
@Composable
fun AdminDashboardTab(
    stats: AdminSystemStats?,
    announcements: List<Announcement>,
    onExportReport: () -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPurgeDialog by remember { mutableStateOf(false) }

    if (showPurgeDialog) {
        DangerConfirmDialog(
            title = "Purge Platform Database?",
            subtitle = "This action will permanently delete all user transactions, categories, and cached database records. This cannot be undone.",
            guardText = "PURGE DATABASE",
            confirmLabel = "Purge System Data",
            onConfirm = {
                showPurgeDialog = false
                onClearAllData()
            },
            onDismiss = { showPurgeDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_dashboard_tab"),
        contentPadding = PaddingValues(AppDimens.paddingLarge),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Stats Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminSectionHeader(title = "Platform Aggregate Metrics", icon = Icons.Default.Speed)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminStatCard(
                        icon = Icons.Default.People,
                        label = "Total Users",
                        value = (stats?.totalUsers ?: 0).toString(),
                        modifier = Modifier.weight(1f).testTag("stat_total_users")
                    )
                    AdminStatCard(
                        icon = Icons.Default.Speed,
                        label = "Active Sessions",
                        value = (stats?.activeUsers ?: 0).toString(),
                        modifier = Modifier.weight(1f).testTag("stat_active_users")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminStatCard(
                        icon = Icons.Default.PieChart,
                        label = "Transactions Ledger",
                        value = (stats?.totalTransactions ?: 0L).toString(),
                        modifier = Modifier.weight(1f).testTag("stat_total_transactions")
                    )
                    AdminStatCard(
                        icon = Icons.Default.Campaign,
                        label = "Published Announcements",
                        value = (stats?.announcementsCount ?: 0).toString(),
                        modifier = Modifier.weight(1f).testTag("stat_total_announcements")
                    )
                }
            }
        }

        // Global Operations
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminSectionHeader(title = "Administrative Operations", icon = Icons.Default.Speed)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onExportReport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.weight(1f).testTag("btn_export_report"),
                        shape = AppShapes.roundedCardMedium
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Export System Report")
                    }

                    Button(
                        onClick = { showPurgeDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.weight(1f).testTag("btn_purge_database"),
                        shape = AppShapes.roundedCardMedium
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Purge Database")
                    }
                }
            }
        }

        // Recent Broadcasts Activity Log
        item {
            AdminSectionHeader(title = "Recent Announcements Log", icon = Icons.Default.Campaign)
        }

        if (announcements.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent broadcasts generated.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(announcements.take(5)) { announcement ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(AppDimens.paddingLarge),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = announcement.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(announcement.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = announcement.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
