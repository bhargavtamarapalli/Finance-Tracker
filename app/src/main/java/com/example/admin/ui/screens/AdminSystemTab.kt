package com.example.admin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.AdminSectionHeader
import com.example.ui.theme.AppDimens
import com.example.ui.theme.AppShapes

/**
 * System configuration tab containing application mode selector,
 * local network diagnostics simulators, and platform encryption declarations.
 */
@Composable
fun AdminSystemTab(
    activeAppMode: String,
    onAppModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mockContext = LocalContext.current

    // Local interactive simulation stats
    var simulatedUsers by remember { mutableStateOf(1248) }
    var activeConnections by remember { mutableStateOf(84) }

    val modeOptions = listOf(
        Triple("Personal Finance Ledger", "Individual income, budgeting, and category expense tracking.", Icons.Default.Person),
        Triple("Chitti / Chit Fund Management", "Group auction schedules, chit contributions, and subscriber tracking.", Icons.Default.AccountBalance),
        Triple("Small Business Ledger (Khata Book)", "Customer credit (Udhaar), supplier accounts, and invoice tallies.", Icons.Default.Store),
        Triple("Apartment & Association Finance", "Maintenance dues collection, shared amenity booking, and sinking funds.", Icons.Default.HomeWork),
        Triple("Group Expense & Trip Sharing", "Split bills, fractional shares, and settlement calculations.", Icons.Default.Groups)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_system_tab"),
        contentPadding = PaddingValues(AppDimens.paddingLarge),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // System Diagnostics
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminSectionHeader(title = "System Diagnostics & Privacy Guard", icon = Icons.Default.Security)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = AppShapes.roundedCardMedium
                    ) {
                        Column(
                            modifier = Modifier.padding(AppDimens.paddingNormal),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Simulated Users",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = simulatedUsers.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (simulatedUsers > 1) simulatedUsers -= 5 },
                                    modifier = Modifier.size(28.dp).testTag("sim_users_dec")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                }
                                Text("Simulate", style = MaterialTheme.typography.labelSmall)
                                IconButton(
                                    onClick = { simulatedUsers += 15 },
                                    modifier = Modifier.size(28.dp).testTag("sim_users_inc")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = AppShapes.roundedCardMedium
                    ) {
                        Column(
                            modifier = Modifier.padding(AppDimens.paddingNormal),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Active Sessions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = activeConnections.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (activeConnections > 1) activeConnections -= 2 },
                                    modifier = Modifier.size(28.dp).testTag("sim_conn_dec")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                }
                                Text("Network", style = MaterialTheme.typography.labelSmall)
                                IconButton(
                                    onClick = { activeConnections += 3 },
                                    modifier = Modifier.size(28.dp).testTag("sim_conn_inc")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Encryption Notice
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                ),
                shape = AppShapes.roundedCardMedium,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(AppDimens.paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EnhancedEncryption,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "System Encryption Protocol Active",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = "User financial transactions and credentials are encrypted using modern client-side AES-256 standard and secure Android sandboxing. As an Administrator, you have zero viewing access, logical capabilities, or security keys to read individual user data. This maintains strict absolute data confidentiality.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Configure App Mode
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminSectionHeader(title = "Configure Application Target Mode", icon = Icons.Default.Build)
                Text(
                    text = "Select the structural blueprint for this instance. This demonstrates the framework's expansion capability.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    modeOptions.forEach { (modeTitle, modeDesc, modeIcon) ->
                        val isSelected = activeAppMode == modeTitle
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                }
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else null,
                            shape = AppShapes.roundedCardMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAppModeChange(modeTitle)
                                    Toast.makeText(context, "App blueprint changed to: $modeTitle", Toast.LENGTH_SHORT).show()
                                }
                                .testTag("blueprint_$modeTitle")
                        ) {
                            Row(
                                modifier = Modifier.padding(AppDimens.paddingLarge),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = modeIcon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = modeTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = modeDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
